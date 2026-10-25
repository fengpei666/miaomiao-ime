/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright 2025 MiaoMiao IME Contributors
 */
package org.fcitx.fcitx5.android.data.remote

import android.net.Uri
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.fcitx.fcitx5.android.BuildConfig

/**
 * 在线数据的进程内缓存（预加载）。
 * 启动时由 [refreshAll] 一次性拉取，各界面直接读取 StateFlow，
 * 数据未到达时为 null（界面据此显示占位/骨架），到达后自动刷新。
 */
object RemoteRepository {

    private val _config = MutableStateFlow<GlobalConfig?>(null)
    val config: StateFlow<GlobalConfig?> = _config.asStateFlow()

    private val _update = MutableStateFlow<UpdateInfo?>(null)
    val update: StateFlow<UpdateInfo?> = _update.asStateFlow()

    private val _beta = MutableStateFlow<UpdateInfo?>(null)
    val beta: StateFlow<UpdateInfo?> = _beta.asStateFlow()

    private val _notice = MutableStateFlow<NoticeList?>(null)
    val notice: StateFlow<NoticeList?> = _notice.asStateFlow()

    private val _sponsor = MutableStateFlow<SponsorInfo?>(null)
    val sponsor: StateFlow<SponsorInfo?> = _sponsor.asStateFlow()

    suspend fun refreshConfig() {
        runCatching { RemoteApi.getConfig() }.onSuccess { _config.value = it }
    }

    suspend fun refreshUpdate() {
        runCatching { RemoteApi.getUpdate() }.onSuccess { _update.value = it }
    }

    suspend fun refreshBeta() {
        runCatching { RemoteApi.getBeta() }.onSuccess { _beta.value = it }
    }

    suspend fun refreshNotice() {
        runCatching { RemoteApi.getNotice(BuildConfig.VERSION_CODE) }.onSuccess { _notice.value = it }
    }

    suspend fun refreshSponsor() {
        runCatching { RemoteApi.getSponsor() }.onSuccess { _sponsor.value = it }
    }

    /** 并行拉取全部接口（预加载），在调用方协程作用域内执行。 */
    suspend fun refreshAll() {
        coroutineScope {
            launch { refreshConfig() }
            launch { refreshUpdate() }
            launch { refreshBeta() }
            launch { refreshNotice() }
            launch { refreshSponsor() }
        }
    }

    /**
     * 返回比当前版本更新、且可下载（available）的最佳更新；
     * 在正式版与内测版中取 versionCode 较大者。无则 null。
     */
    fun bestUpdate(): UpdateInfo? {
        val current = BuildConfig.VERSION_CODE
        return listOfNotNull(_update.value, _beta.value)
            .filter { it.available && it.versionCode > current }
            .maxByOrNull { it.versionCode }
    }

    /**
     * 判断一个外链是否指向「本 App 自己的安装包/下载页」。
     * 公告链接若指向这里，浏览器打开会直接下载 APK，应予以拦截。
     */
    fun isSelfAppDownloadUrl(url: String): Boolean {
        if (url.isEmpty()) return false
        if (url.endsWith(".apk", ignoreCase = true)) return true
        val linkHost = runCatching { Uri.parse(url).host }.getOrNull() ?: return false
        return listOfNotNull(_update.value, _beta.value)
            .flatMap { listOf(it.apkUrl, it.downloadUrl, it.redirectUrl) }
            .mapNotNull { runCatching { Uri.parse(it).host }.getOrNull() }
            .any { it.equals(linkHost, ignoreCase = true) }
    }
}
