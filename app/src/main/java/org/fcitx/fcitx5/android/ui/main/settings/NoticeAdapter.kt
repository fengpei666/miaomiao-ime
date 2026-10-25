/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright 2025 MiaoMiao IME Contributors
 */
package org.fcitx.fcitx5.android.ui.main.settings

import android.content.Intent
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import org.fcitx.fcitx5.android.R
import org.fcitx.fcitx5.android.data.remote.NoticeItem
import org.fcitx.fcitx5.android.data.remote.RemoteRepository
import org.fcitx.fcitx5.android.databinding.ItemNoticeBinding

class NoticeAdapter(
    private val onItemClick: (NoticeItem) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    /** null 表示数据未到达，展示骨架占位 */
    private var items: List<NoticeItem>? = null

    fun submit(data: List<NoticeItem>?) {
        items = data
        notifyDataSetChanged()
    }

    override fun getItemViewType(position: Int) =
        if (items == null) VIEW_SKELETON else VIEW_ITEM

    override fun getItemCount() = if (items == null) SKELETON_COUNT else items!!.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return if (viewType == VIEW_SKELETON) {
            SkeletonHolder(inflater.inflate(R.layout.item_notice_skeleton, parent, false))
        } else {
            ItemHolder(inflater.inflate(R.layout.item_notice, parent, false))
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is ItemHolder && items != null) {
            holder.bind(items!![position], onItemClick)
        }
    }

    class SkeletonHolder(itemView: View) : RecyclerView.ViewHolder(itemView)

    class ItemHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val binding = ItemNoticeBinding.bind(itemView)

        fun bind(item: NoticeItem, onItemClick: (NoticeItem) -> Unit) {
            val ctx = itemView.context
            binding.title.text = item.title.ifEmpty { ctx.getString(R.string.notice) }
            binding.content.text = item.content
            binding.type.text = item.type
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
            // 整条公告点击进入详情
            itemView.setOnClickListener { onItemClick(item) }
        }
    }

    companion object {
        private const val VIEW_SKELETON = 0
        private const val VIEW_ITEM = 1
        private const val SKELETON_COUNT = 4
    }
}
