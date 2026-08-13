package com.newspro.app.ui.glass

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.newspro.app.ui.theme.NewsTheme
import androidx.compose.material3.Icon as M3Icon
import kotlin.math.abs

@Immutable
data class NavItem(
    val route: String,
    val label: String,
    val icon: ImageVector,
)

/**
 * The floating navigation bar.
 *
 * This is the component the reference renders are really about — a shape passing *under* a pane of
 * glass and being bent by it. Three layers stack up:
 *
 *  - The accent glow is drawn **inside** the glass, so the shader blurs and refracts it along with
 *    the backdrop. As it slides past the capsule's rounded ends it stretches and smears exactly
 *    the way the blue bar does in the reference.
 *  - A crisp selection capsule sits **on** the glass, giving the selected tab a hard edge to be
 *    read against.
 *  - Icons and labels sit above everything, unblurred, so they stay legible.
 *
 * Both moving layers deform with the spring's velocity: they stretch along the direction of travel
 * and recover when the spring settles.
 */
@Composable
fun GlassNavBar(
    backdrop: BackdropState,
    items: List<NavItem>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = NewsTheme.colors
    val accent = colors.accent
    val rim = colors.glassRim

    val indicator = remember { Animatable(selectedIndex.toFloat()) }
    LaunchedEffect(selectedIndex) {
        indicator.animateTo(
            targetValue = selectedIndex.toFloat(),
            animationSpec = spring(dampingRatio = 0.62f, stiffness = 330f),
        )
    }

    // The glow lives inside the glass, so the backdrop has to re-record as the spring travels.
    RefractionSync(backdrop) { indicator.value }

    val count = items.size

    Box(
        modifier = modifier
            .height(68.dp)
            .liquidGlass(
                backdrop = backdrop,
                cornerRadius = PillCorner,
                style = GlassStyle.Chrome,
                refracted = { panel ->
                    val slot = panel.width / count
                    val cx = slot * (indicator.value + 0.5f)
                    val stretch = (abs(indicator.velocity) / 26f).coerceAtMost(0.42f)
                    val glowW = slot * (0.94f + stretch)
                    val glowH = panel.height * (0.90f - stretch * 0.30f)

                    drawOval(
                        // Tight and hot in the middle: a wide, soft accent pool washes out once
                        // the chrome blur gets hold of it and disappears into the ambient field.
                        brush = Brush.radialGradient(
                            colors = listOf(
                                lerp(accent, Color.White, 0.40f),
                                accent,
                                accent.copy(alpha = 0.35f),
                                Color.Transparent,
                            ),
                            center = Offset(cx, panel.height / 2f),
                            radius = glowW * 0.50f,
                        ),
                        topLeft = Offset(cx - glowW / 2f, (panel.height - glowH) / 2f),
                        size = Size(glowW, glowH),
                    )
                },
            ),
    ) {
        // Selection capsule, sharp, on the surface of the glass.
        Canvas(Modifier.matchParentSize()) {
            val slot = size.width / count
            val cx = slot * (indicator.value + 0.5f)
            val stretch = (abs(indicator.velocity) / 26f).coerceAtMost(0.42f)

            val capW = slot * (0.86f + stretch * 0.9f)
            val capH = (size.height - 11.dp.toPx()) * (1f - stretch * 0.22f)
            val topLeft = Offset(cx - capW / 2f, (size.height - capH) / 2f)
            val corner = CornerRadius(capH / 2f)

            drawRoundRect(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        rim.copy(alpha = 0.22f),
                        rim.copy(alpha = 0.08f),
                    ),
                    startY = topLeft.y,
                    endY = topLeft.y + capH,
                ),
                topLeft = topLeft,
                size = Size(capW, capH),
                cornerRadius = corner,
            )
            drawRoundRect(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        rim.copy(alpha = 0.65f),
                        rim.copy(alpha = 0.14f),
                        rim.copy(alpha = 0.38f),
                    ),
                    startY = topLeft.y,
                    endY = topLeft.y + capH,
                ),
                topLeft = topLeft,
                size = Size(capW, capH),
                cornerRadius = corner,
                style = Stroke(width = 1.dp.toPx()),
            )
        }

        Row(Modifier.fillMaxSize()) {
            items.forEachIndexed { index, item ->
                NavCell(
                    item = item,
                    selected = index == selectedIndex,
                    onClick = { onSelect(index) },
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                )
            }
        }
    }
}

@Composable
private fun NavCell(
    item: NavItem,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = NewsTheme.colors
    val lift by animateFloatAsState(
        targetValue = if (selected) 1f else 0f,
        animationSpec = spring(dampingRatio = 0.52f, stiffness = 400f),
        label = "navLift",
    )
    val tint by animateColorAsState(
        targetValue = if (selected) colors.textPrimary else colors.textSecondary,
        label = "navTint",
    )
    val interaction = remember { MutableInteractionSource() }

    Column(
        modifier = modifier.selectable(
            selected = selected,
            interactionSource = interaction,
            indication = null,
            onClick = onClick,
        ),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        M3Icon(
            imageVector = item.icon,
            contentDescription = item.label,
            tint = tint,
            modifier = Modifier
                .size(23.dp)
                .graphicsLayer {
                    val s = 1f + 0.10f * lift
                    scaleX = s
                    scaleY = s
                    translationY = -2.dp.toPx() * lift
                },
        )
        Text(
            text = item.label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = tint,
            modifier = Modifier.graphicsLayer {
                alpha = 0.65f + 0.35f * lift
                translationY = -1.dp.toPx() * lift
            },
        )
    }
}
