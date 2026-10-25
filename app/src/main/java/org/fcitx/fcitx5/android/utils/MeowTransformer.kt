/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2026 Fcitx5 for Android Contributors
 */
package org.fcitx.fcitx5.android.utils

/**
 * 猫娘语气转换器:
 * 1. 内置语癖替换(如 我->本喵、吗->喵)
 * 2. 用户自定义规则,每行一条,格式 `原文=替换文`,
 *    分隔符支持半角 `=` 或全角 `＝`,以 `#`/`＃` 开头的行视为注释
 */
object MeowTransformer {

    /** 内置替换规则,与用户自定义规则同 key 时会被自定义覆盖 */
    private val builtinRules: Map<String, String> = mapOf(
        "我" to "本喵",
        "吗" to "喵",
        "呢" to "喵",
        "呀" to "喵"
    )

    /**
     * 解析自定义规则文本。忽略无法解析的行;
     * 相同原文以后面的规则为准(方便在设置里微调)。
     */
    fun parseRules(raw: String): Map<String, String> = raw.lineSequence()
        .map { it.trim() }
        .filter { it.isNotEmpty() && !it.startsWith("#") && !it.startsWith("＃") }
        .mapNotNull { line ->
            val sep = line.indexOfFirst { it == '=' || it == '＝' }
            if (sep <= 0) return@mapNotNull null
            val from = line.substring(0, sep).trim()
            val to = line.substring(sep + 1).trim()
            if (from.isEmpty()) null else from to to
        }.toMap()

    /**
     * 对文本应用内置 + 自定义替换。
     * 原文按长度降序匹配,避免「我们」先被「我」的规则截断。
     */
    fun replace(text: String, customRulesRaw: String): String {
        if (text.isEmpty()) return text
        val rules = (builtinRules + parseRules(customRulesRaw))
            .entries
            .sortedByDescending { it.key.length }
        var result = text
        for ((from, to) in rules) {
            result = result.replace(from, to)
        }
        return result
    }
}
