package com.newspro.app.ui.util

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb

/**
 * Opens a publisher's page.
 *
 * A Custom Tab rather than an in-app WebView: the reader keeps their login and
 * cookies, the publisher gets a real visit that counts, and the app is not in the
 * business of re-rendering someone else's article.
 */
fun openStoryUrl(context: Context, url: String, toolbarColor: Color) {
    if (url.isBlank()) return
    val uri = runCatching { Uri.parse(url) }.getOrNull() ?: return
    try {
        CustomTabsIntent.Builder()
            .setShowTitle(true)
            .setUrlBarHidingEnabled(true)
            .setDefaultColorSchemeParams(
                androidx.browser.customtabs.CustomTabColorSchemeParams.Builder()
                    .setToolbarColor(toolbarColor.toArgb())
                    .build(),
            )
            .build()
            .launchUrl(context, uri)
    } catch (_: ActivityNotFoundException) {
        // No browser that supports Custom Tabs — fall back to whatever can view it.
        runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, uri)) }
    }
}
