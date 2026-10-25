/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2021-2025 Fcitx5 for Android Contributors
 */
package org.fcitx.fcitx5.android.ui.main

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.lifecycleScope
import androidx.preference.Preference
import kotlinx.coroutines.launch
import org.fcitx.fcitx5.android.BuildConfig
import org.fcitx.fcitx5.android.R
import org.fcitx.fcitx5.android.data.prefs.AppPrefs
import org.fcitx.fcitx5.android.data.remote.RemotePrompts
import org.fcitx.fcitx5.android.data.remote.RemoteRepository
import org.fcitx.fcitx5.android.data.remote.SponsorInfo
import org.fcitx.fcitx5.android.ui.common.PaddingPreferenceFragment
import org.fcitx.fcitx5.android.ui.main.settings.SettingsRoute
import org.fcitx.fcitx5.android.utils.Const
import org.fcitx.fcitx5.android.utils.addCategory
import org.fcitx.fcitx5.android.utils.addPreference
import org.fcitx.fcitx5.android.utils.formatDateTime
import org.fcitx.fcitx5.android.utils.navigateWithAnim

class AboutFragment : PaddingPreferenceFragment() {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        buildScreen()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // 预加载完成后刷新"在线服务"分区（占位 → 真实链接 / 赞助入口）
        viewLifecycleOwner.lifecycleScope.launch {
            RemoteRepository.config.collect { buildScreen() }
        }
        viewLifecycleOwner.lifecycleScope.launch {
            RemoteRepository.sponsor.collect { buildScreen() }
        }
    }

    private fun buildScreen() {
        val ctx = requireContext()
        preferenceScreen = preferenceManager.createPreferenceScreen(ctx).apply {
            // 二开声明：明确写出本项目基于哪个上游项目
            addCategory(R.string.about_source) {
                isIconSpaceReserved = false
                addPreference(R.string.fork_notice_title, R.string.fork_notice_summary) {
                    showForkNotice()
                }
                addPreference(R.string.upstream_project, R.string.upstream_project_summary) {
                    openUrl(Const.upstreamRepo)
                }
                addPreference(R.string.upstream_engine, R.string.upstream_engine_summary) {
                    openUrl(Const.upstreamEngineUrl)
                }
            }
            addPreference(R.string.privacy_policy) {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(Const.privacyPolicyUrl)))
            }
            addPreference(
                R.string.open_source_licenses,
                R.string.licenses_of_third_party_libraries
            ) {
                navigateWithAnim(SettingsRoute.License)
            }
            addPreference(R.string.official_website) {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(Const.officialWebsite)))
            }
            addPreference(R.string.license, Const.licenseSpdxId) {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(Const.licenseUrl)))
            }
            addCategory(R.string.version) {
                isIconSpaceReserved = false
                addPreference(R.string.current_version, Const.versionName)
                addPreference(R.string.build_git_hash, BuildConfig.BUILD_GIT_HASH) {
                    val commit = BuildConfig.BUILD_GIT_HASH.substringBefore('-')
                    val uri = Uri.parse("${Const.githubRepo}/commit/${commit}")
                    startActivity(Intent(Intent.ACTION_VIEW, uri))
                }
                addPreference(R.string.build_time, formatDateTime(BuildConfig.BUILD_TIME))
            }
            // 在线：更新 / 公告
            addCategory(R.string.online_update) {
                isIconSpaceReserved = false
                addPreference(R.string.check_update) {
                    lifecycleScope.launch { checkUpdateManually() }
                }
                addPreference(R.string.notice) {
                    navigateWithAnim(SettingsRoute.Notice)
                }
            }
            // 在线服务（依赖全局配置；未加载时显示占位）
            addCategory(R.string.online_services) {
                isIconSpaceReserved = false
                val config = RemoteRepository.config.value
                if (config == null) {
                    // 占位：占住位置，数据到达后替换为真实链接
                    addPreference(Preference(ctx).apply {
                        setTitle(R.string.loading_placeholder)
                        isEnabled = false
                        isIconSpaceReserved = false
                    })
                } else {
                    if (config.qqGroup.isNotEmpty())
                        addPreference(R.string.official_group) { openUrl(config.qqGroup) }
                    if (config.douyinGroup.isNotEmpty())
                        addPreference(R.string.douyin_group) { openUrl(config.douyinGroup) }
                    if (config.feedbackUrl.isNotEmpty())
                        addPreference(R.string.feedback) { openUrl(config.feedbackUrl) }
                    if (config.privacyUrl.isNotEmpty())
                        addPreference(R.string.privacy_policy) { openUrl(config.privacyUrl) }
                    if (config.agreementUrl.isNotEmpty())
                        addPreference(R.string.user_agreement) { openUrl(config.agreementUrl) }
                    if (config.shareText.isNotEmpty())
                        addPreference(R.string.share) { shareText(config.shareText) }
                }
                // 赞助入口：依赖 sponsor.enabled；未加载时占位
                when (val sponsor = RemoteRepository.sponsor.value) {
                    null -> addPreference(Preference(ctx).apply {
                        setTitle(R.string.sponsor)
                        isEnabled = false
                        isIconSpaceReserved = false
                    })

                    else -> if (sponsor.enabled) {
                        addPreference(R.string.sponsor) { openSponsor() }
                    }
                }
            }
        }
    }

    private fun showForkNotice() {
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.fork_notice_title)
            .setMessage(R.string.fork_notice_content)
            .setPositiveButton(android.R.string.ok, null)
            .show()
    }

    private fun openUrl(url: String) {
        if (url.isEmpty()) return
        runCatching { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }
    }

    private fun shareText(text: String) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
        }
        startActivity(Intent.createChooser(intent, getString(R.string.share)))
    }

    private suspend fun checkUpdateManually() {
        RemoteRepository.refreshUpdate()
        val update = RemoteRepository.bestUpdate()
        val prefs = AppPrefs.getInstance()
        when {
            update == null ->
                Toast.makeText(requireContext(), R.string.update_latest, Toast.LENGTH_SHORT).show()

            update.versionCode == prefs.remote.ignoredUpdateVersion.getValue() ->
                Toast.makeText(requireContext(), R.string.update_ignored, Toast.LENGTH_SHORT).show()

            else -> RemotePrompts.buildUpdate(requireContext(), update) { vc ->
                prefs.remote.ignoredUpdateVersion.setValue(vc)
            }.show()
        }
    }

    private fun openSponsor() {
        lifecycleScope.launch {
            if (RemoteRepository.sponsor.value == null) RemoteRepository.refreshSponsor()
            val s = RemoteRepository.sponsor.value
            if (s?.enabled == true) showSponsorDialog(s)
            else Toast.makeText(requireContext(), R.string.sponsor_hidden, Toast.LENGTH_SHORT).show()
        }
    }

    private fun showSponsorDialog(s: SponsorInfo) {
        val ctx = requireContext()
        val view = layoutInflater.inflate(R.layout.dialog_sponsor, null)
        val b = org.fcitx.fcitx5.android.databinding.DialogSponsorBinding.bind(view)
        b.title.text = s.title
        b.desc.text = s.desc
        b.go.text = s.buttonText.ifEmpty { getString(R.string.sponsor) }
        b.go.setOnClickListener { openUrl(s.url) }
        if (s.sponsorsTitle.isNotEmpty()) {
            b.sponsorsTitle.visibility = View.VISIBLE
            b.sponsorsTitle.text = s.sponsorsTitle
        }
        if (s.sponsors.isNotEmpty()) {
            b.sponsorList.visibility = View.VISIBLE
            s.sponsors.forEach { item ->
                b.sponsorList.addView(TextView(ctx).apply {
                    text = buildString {
                        append(item.name)
                        if (item.channel.isNotEmpty()) append(" · ${item.channel}")
                        if (item.amount.isNotEmpty()) append("  ${item.amount}")
                    }
                    textSize = 14f
                    setPadding(0, 6, 0, 6)
                })
            }
        }
        if (s.notice.isNotEmpty()) {
            b.notice.visibility = View.VISIBLE
            b.notice.text = s.notice
        }
        AlertDialog.Builder(ctx).setView(view).create().show()
    }
}
