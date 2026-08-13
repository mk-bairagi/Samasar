package com.newspro.app.ui.glass

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.unit.IntSize

/**
 * Supplies glass panels with the imagery they refract.
 *
 * ### Why panels do not simply share one recorded layer
 *
 * The obvious design — record the screen once, then have every panel draw that layer into its own
 * effect layer — does not survive contact with Skia. A `RuntimeShader` that samples its child at
 * anything other than the identity coordinate forces Skia to rasterise that child into an offscreen
 * texture, and **nested RenderNodes are dropped when it does**. Direct draw commands come through;
 * a `drawLayer` call does not. The panel ends up sampling an empty texture and renders black.
 *
 * So the flow is inverted. Panels register themselves here, and the backdrop records the content
 * *directly into each panel's own layer* during its own draw pass. Every panel layer therefore
 * holds nothing but primitive draw commands, which materialise correctly.
 *
 * The cost is one extra walk of the content draw tree per registered panel, which is why true
 * refracting glass is reserved for the few floating surfaces that earn it — see
 * [Modifier.frostedSurface] for the cheap variant used by small controls.
 */
@Stable
class BackdropState internal constructor() {

    /** Position of the backdrop root in the window, so panels can offset themselves against it. */
    internal var origin: Offset by mutableStateOf(Offset.Zero)

    /**
     * Bumped every time the backdrop re-records.
     *
     * Panels read this inside their draw block, which is what repaints them when the feed scrolls
     * underneath. Writing state during the draw phase schedules one more frame; it cannot loop,
     * because nothing on the backdrop's own draw path reads it.
     */
    internal var version by mutableIntStateOf(0)

    /**
     * Bumped by panels whose *refracted* content animates on its own clock.
     *
     * Backdrop content invalidating (a scroll, the drifting ambient field) naturally re-runs the
     * recording. Something like the nav bar's selection glow does not: it lives inside the panel
     * layer, but the spring driving it changes nothing the backdrop's draw depends on, so without
     * this nudge the layer keeps whatever position was recorded last and the glow lags behind the
     * capsule. The backdrop reads this in its draw, so a bump forces a fresh record.
     */
    private var pulse by mutableIntStateOf(0)

    /** Ask for a re-record because refracted content changed. Safe to call every frame. */
    fun invalidateRefraction() {
        pulse++
    }

    internal fun observePulse(): Int = pulse

    private val panels = mutableListOf<GlassPanel>()

    internal fun register(panel: GlassPanel) {
        if (panels.none { it === panel }) panels += panel
    }

    internal fun unregister(panel: GlassPanel) {
        panels.removeAll { it === panel }
    }

    internal fun recordPanels(scope: ContentDrawScope) {
        for (panel in panels) panel.record(scope, origin)
    }
}

/**
 * One registered pane of glass: where it is, how much surrounding backdrop it needs, and what
 * should be painted inside the material.
 */
internal class GlassPanel(val layer: GraphicsLayer) {
    var position: Offset = Offset.Zero
    var size: IntSize = IntSize.Zero
    var margin: Int = 0
    var canvasColor: Color = Color.Black
    var refracted: (DrawScope.(panel: Size) -> Unit)? = null

    fun record(scope: ContentDrawScope, origin: Offset) {
        val w = size.width
        val h = size.height
        if (w <= 0 || h <= 0) return

        val m = margin
        // Must be the DrawScope-scoped `record`: it retargets the *outer* scope's canvas at the
        // layer, which is what makes `scope.drawContent()` land inside the recording. The
        // four-argument overload builds its own draw scope, so drawContent() would quietly keep
        // painting to the screen and the layer would come back empty.
        with(scope) {
            layer.record(IntSize(w + m * 2, h + m * 2)) {
                // The recorded region reaches past the backdrop near screen edges. Without a base
                // fill the blur would drag transparent black inward and bruise the rim; painting
                // the page colour first means it drags the page colour instead.
                drawRect(canvasColor)
                translate(m - (position.x - origin.x), m - (position.y - origin.y)) {
                    scope.drawContent()
                }
                refracted?.let { paint ->
                    translate(m.toFloat(), m.toFloat()) {
                        paint(Size(w.toFloat(), h.toFloat()))
                    }
                }
            }
        }
    }
}

@Composable
fun rememberBackdrop(): BackdropState = remember { BackdropState() }

/**
 * Marks a subtree as the imagery that glass panels refract.
 *
 * Glass panels must be *siblings* drawn after this node, never children of it — a panel nested
 * inside the content it samples would feed on itself.
 */
fun Modifier.backdrop(state: BackdropState): Modifier = this
    .onGloballyPositioned { state.origin = it.positionInRoot() }
    .drawWithContent {
        @Suppress("UNUSED_EXPRESSION")
        state.observePulse()
        state.recordPanels(this)
        drawContent()
        state.version++
    }
