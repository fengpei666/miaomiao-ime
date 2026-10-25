/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright 2025 MiaoMiao IME Contributors
 */
package org.fcitx.fcitx5.android.ui.main.settings

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import org.fcitx.fcitx5.android.R
import org.fcitx.fcitx5.android.data.remote.RemoteRepository
import org.fcitx.fcitx5.android.databinding.FragmentNoticeDetailBinding
import org.fcitx.fcitx5.android.utils.lazyRoute

class NoticeDetailFragment : Fragment(R.layout.fragment_notice_detail) {

    private val args by lazyRoute<SettingsRoute.NoticeDetail>()
    private var _binding: FragmentNoticeDetailBinding? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val binding = FragmentNoticeDetailBinding.bind(view)
        _binding = binding

        val id = args.id
        // 从已加载的公告列表中按 id 查找（由列表点击进入，数据通常已在内存中）
        viewLifecycleOwner.lifecycleScope.launch {
            RemoteRepository.notice.collect { list ->
                val item = list?.notices?.firstOrNull { it.id == id }
                if (item == null) {
                    binding.title.text = getString(R.string.notice)
                    binding.content.text = ""
                    binding.link.visibility = View.GONE
                    return@collect
                }
                val ctx = requireContext()
                binding.title.text = item.title.ifEmpty { ctx.getString(R.string.notice) }
                binding.content.text = item.content
                // 仅当「链接文字」与「链接地址」都存在时才展示可点击链接（如加群链接）
                if (item.linkText.isNotEmpty() && item.linkUrl.isNotEmpty()) {
                    binding.link.visibility = View.VISIBLE
                    binding.link.text = item.linkText
                    binding.link.setOnClickListener {
                        // 拦截指向本 App 安装包/下载页的链接，避免点击后浏览器直接下载 APK
                        if (RemoteRepository.isSelfAppDownloadUrl(item.linkUrl)) {
                            Toast.makeText(
                                ctx, R.string.notice_link_invalid, Toast.LENGTH_SHORT
                            ).show()
                            return@setOnClickListener
                        }
                        runCatching {
                            ctx.startActivity(
                                Intent(Intent.ACTION_VIEW, Uri.parse(item.linkUrl))
                            )
                        }
                    }
                } else {
                    binding.link.visibility = View.GONE
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
