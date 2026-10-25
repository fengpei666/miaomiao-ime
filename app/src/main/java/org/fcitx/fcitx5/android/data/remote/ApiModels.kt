/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright 2025 MiaoMiao IME Contributors
 */
package org.fcitx.fcitx5.android.data.remote

import kotlinx.serialization.Serializable

/**
 * 更新信息（对应 remote/update.json、remote/beta.json，字段平铺）。
 */
@Serializable
data class UpdateInfo(
    val versionCode: Int = 0,
    val versionName: String = "",
    val updateTime: String = "",
    val apkPath: String = "",
    val apkUrl: String = "",
    val apkSize: Long = 0,
    val updateLog: String = "",
    val force: Boolean = false,
    val minSupportCode: Int = 1,
    val available: Boolean = false,
    val apkSizeText: String = "",
    val downloadUrl: String = "",
    /** 更新方式：inapp=内置下载；redirect=跳转外部（downloadUrl 此时已是外部链接）。后台新增。 */
    val updateMode: String = "inapp",
    /** 跳转模式下的外部下载地址（redirect 时 downloadUrl 等于它） */
    val redirectUrl: String = "",
    /** 跳转前提示语，空则 APP 用默认文案 */
    val redirectTip: String = "",
    val serverTime: String = ""
)

/**
 * 公告列表（对应 remote/notice.json；版本/时间/启用由客户端过滤）。
 */
@Serializable
data class NoticeList(
    val notices: List<NoticeItem> = emptyList(),
    val count: Int = 0,
    val serverTime: String = ""
)

@Serializable
data class NoticeItem(
    val id: String = "",
    val type: String = "dialog",
    val enabled: Boolean = true,
    val title: String = "",
    val content: String = "",
    val confirmText: String = "",
    val cancelText: String = "",
    val linkText: String = "",
    val linkUrl: String = "",
    val once: Boolean = false,
    val force: Boolean = false,
    val startTime: String = "",
    val endTime: String = "",
    val minVersionCode: Int = 0,
    val maxVersionCode: Int = Int.MAX_VALUE
)

/**
 * 赞助信息（对应 remote/sponsor.json）。
 */
@Serializable
data class SponsorInfo(
    val enabled: Boolean = false,
    val title: String = "",
    val desc: String = "",
    val buttonText: String = "",
    val url: String = "",
    val notice: String = "",
    val sponsorsTitle: String = "",
    val sponsors: List<SponsorItem> = emptyList(),
    val serverTime: String = ""
)

@Serializable
data class SponsorItem(
    val name: String = "",
    val channel: String = "",
    val amount: String = "",
    val enabled: Boolean = true
)

/**
 * 全局配置（对应 remote/config.json）。
 */
@Serializable
data class GlobalConfig(
    val maintenance: Boolean = false,
    val maintenanceTip: String = "",
    val minUseVersionCode: Int = 1,
    val checkUpdateIntervalHours: Int = 24,
    val qqGroup: String = "",
    val douyinGroup: String = "",
    val feedbackUrl: String = "",
    val privacyUrl: String = "",
    val agreementUrl: String = "",
    val shareText: String = "",
    val serverTime: String = ""
)
