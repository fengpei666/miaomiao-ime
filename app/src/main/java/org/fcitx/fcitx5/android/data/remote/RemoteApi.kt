/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright 2025 MiaoMiao IME Contributors
 */
package org.fcitx.fcitx5.android.data.remote

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import org.fcitx.fcitx5.android.BuildConfig
import org.fcitx.fcitx5.android.utils.Const
import timber.log.Timber
import java.net.HttpURLConnection
import java.net.URL
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * 在线数据客户端：直接读取仓库 remote/ 目录下的静态 JSON（GitHub 托管），
 * 不再依赖自建后端。多源依次回退，并用时间戳参数绕过 CDN 缓存。
 */
object RemoteApi {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

    private val timeFormats = listOf(
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"),
        DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"),
        DateTimeFormatter.ISO_LOCAL_DATE_TIME
    )

    /** 依次尝试各数据源，返回第一份可用的文本 */
    private suspend fun fetchText(file: String): String = withContext(Dispatchers.IO) {
        var lastError: Throwable? = null
        for (base in Const.remoteJsonBases) {
            val result = runCatching {
                // 时间戳参数：让 CDN 视为新资源，避免读到旧公告/旧版本号
                val url = URL("$base/$file?t=${System.currentTimeMillis()}")
                val conn = (url.openConnection() as HttpURLConnection).apply {
                    connectTimeout = 6_000
                    readTimeout = 10_000
                    instanceFollowRedirects = true
                    setRequestProperty("Accept", "application/json")
                    setRequestProperty("Cache-Control", "no-cache")
                }
                try {
                    val code = conn.responseCode
                    if (code !in 200..299) error("HTTP $code for $file")
                    conn.inputStream.bufferedReader().use { it.readText() }
                } finally {
                    conn.disconnect()
                }
            }
            result.onSuccess { return@withContext it }
            lastError = result.exceptionOrNull()
            Timber.w(lastError, "remote json failed: %s/%s", base, file)
        }
        throw lastError ?: IllegalStateException("no remote source configured")
    }

    private suspend inline fun <reified T> fetch(file: String): T =
        json.decodeFromString<T>(fetchText(file))

    /** 正式版更新 */
    suspend fun getUpdate(): UpdateInfo = fetch("update.json")

    /** 内测更新 */
    suspend fun getBeta(): UpdateInfo = fetch("beta.json")

    /** 公告：拉取全量后在本地按版本/时间/启用过滤 */
    suspend fun getNotice(versionCode: Int = BuildConfig.VERSION_CODE): NoticeList =
        fetch<NoticeList>("notice.json").fitFor(versionCode)

    /** 赞助 */
    suspend fun getSponsor(): SponsorInfo = fetch("sponsor.json")

    /** 全局配置 */
    suspend fun getConfig(): GlobalConfig = fetch("config.json")

    /** 客户端侧公告过滤，规则与原先服务端一致 */
    internal fun NoticeList.fitFor(versionCode: Int): NoticeList {
        val now = LocalDateTime.now()
        val kept = notices.filter { n ->
            n.enabled &&
                    versionCode >= n.minVersionCode &&
                    versionCode <= n.maxVersionCode &&
                    reachStart(n.startTime, now) &&
                    notExpired(n.endTime, now)
        }
        return copy(notices = kept, count = kept.size)
    }

    private fun reachStart(start: String, now: LocalDateTime): Boolean {
        if (start.isBlank()) return true
        val t = parseTime(start) ?: return true
        return !now.isBefore(t)
    }

    private fun notExpired(end: String, now: LocalDateTime): Boolean {
        if (end.isBlank()) return true
        val t = parseTime(end) ?: return true
        return !now.isAfter(t)
    }

    /** 支持 `yyyy-MM-dd HH:mm:ss`、`yyyy-MM-dd HH:mm`、ISO 及纯日期；无法解析时返回 null（视为不限） */
    private fun parseTime(raw: String): LocalDateTime? {
        val text = raw.trim()
        if (text.isEmpty()) return null
        for (fmt in timeFormats) {
            runCatching { return LocalDateTime.parse(text, fmt) }
        }
        runCatching { return LocalDate.parse(text).atStartOfDay() }
        Timber.w("unparsable notice time: %s", text)
        return null
    }
}
