package com.newspro.app.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.dp
import com.newspro.app.ui.theme.NewsTheme
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * The field the glass lives on.
 *
 * Two things here are doing real work for the material. The drifting colour blobs give the panels
 * something saturated to bend, and the faint rectilinear grid gives them something *straight* to
 * bend — a curved line refracting at a rim is the cue that sells glass instantly, which is why the
 * reference renders all sit on gridded backdrops.
 */
@Composable
fun AmbientBackground(modifier: Modifier = Modifier) {
    val colors = NewsTheme.colors
    val transition = rememberInfiniteTransition(label = "ambient")

    // Deliberately coprime periods, so the field never visibly loops.
    val p1 by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(61_000, easing = LinearEasing)),
        label = "p1",
    )
    val p2 by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(83_000, easing = LinearEasing)),
        label = "p2",
    )
    val p3 by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(107_000, easing = LinearEasing)),
        label = "p3",
    )

    val blobAlpha = if (colors.isDark) 0.55f else 0.70f
    val gridAlpha = if (colors.isDark) 0.055f else 0.075f

    Canvas(modifier) {
        drawRect(colors.canvas)

        val w = size.width
        val h = size.height
        val tau = (2 * PI).toFloat()
        val unit = maxOf(w, h)

        drawBlob(
            center = Offset(w * (0.22f + 0.16f * cos(p1 * tau)), h * (0.18f + 0.10f * sin(p1 * tau))),
            radius = unit * 0.62f,
            color = colors.ambient[0],
            alpha = blobAlpha,
        )
        drawBlob(
            center = Offset(w * (0.86f + 0.12f * sin(p2 * tau)), h * (0.34f + 0.14f * cos(p2 * tau))),
            radius = unit * 0.54f,
            color = colors.ambient[1],
            alpha = blobAlpha * 0.92f,
        )
        drawBlob(
            center = Offset(w * (0.14f + 0.18f * sin(p3 * tau)), h * (0.78f + 0.12f * cos(p3 * tau))),
            radius = unit * 0.58f,
            color = colors.ambient[2],
            alpha = blobAlpha * 0.80f,
        )
        drawBlob(
            center = Offset(w * (0.74f + 0.14f * cos(p2 * tau + 2f)), h * (0.92f + 0.08f * sin(p1 * tau))),
            radius = unit * 0.50f,
            color = colors.ambient[3],
            alpha = blobAlpha * 0.66f,
        )

        drawGrid(colors.textPrimary.copy(alpha = gridAlpha), spacing = 58.dp.toPx())

        // Corner falloff keeps the ambient field from washing out the status bar area.
        drawRect(
            Brush.verticalGradient(
                0f to colors.canvas.copy(alpha = 0.55f),
                0.22f to Color.Transparent,
                0.80f to Color.Transparent,
                1f to colors.canvas.copy(alpha = 0.45f),
            ),
        )
    }
}

private fun DrawScope.drawBlob(center: Offset, radius: Float, color: Color, alpha: Float) {
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                color.copy(alpha = alpha),
                color.copy(alpha = alpha * 0.45f),
                Color.Transparent,
            ),
            center = center,
            radius = radius,
        ),
        radius = radius,
        center = center,
    )
}

private fun DrawScope.drawGrid(color: Color, spacing: Float) {
    val step = spacing.coerceAtLeast(24f)
    var x = 0f
    while (x <= size.width) {
        drawLine(color, Offset(x, 0f), Offset(x, size.height), strokeWidth = 1f)
        x += step
    }
    var y = 0f
    while (y <= size.height) {
        drawLine(color, Offset(0f, y), Offset(size.width, y), strokeWidth = 1f)
        y += step
    }
}
