/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2021-2025 Fcitx5 for Android Contributors
 */
package org.fcitx.fcitx5.android.ui.main

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.core.view.MenuProvider
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.preference.Preference
import androidx.preference.PreferenceCategory
import androidx.preference.PreferenceViewHolder
import kotlinx.coroutines.launch
import org.fcitx.fcitx5.android.R
import org.fcitx.fcitx5.android.ui.common.PaddingPreferenceFragment
import org.fcitx.fcitx5.android.ui.main.settings.SettingsRoute
import org.fcitx.fcitx5.android.utils.Const
import org.fcitx.fcitx5.android.utils.addCategory
import org.fcitx.fcitx5.android.utils.addPreference
import org.fcitx.fcitx5.android.data.remote.RemoteRepository
import org.fcitx.fcitx5.android.data.remote.SponsorInfo
import org.fcitx.fcitx5.android.utils.item
import org.fcitx.fcitx5.android.utils.navigateWithAnim

class MainFragment : PaddingPreferenceFragment() {

    private var sponsorPreference: SponsorPreference? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // 保留偏好列表视图，外面套一层：中间列表 + 底部赞助
        val prefView = super.onCreateView(inflater, container, savedInstanceState)!!
        val root = inflater.inflate(R.layout.fragment_home, container, false)
        // 设置列表必须显式撑满 listHost（否则 FrameLayout 默认 wrap_content 会让列表塌缩）
        root.findViewById<ViewGroup>(R.id.listHost).addView(
            prefView,
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
        return root
    }

    override fun onViewCreated(
        view: View, savedInstanceState: Bundle?
    ) {
        super.onViewCreated(view, savedInstanceState)
        // AboutMenuProvider is tied to viewLifecycleOwner, so the about menu items
        // are automatically shown when this Fragment is visible and removed when it's not
        requireActivity().addMenuProvider(
            AboutMenuProvider(), viewLifecycleOwner, Lifecycle.State.STARTED
        )
        setupHomeBannerAndSponsor()
    }

    private fun setupHomeBannerAndSponsor() {
        // 赞助：作为设置列表的最后一项，跟随列表滚动；有数据才插入，无数据则移除
        viewLifecycleOwner.lifecycleScope.launch {
            RemoteRepository.sponsor.collect { s ->
                val screen = preferenceScreen ?: return@collect
                if (s != null && s.enabled) {
                    if (sponsorPreference == null) {
                        sponsorPreference = SponsorPreference(requireContext()).also {
                            it.sponsor = s
                            screen.addPreference(it)
                        }
                    } else {
                        sponsorPreference!!.sponsor = s
                        sponsorPreference!!.refresh()
                    }
                } else {
                    sponsorPreference?.let { screen.removePreference(it) }
                    sponsorPreference = null
                }
            }
        }
    }

    private inner class AboutMenuProvider : MenuProvider {
        override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
            // 铃铛：点击进入公告列表（位于右上角三点左侧）
            menu.item(R.string.notice, R.drawable.ic_baseline_notifications_24,
                showAsAction = true) { navigateWithAnim(SettingsRoute.Notice) }
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS)
            menu.item(R.string.faq) {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(Const.faqUrl)))
            }
            menu.item(R.string.developer) {
                navigateWithAnim(SettingsRoute.Developer)
            }
            menu.item(R.string.about) {
                navigateWithAnim(SettingsRoute.About)
            }
        }

        override fun onMenuItemSelected(menuItem: MenuItem): Boolean = false
    }

    private fun PreferenceCategory.addDestinationPreference(
        @StringRes title: Int,
        @DrawableRes icon: Int,
        @StringRes summary: Int,
        route: SettingsRoute
    ) {
        addPreference(title, summary, icon = icon) {
            navigateWithAnim(route)
        }
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        preferenceScreen = preferenceManager.createPreferenceScreen(requireContext()).apply {
            // 按功能域分组,并按使用频率从高到低排列
            addCategory(R.string.home_category_input) {
                addDestinationPreference(
                    R.string.input_methods,
                    R.drawable.ic_baseline_language_24,
                    R.string.home_summary_input_methods,
                    SettingsRoute.InputMethodList
                )
                addDestinationPreference(
                    R.string.virtual_keyboard,
                    R.drawable.ic_baseline_keyboard_24,
                    R.string.home_summary_virtual_keyboard,
                    SettingsRoute.VirtualKeyboard
                )
                addDestinationPreference(
                    R.string.candidates_window,
                    R.drawable.ic_baseline_list_alt_24,
                    R.string.home_summary_candidates_window,
                    SettingsRoute.CandidatesWindow
                )
            }
            addCategory(R.string.home_category_appearance) {
                addDestinationPreference(
                    R.string.theme,
                    R.drawable.ic_baseline_palette_24,
                    R.string.home_summary_theme,
                    SettingsRoute.Theme
                )
            }
            addCategory(R.string.home_category_meow) {
                addDestinationPreference(
                    R.string.meow_feature,
                    R.drawable.ic_baseline_auto_awesome_24,
                    R.string.home_summary_meow,
                    SettingsRoute.Meow
                )
            }
            addCategory(R.string.home_category_assistant) {
                addDestinationPreference(
                    R.string.emoji_and_symbols,
                    R.drawable.ic_baseline_emoji_symbols_24,
                    R.string.home_summary_symbol,
                    SettingsRoute.Symbol
                )
                addDestinationPreference(
                    R.string.clipboard,
                    R.drawable.ic_clipboard,
                    R.string.home_summary_clipboard,
                    SettingsRoute.Clipboard
                )
            }
            addCategory(R.string.home_category_extend) {
                addDestinationPreference(
                    R.string.plugins,
                    R.drawable.ic_baseline_android_24,
                    R.string.home_summary_plugins,
                    SettingsRoute.Plugin
                )
                addDestinationPreference(
                    R.string.addons,
                    R.drawable.ic_baseline_extension_24,
                    R.string.home_summary_addons,
                    SettingsRoute.AddonList
                )
                addDestinationPreference(
                    R.string.global_options,
                    R.drawable.ic_baseline_tune_24,
                    R.string.home_summary_global_options,
                    SettingsRoute.GlobalConfig
                )
                addDestinationPreference(
                    R.string.advanced,
                    R.drawable.ic_baseline_more_horiz_24,
                    R.string.home_summary_advanced,
                    SettingsRoute.Advanced
                )
            }
        }
    }
}

/**
 * 赞助卡片：作为设置列表的最后一项（自定义布局），跟随列表滚动。
 * 有赞助数据时由 MainFragment 动态插入，无数据则移除；点击打开赞助链接。
 */
private class SponsorPreference(context: Context) : Preference(context) {

    var sponsor: SponsorInfo? = null

    init {
        layoutResource = R.layout.home_sponsor
        isSelectable = true
    }

    /** 数据更新后通知列表重绑 */
    fun refresh() = notifyChanged()

    override fun onBindViewHolder(holder: PreferenceViewHolder) {
        super.onBindViewHolder(holder)
        val v = holder.itemView
        val s = sponsor ?: return
        v.findViewById<TextView>(R.id.sponsorTitle).text =
            s.title.ifEmpty { v.context.getString(R.string.sponsor) }
        v.findViewById<TextView>(R.id.sponsorDesc).text = s.desc
        v.setOnClickListener {
            if (s.url.isNotEmpty()) {
                runCatching {
                    v.context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(s.url)))
                }
            }
        }
    }
}