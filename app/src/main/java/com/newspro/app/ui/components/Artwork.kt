package com.newspro.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.lerp
import com.newspro.app.ui.theme.categoryGradient

/**
 * Stand-in artwork for a story.
 *
 * Generated rather than downloaded, so the app has no image dependency yet and every card still
 * carries a distinct, saturated image the glass can refract. Composition is seeded from the
 * article id, so a given story always looks the same. Swap for real photography by replacing this
 * composable's body with an image loader.
 */
@Composable
fun ArticleArtwork(
    seed: String,
    category: String,
    modifier: Modifier = Modifier,
    scrimStrength: Float = 0f,
) {
    val palette = categoryGradient(category)
    Canvas(modifier.fillMaxSize()) {
        val s = seed.hashCode()
        val base = palette[0]
        val accent = palette[1]

        // Base wash, angled slightly differently per story.
        val tilt = noise(s, 0)
        drawRect(
            Brush.linearGradient(
                colors = listOf(base, lerp(base, accent, 0.55f), accent),
                start = Offset(size.width * tilt * 0.4f, 0f),
                end = Offset(size.width * (1f - tilt * 0.3f), size.height),
            ),
        )

        // Colour pools — these are what create the sense of a photograph rather than a swatch.
        pool(
            center = Offset(size.width * (0.15f + 0.6f * noise(s, 1)), size.height * (0.1f + 0.5f * noise(s, 2))),
            radius = size.maxDimension * (0.35f + 0.3f * noise(s, 3)),
            color = lerp(accent, Color.White, 0.42f),
            alpha = 0.50f,
        )
        pool(
            center = Offset(size.width * (0.4f + 0.55f * noise(s, 4)), size.height * (0.45f + 0.5f * noise(s, 5))),
            radius = size.maxDimension * (0.30f + 0.3f * noise(s, 6)),
            color = lerp(base, Color.Black, 0.45f),
            alpha = 0.42f,
        )
        pool(
            center = Offset(size.width * noise(s, 7), size.height * (0.6f + 0.4f * noise(s, 8))),
            radius = size.maxDimension * (0.24f + 0.22f * noise(s, 9)),
            color = lerp(accent, Color.White, 0.72f),
            alpha = 0.30f,
        )

        // Specular sweep across the surface.
        val sweep = 0.25f + 0.4f * noise(s, 10)
        drawRect(
            Brush.linearGradient(
                colors = listOf(
                    Color.Transparent,
                    Color.White.copy(alpha = 0.13f),
                    Color.Transparent,
                ),
                start = Offset(size.width * (sweep - 0.35f), 0f),
                end = Offset(size.width * (sweep + 0.35f), size.height),
            ),
        )

        if (scrimStrength > 0f) {
            drawRect(
                Brush.verticalGradient(
                    0.30f to Color.Transparent,
                    1.00f to Color.Black.copy(alpha = 0.72f * scrimStrength),
                ),
            )
        }
    }
}

private fun DrawScope.pool(center: Offset, radius: Float, color: Color, alpha: Float) {
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(color.copy(alpha = alpha), Color.Transparent),
            center = center,
            radius = radius,
        ),
        radius = radius,
        center = center,
    )
}

/** Cheap deterministic hash-to-[0,1) so artwork is stable across recompositions and restarts. */
private fun noise(seed: Int, index: Int): Float {
    var x = seed * 374761393 + index * 668265263
    x = (x xor (x shr 13)) * 1274126177
    x = x xor (x shr 16)
    return (x and 0x7FFFFFF).toFloat() / 0x7FFFFFF.toFloat()
}
