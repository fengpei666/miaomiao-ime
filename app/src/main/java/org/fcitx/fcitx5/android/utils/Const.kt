/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2021-2023 Fcitx5 for Android Contributors
 */
package org.fcitx.fcitx5.android.utils

import org.fcitx.fcitx5.android.BuildConfig

object Const {
    const val versionName = "${BuildConfig.VERSION_NAME}-${BuildConfig.BUILD_TYPE}"
    /** 本项目仓库 */
    const val githubRepo = "https://github.com/fengpei666/miaomiao-ime"
    /** 本应用二次开发所基于的上游项目：Fcitx5 for Android（小企鹅输入法 Android 版） */
    const val upstreamRepo = "https://github.com/fcitx5-android/fcitx5-android"
    /** 上游输入法框架 Fcitx5（小企鹅输入法）官网 */
    const val upstreamEngineUrl = "https://fcitx-im.org"
    /** 官方网址（喵喵输入法官网主页） */
    const val officialWebsite = "https://miao.zyycyun.cn"
    const val licenseSpdxId = "LGPL-2.1-or-later"
    const val licenseUrl = "https://www.gnu.org/licenses/old-licenses/lgpl-2.1"
    const val privacyPolicyUrl = "https://miao.zyycyun.cn/privacy/"
    const val faqUrl = "https://miao.zyycyun.cn/faq/"

    /** 在线接口基址（喵喵输入法后端） */
    const val API_BASE = "https://miao.zyycyun.cn/api"
}