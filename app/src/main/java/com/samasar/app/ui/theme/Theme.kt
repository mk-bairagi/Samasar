package com.samasar.app.ui.theme

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

val LocalNewsColors = staticCompositionLocalOf { DarkNewsColors }

/** Convenience accessor: `NewsTheme.colors.textPrimary`. */
object NewsTheme {
    val colors: NewsColors
        @Composable get() = LocalNewsColors.current
}

@Composable
fun SamasarTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit,
) {
    val target = if (darkTheme) DarkNewsColors else LightNewsColors

    // Cross-fade the palette so the theme toggle reads as one continuous material change
    // rather than a hard swap. Only the values the eye tracks are animated.
    val spec = tween<Color>(durationMillis = 520)
    val canvas by animateColorAsState(target.canvas, spec, label = "canvas")
    val textPrimary by animateColorAsState(target.textPrimary, spec, label = "textPrimary")
    val textSecondary by animateColorAsState(target.textSecondary, spec, label = "textSecondary")
    val textTertiary by animateColorAsState(target.textTertiary, spec, label = "textTertiary")
    val accent by animateColorAsState(target.accent, spec, label = "accent")
    val glassTint by animateColorAsState(target.glassTint, spec, label = "glassTint")
    val divider by animateColorAsState(target.divider, spec, label = "divider")

    val colors = target.copy(
        canvas = canvas,
        textPrimary = textPrimary,
        textSecondary = textSecondary,
        textTertiary = textTertiary,
        accent = accent,
        glassTint = glassTint,
        divider = divider,
    )

    val scheme = if (darkTheme) {
        darkColorScheme(
            primary = colors.accent,
            onPrimary = colors.onAccent,
            background = colors.canvas,
            onBackground = colors.textPrimary,
            surface = colors.canvas,
            onSurface = colors.textPrimary,
        )
    } else {
        lightColorScheme(
            primary = colors.accent,
            onPrimary = colors.onAccent,
            background = colors.canvas,
            onBackground = colors.textPrimary,
            surface = colors.canvas,
            onSurface = colors.textPrimary,
        )
    }

    CompositionLocalProvider(LocalNewsColors provides colors) {
        MaterialTheme(
            colorScheme = scheme,
            typography = NewsTypography,
            content = content,
        )
    }
}
