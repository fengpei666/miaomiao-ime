/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright 2025 MiaoMiao IME Contributors
 */
package org.fcitx.fcitx5.android.ui.main.settings

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import org.fcitx.fcitx5.android.R
import org.fcitx.fcitx5.android.data.remote.RemoteRepository
import org.fcitx.fcitx5.android.databinding.FragmentNoticeBinding
import org.fcitx.fcitx5.android.utils.navigateWithAnim

class NoticeFragment : Fragment(R.layout.fragment_notice) {

    private var _binding: FragmentNoticeBinding? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val binding = FragmentNoticeBinding.bind(view)
        _binding = binding

        val adapter = NoticeAdapter { item ->
            navigateWithAnim(SettingsRoute.NoticeDetail(item.id))
        }
        binding.recycler.layoutManager = LinearLayoutManager(requireContext())
        binding.recycler.adapter = adapter
        // 预加载占位：先展示骨架，数据到达后自动替换
        adapter.submit(null)

        viewLifecycleOwner.lifecycleScope.launch {
            // 若启动预加载尚未完成，这里补拉一次
            if (RemoteRepository.notice.value == null) RemoteRepository.refreshNotice()
            RemoteRepository.notice.collect { data ->
                if (data == null) {
                    adapter.submit(null)
                    binding.empty.visibility = View.GONE
                } else {
                    adapter.submit(data.notices)
                    binding.empty.visibility =
                        if (data.notices.isEmpty()) View.VISIBLE else View.GONE
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
