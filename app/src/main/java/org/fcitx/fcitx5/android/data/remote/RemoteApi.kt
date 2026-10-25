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
import java.net.HttpURLConnection
import java.net.URL

/**
 * 轻量 HTTP 客户端：直接拉取后端 PHP 接口并以 kotlinx.serialization 解析。
 * 不引入 OkHttp/Retrofit，复用项目已有的序列化与协程依赖。
 */
object RemoteApi {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

    private suspend inline fun <reified T> fetch(path: String): T = withContext(Dispatchers.IO) {
        val url = URL("${Const.API_BASE}/$path")
        val conn = (url.openConnection() as HttpURLConnection).apply {
            connectTimeout = 10_000
            readTimeout = 15_000
            setRequestProperty("Accept", "application/json")
        }
        conn.inputStream.bufferedReader().use {
            json.decodeFromString<T>(it.readText())
        }
    }

    /** 正式版更新 */
    suspend fun getUpdate(): UpdateInfo = fetch("update.php")

    /** 内测更新 */
    suspend fun getBeta(): UpdateInfo = fetch("beta.php")

    /** 公告（带本机 versionCode，服务端按版本/时间/启用过滤） */
    suspend fun getNotice(versionCode: Int = BuildConfig.VERSION_CODE): NoticeList =
        fetch("notice.php?versionCode=$versionCode")

    /** 赞助 */
    suspend fun getSponsor(): SponsorInfo = fetch("sponsor.php")

    /** 全局配置 */
    suspend fun getConfig(): GlobalConfig = fetch("config.php")
}
