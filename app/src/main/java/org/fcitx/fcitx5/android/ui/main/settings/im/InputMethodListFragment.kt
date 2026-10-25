/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2021-2025 Fcitx5 for Android Contributors
 */
package org.fcitx.fcitx5.android.ui.main.settings.im

import android.graphics.Typeface
import android.os.Build
import android.view.View
import androidx.core.graphics.ColorUtils
import androidx.core.view.doOnAttach
import androidx.lifecycle.Lifecycle
import com.google.android.material.snackbar.Snackbar
import org.fcitx.fcitx5.android.R
import org.fcitx.fcitx5.android.core.InputMethodEntry
import org.fcitx.fcitx5.android.core.SubtypeManager
import org.fcitx.fcitx5.android.daemon.launchOnReady
import org.fcitx.fcitx5.android.ui.common.BaseDynamicListUi
import org.fcitx.fcitx5.android.ui.common.DynamicListUi
import org.fcitx.fcitx5.android.ui.common.OnItemChangedListener
import org.fcitx.fcitx5.android.ui.main.EditDeleteMenuProvider
import org.fcitx.fcitx5.android.ui.main.MainViewModel.ButtonMode
import org.fcitx.fcitx5.android.ui.main.settings.ProgressFragment
import org.fcitx.fcitx5.android.ui.main.settings.SettingsRoute
import org.fcitx.fcitx5.android.utils.navigateWithAnim
import splitties.resources.styledColor

/**
 * 输入法列表。
 *
 * 关于 fcitx 的语义（决定了这里的实现方式）：
 * - group 中的**第一项**是“不启用输入法”的槽位（通常是 keyboard-us，即英文直通），
 *   `Instance::setCurrentInputMethod` 对索引 0 的处理是 `setActive(false)`；
 * - 真正决定“键盘激活时使用哪个输入法”的是 group 的 **DefaultIM**
 *   （`Instance::inputMethod` 在 isActive 时返回 defaultInputMethod）。
 *
 * 因此“默认语言”必须通过 [org.fcitx.fcitx5.android.core.FcitxAPI.setDefaultIme] 写入 profile，
 * 而不能靠“把某一项拖到第一位”——那样反而会触发 fcitx 的“关闭输入法”分支。
 */
class InputMethodListFragment : ProgressFragment(), OnItemChangedListener<InputMethodEntry> {

    private lateinit var ui: BaseDynamicListUi<InputMethodEntry>

    /**
     * 当前默认输入法（= fcitx group 的 DefaultIM），用于标记列表项
     */
    private var defaultUniqueName: String = ""

    companion object {
        /**
         * 操作提示只在本次进程内显示一次
         */
        private var hintShown = false
    }

    /**
     * 刷新“默认”标记（默认项变化或条目增删后调用）
     */
    private fun refreshMarks() {
        if (::ui.isInitialized && ui.itemCount > 0) {
            ui.notifyItemRangeChanged(0, ui.itemCount)
        }
    }

