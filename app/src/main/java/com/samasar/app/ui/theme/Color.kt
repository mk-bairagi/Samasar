package com.samasar.app.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

/**
 * Palette for Samasar.
 *
 * Glass is a *relationship* between a surface and what sits behind it, so the palette leads with
 * the ambient field (the drifting colour blobs painted behind everything). Those colours are what
 * the glass bends, tints and disperses — a flat background makes even a perfect shader look dead.
 */
@Immutable
data class NewsColors(
    val isDark: Boolean,
    val canvas: Color,
    val ambient: List<Color>,
    val textPrimary: Color,
    val textSecondary: Color,
    val textTertiary: Color,
    val accent: Color,
    val accentSoft: Color,
    val accentGradient: List<Color>,
    val onAccent: Color,
    val glassTint: Color,
    val glassRim: Color,
    val glassRimShadow: Color,
    val divider: Color,
    val scrim: Color,
    val shadow: Color,
)

val DarkNewsColors = NewsColors(
    isDark = true,
    canvas = Color(0xFF06060D),
    ambient = listOf(
        Color(0xFF5B2BD9),
        Color(0xFF1152C7),
        Color(0xFFB4207A),
        Color(0xFF0E9C8F),
    ),
    textPrimary = Color(0xFFF6F6FB),
    textSecondary = Color(0xFFA8A8C0),
    textTertiary = Color(0xFF6E6E88),
    accent = Color(0xFF7C93FF),
    accentSoft = Color(0xFF3B3F7A),
    accentGradient = listOf(Color(0xFF6E8BFF), Color(0xFFB06CFF)),
    onAccent = Color(0xFF0A0A14),
    glassTint = Color(0xFFEEF0FF),
    glassRim = Color(0xFFFFFFFF),
    glassRimShadow = Color(0xFF000000),
    divider = Color(0x1FFFFFFF),
    scrim = Color(0xCC06060D),
    shadow = Color(0xFF000000),
)

val LightNewsColors = NewsColors(
    isDark = false,
    canvas = Color(0xFFEDEBF6),
    ambient = listOf(
        Color(0xFFB9AEE8),
        Color(0xFF9DBAEC),
        Color(0xFFE9B9D6),
        Color(0xFFA9DCD2),
    ),
    textPrimary = Color(0xFF13131E),
    textSecondary = Color(0xFF5B5B72),
    textTertiary = Color(0xFF8C8CA4),
    accent = Color(0xFF3F5BEF),
    accentSoft = Color(0xFFC9D2FF),
    accentGradient = listOf(Color(0xFF3F5BEF), Color(0xFF8B4DE8)),
    onAccent = Color(0xFFFFFFFF),
    glassTint = Color(0xFFFFFFFF),
    glassRim = Color(0xFFFFFFFF),
    glassRimShadow = Color(0xFF2A2A45),
    divider = Color(0x14000000),
    scrim = Color(0xCCEDEBF6),
    shadow = Color(0xFF3A3A5C),
)

/**
 * Accent pairs used by generated artwork.
 *
 * The pipeline does not classify stories by topic, so these are keyed off the
 * publisher instead — a stable hash means a given source always looks the same,
 * which reads as intentional rather than random.
 */
val ArtworkPalettes: List<List<Color>> = listOf(
    listOf(Color(0xFF6E8BFF), Color(0xFFB06CFF)),
    listOf(Color(0xFF2BA3E0), Color(0xFF1F5FD0)),
    listOf(Color(0xFF7B4DFF), Color(0xFFD34DFF)),
    listOf(Color(0xFF12B886), Color(0xFF0E8FA8)),
    listOf(Color(0xFFFF7A45), Color(0xFFFFC24D)),
    listOf(Color(0xFFFF4D8D), Color(0xFFFF8A4D)),
    listOf(Color(0xFFFFB84D), Color(0xFFE0517A)),
)

fun paletteFor(key: String): List<Color> {
    if (key.isEmpty()) return ArtworkPalettes[0]
    val hash = key.fold(7) { acc, c -> acc * 31 + c.code }
    return ArtworkPalettes[(hash % ArtworkPalettes.size + ArtworkPalettes.size) % ArtworkPalettes.size]
}
