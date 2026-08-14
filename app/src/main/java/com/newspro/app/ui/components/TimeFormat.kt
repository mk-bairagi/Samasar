package com.newspro.app.ui.components

/**
 * Compact relative time.
 *
 * Deliberately short — these sit in dense meta rows next to a publisher name, so
 * "5h" earns its space where "5 hours ago" does not.
 */
fun relativeTime(epochSeconds: Long, lang: String, nowSeconds: Long = System.currentTimeMillis() / 1000): String {
    if (epochSeconds <= 0) return ""
    val delta = (nowSeconds - epochSeconds).coerceAtLeast(0)
    return when {
        delta < 60 -> when (lang) {
            "hi" -> "अभी"
            "gu" -> "હમણાં"
            else -> "now"
        }
        delta < 3600 -> "${delta / 60}m"
        delta < 86_400 -> "${delta / 3600}h"
        else -> "${delta / 86_400}d"
    }
}
