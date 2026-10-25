/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright 2025 MiaoMiao IME Contributors
 */
package org.fcitx.fcitx5.android.data.remote

import android.app.Activity
import androidx.appcompat.app.AlertDialog
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import org.fcitx.fcitx5.android.BuildConfig
import org.fcitx.fcitx5.android.R

/**
 * 在线接口相关的弹窗（更新 / 维护 / 公告）。
 * 各 buildXxx 返回已构建但未显示的 [AlertDialog]，便于按顺序串联展示。
 */
object RemotePrompts {

    private fun openBrowser(context: Context, url: String?) {
        if (url.isNullOrEmpty()) return
        runCatching {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                if (context !is Activity) addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        }.onFailure {
            if (it is ActivityNotFoundException) {
                // 没有浏览器可处理，忽略
            }
        }
    }

    fun buildMaintenance(context: Context, config: GlobalConfig): AlertDialog =
        AlertDialog.Builder(context).apply {
            setTitle(R.string.maintenance_title)
            setMessage(
                config.maintenanceTip.ifEmpty {
                    context.getString(R.string.maintenance_default)
                }
            )
            setPositiveButton(android.R.string.ok, null)
            setCancelable(false)
        }.create()

    /**
     * @param onIgnore 用户点击"忽略此版本"时回调，传入被忽略的 versionCode
     */
    fun buildUpdate(
        context: Context,
        info: UpdateInfo,
        onIgnore: (Int) -> Unit
    ): AlertDialog {
        val forced = info.force || info.minSupportCode > BuildConfig.VERSION_CODE
        val isRedirect = info.updateMode == "redirect"
        val message = buildString {
            appendLine(context.getString(R.string.update_version, info.versionName))
            if (info.apkSizeText.isNotEmpty())
                appendLine(context.getString(R.string.update_size, info.apkSizeText))
            if (info.updateTime.isNotEmpty())
                appendLine(context.getString(R.string.update_time, info.updateTime))
            appendLine()
            info.updateLog.lineSequence()
                .map { it.trim() }
                .filter { it.isNotEmpty() }
                .forEach { appendLine("• $it") }
        }
        val downloadText = if (isRedirect) R.string.update_go_external else R.string.update_download
        return AlertDialog.Builder(context).apply {
            setTitle(R.string.update_available)
            setMessage(message)
            setPositiveButton(downloadText) { _, _ ->
                if (isRedirect) {
                    // 跳转模式：先提示，再打开外部链接（downloadUrl 此时已是外部地址）
                    val tip = info.redirectTip.ifEmpty {
                        context.getString(R.string.update_redirect_default_tip)
                    }
                    AlertDialog.Builder(context).apply {
                        setMessage(tip)
                        setPositiveButton(R.string.update_go_external) { _, _ ->
                            openBrowser(context, info.downloadUrl.ifEmpty { info.apkUrl })
                        }
                        if (!forced) setNegativeButton(android.R.string.cancel, null)
                        else setCancelable(false)
                    }.show()
                } else {
                    // 内置下载：打开 JSON 中配置的下载地址（通常是 GitHub Releases）
                    openBrowser(context, info.downloadUrl.ifEmpty { info.apkUrl })
                }
            }
            if (!forced) {
                setNegativeButton(R.string.update_ignore) { _, _ -> onIgnore(info.versionCode) }
                setNeutralButton(android.R.string.cancel, null)
            } else {
                setCancelable(false)
            }
        }.create()
    }

    fun buildNotice(context: Context, notice: NoticeItem): AlertDialog {
        val builder = AlertDialog.Builder(context).apply {
            setTitle(notice.title.ifEmpty { context.getString(R.string.notice) })
            setMessage(notice.content.ifEmpty { "" })
        }
        val confirm = notice.confirmText.ifEmpty { context.getString(android.R.string.ok) }
        builder.setPositiveButton(confirm, null)
        if (notice.cancelText.isNotEmpty() && !notice.force) {
            builder.setNegativeButton(notice.cancelText, null)
        }
        if (notice.linkText.isNotEmpty() && notice.linkUrl.isNotEmpty()) {
            builder.setNeutralButton(notice.linkText) { _, _ ->
                openBrowser(context, notice.linkUrl)
            }
        }
        if (notice.force) builder.setCancelable(false)
        return builder.create()
    }

    /** 将多个弹窗按顺序展示：关闭一个再展示下一个。 */
    fun showSequential(context: Context, dialogs: List<AlertDialog>) {
        if (dialogs.isEmpty()) return
        var index = 0
        fun show(i: Int) {
            if (i >= dialogs.size) return
            val d = dialogs[i]
            d.setOnDismissListener { show(i + 1) }
            d.show()
        }
        show(0)
    }
}
