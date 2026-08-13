package com.newspro.app.ui.glass

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer

/**
 * Press feedback for glass controls.
 *
 * Glass has no ink ripple — it has weight. Pressing sinks the pane slightly and releasing lets it
 * spring back past its resting size, which is what makes the material feel physical.
 */
fun Modifier.pressBounce(
    enabled: Boolean = true,
    pressedScale: Float = 0.955f,
    onClick: () -> Unit,
): Modifier = composed {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) pressedScale else 1f,
        animationSpec = spring(dampingRatio = 0.42f, stiffness = 850f),
        label = "pressBounce",
    )
    this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .clickable(
            interactionSource = interaction,
            indication = null,
            enabled = enabled,
            onClick = onClick,
        )
}

/**
 * Keeps a panel's refracted content in step with the material.
 *
 * Feed [value] whatever animates *inside* the glass — a selection indicator's position, a toggle's
 * travel. Backdrop content changing already forces a re-record; this covers the case where only
 * the embedded content moved.
 */
@Composable
fun RefractionSync(backdrop: BackdropState, value: () -> Float) {
    LaunchedEffect(backdrop) {
        snapshotFlow(value).collect { backdrop.invalidateRefraction() }
    }
}

/**
 * Scroll position shared between a screen's list and the floating chrome above it, so the top bar
 * can react to content moving underneath without owning the list itself.
 */
@Stable
class ChromeState {
    var scrolled by mutableFloatStateOf(0f)
        internal set

    fun report(offsetPx: Float) {
        scrolled = offsetPx
    }
}
