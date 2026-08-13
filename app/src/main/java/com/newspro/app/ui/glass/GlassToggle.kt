package com.newspro.app.ui.glass

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.newspro.app.ui.theme.NewsTheme
import kotlin.math.abs

/**
 * Liquid glass switch.
 *
 * The track is a real pane of glass with the accent colour bled in underneath it. The thumb is
 * painted on top as a bright lens, and it deforms: the faster it travels, the more it stretches
 * along its direction of motion and flattens across it, settling back to a circle when the spring
 * comes to rest. That deformation is what makes the control read as liquid rather than as a
 * sliding dot.
 */
@Composable
fun GlassToggle(
    backdrop: BackdropState,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = NewsTheme.colors
    val accent = colors.accent
    val rim = colors.glassRim

    val progress = remember { Animatable(if (checked) 1f else 0f) }
    LaunchedEffect(checked) {
        progress.animateTo(
            targetValue = if (checked) 1f else 0f,
            animationSpec = spring(dampingRatio = 0.55f, stiffness = 300f),
        )
    }

    RefractionSync(backdrop) { progress.value }

    Box(
        modifier = modifier
            .width(60.dp)
            .height(34.dp)
            .pressBounce(pressedScale = 0.93f) { onCheckedChange(!checked) }
            .liquidGlass(
                backdrop = backdrop,
                cornerRadius = PillCorner,
                style = GlassStyle.Control.copy(blur = 12.dp, refraction = 11.dp, thickness = 10.dp),
                refracted = { panel ->
                    val p = progress.value.coerceIn(0f, 1f)
                    if (p > 0.01f) {
                        drawRect(
                            brush = Brush.horizontalGradient(
                                colors = listOf(
                                    accent.copy(alpha = 0.95f * p),
                                    accent.copy(alpha = 0.70f * p),
                                ),
                            ),
                            size = panel,
                        )
                    }
                },
            ),
    ) {
        Canvas(Modifier.matchParentSize()) {
            val p = progress.value
            val v = progress.velocity

            val inset = 4.dp.toPx()
            val r = (size.height - inset * 2f) / 2f
            val travel = size.width - inset * 2f - r * 2f
            val cx = inset + r + p.coerceIn(0f, 1f) * travel
            val cy = size.height / 2f

            // Velocity-driven deformation, capped so a fast double-tap cannot tear it apart.
            val stretch = (abs(v) / 9f).coerceAtMost(0.30f)
            val rx = r * (1f + stretch)
            val ry = r * (1f - stretch * 0.62f)

            // Contact shadow.
            drawOval(
                color = Color.Black.copy(alpha = 0.22f),
                topLeft = Offset(cx - rx, cy - ry + 2.dp.toPx()),
                size = Size(rx * 2f, ry * 2f),
            )

            // Lens body.
            drawOval(
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.98f),
                        Color.White.copy(alpha = 0.86f),
                        rim.copy(alpha = 0.70f),
                    ),
                    start = Offset(cx - rx, cy - ry),
                    end = Offset(cx + rx, cy + ry),
                ),
                topLeft = Offset(cx - rx, cy - ry),
                size = Size(rx * 2f, ry * 2f),
            )

            // Specular cap along the lit edge.
            drawOval(
                brush = Brush.verticalGradient(
                    colors = listOf(Color.White, Color.White.copy(alpha = 0f)),
                    startY = cy - ry,
                    endY = cy,
                ),
                topLeft = Offset(cx - rx * 0.72f, cy - ry * 0.86f),
                size = Size(rx * 1.44f, ry * 0.80f),
            )

            drawOval(
                color = Color.White.copy(alpha = 0.55f),
                topLeft = Offset(cx - rx, cy - ry),
                size = Size(rx * 2f, ry * 2f),
                style = Stroke(width = 1.dp.toPx()),
            )
        }
    }
}