    /**
     * 把当前列表顺序与默认输入法同步给 fcitx。
     *
     * 只需写 enabled 列表：native 侧会保留合法的 DefaultIM，
     * 若默认项被删除则由 fcitx 自行修正，这里再把修正后的值读回来。
     */
    private fun applyOrder() {
        if (!isInitialized) return
        fcitx.launchOnReady { f ->
            f.setEnabledIme(ui.entries.map { it.uniqueName }.toTypedArray())
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                SubtypeManager.syncWith(f.enabledIme())
            }
            val latestDefault = f.defaultIme()
            if (latestDefault != defaultUniqueName) {
                defaultUniqueName = latestDefault
                ui.root.post { refreshMarks() }
            }
        }
    }

    /**
     * fcitx 把输入法组的第一项当作“不启用输入法”的槽位（键盘直通，用于直接输入英文），
     * 所以键盘布局必须排在第一位。历史 profile / 旧版本的拖动可能让它不在第一位，
     * 需要在进入页面时纠正，否则第一项的中文输入法会被当作“关闭输入法”处理。
     */
    private fun normalizeOrder(entries: List<InputMethodEntry>): List<InputMethodEntry> {
        if (entries.isEmpty()) return entries
        if (entries.first().uniqueName.startsWith("keyboard-")) return entries
        val keyboards = entries.filter { it.uniqueName.startsWith("keyboard-") }
        if (keyboards.isEmpty()) return entries
        return keyboards + entries.filterNot { it.uniqueName.startsWith("keyboard-") }
    }

    private fun setDefault(entry: InputMethodEntry) {
        defaultUniqueName = entry.uniqueName
        refreshMarks()
        fcitx.launchOnReady { it.setDefaultIme(entry.uniqueName) }
        Snackbar.make(
            ui.root,
            requireContext().getString(R.string.ime_set_default, entry.displayName),
            Snackbar.LENGTH_SHORT
        ).show()
    }

    override suspend fun initialize(): View {
        val available = fcitx.runOnReady { availableIme().toSet() }
        val initialEnabled = fcitx.runOnReady { enabledIme().toList() }
        val ordered = normalizeOrder(initialEnabled)
        defaultUniqueName = fcitx.runOnReady { defaultIme() }
        if (ordered != initialEnabled) {
            // 顺序不合法（第一项不是键盘布局），写回 fcitx 并读回被修正后的默认输入法
            fcitx.runOnReady {
                setEnabledIme(ordered.map { it.uniqueName }.toTypedArray())
                defaultUniqueName = defaultIme()
            }
        }
        ui = requireContext().DynamicListUi(
            mode = BaseDynamicListUi.Mode.ChooseOne {
                (available - entries.toSet()).toTypedArray()
            },
            initialEntries = ordered,
            enableOrder = true,
            initSettingsButton = { entry ->
                setOnClickListener {
                    navigateWithAnim(
                        SettingsRoute.InputMethodConfig(entry.displayName, entry.uniqueName),
                    )
                }
            },
            show = { it.displayName }
        )
        // 第一项是 fcitx 的“英文直通”槽位，不允许被拖走、顶替或删除
        ui.protectFirstItem = true
        ui.removable = { entry -> entry.uniqueName != ui.entries.firstOrNull()?.uniqueName }

        val ctx = requireContext()
        val normalBackground = ctx.styledColor(android.R.attr.colorBackground)
        val defaultBackground = ColorUtils.setAlphaComponent(
            ctx.styledColor(android.R.attr.colorAccent), 0x26
        )
        ui.entryDecorator = { holder, entry, position ->
            val isDefault =
                !ui.multiselect && defaultUniqueName.isNotEmpty() && entry.uniqueName == defaultUniqueName
            holder.nameText.text = if (isDefault) {
                ctx.getString(R.string.ime_default_entry, entry.displayName)
            } else {
                entry.displayName
            }
            holder.nameText.setTypeface(null, if (isDefault) Typeface.BOLD else Typeface.NORMAL)
            holder.itemView.setBackgroundColor(
                if (isDefault) defaultBackground else normalBackground
            )
        }
        // 点按某项：设为默认（写 DefaultIM，重启/重开输入框后依然生效）
        ui.onEntryClick = { entry ->
            when {
                // 首项是 keyboard-us 之类，fcitx 会把它当作“关闭输入法”，不能作为默认语言
                ui.indexItem(entry) == 0 -> Snackbar.make(
                    ui.root, R.string.ime_default_first_item, Snackbar.LENGTH_SHORT
                ).show()

                entry.uniqueName == defaultUniqueName -> Unit

                else -> setDefault(entry)
            }
        }
        ui.addOnItemChangedListener(this@InputMethodListFragment)
        ui.setViewModel(viewModel)
        viewModel.toolbarButton.value =
            if (ui.entries.isNotEmpty()) ButtonMode.EDIT else ButtonMode.NONE
        requireActivity().addMenuProvider(
            EditDeleteMenuProvider(
                buttonMode = viewModel.toolbarButton,
                editButtonAction = { ui.enterMultiSelect(requireActivity().onBackPressedDispatcher) },
                deleteButtonAction = { ui.deleteSelected(); ui.exitMultiSelect() },
                menuHost = requireActivity(),
                lifecycleOwner = viewLifecycleOwner,
            ),
            viewLifecycleOwner,
            Lifecycle.State.STARTED
        )
        if (!hintShown) {
            ui.root.doOnAttach {
                hintShown = true
                Snackbar.make(ui.root, R.string.ime_default_hint, Snackbar.LENGTH_LONG).show()
            }
        }
        return ui.root
    }

    override fun onStop() {
        if (::ui.isInitialized) {
            ui.exitMultiSelect()
        }
        super.onStop()
    }

    override fun onDestroy() {
        if (::ui.isInitialized) {
            ui.removeItemChangedListener()
        }
        super.onDestroy()
    }

    override fun onItemSwapped(fromIdx: Int, toIdx: Int, item: InputMethodEntry) {
        applyOrder()
    }

    override fun onItemAdded(idx: Int, item: InputMethodEntry) {
        applyOrder()
    }

    override fun onItemRemoved(idx: Int, item: InputMethodEntry) {
        applyOrder()
    }

    override fun onItemRemovedBatch(indexed: List<Pair<Int, InputMethodEntry>>) {
        applyOrder()
    }

    override fun onItemUpdated(idx: Int, old: InputMethodEntry, new: InputMethodEntry) {
        applyOrder()
    }
}
