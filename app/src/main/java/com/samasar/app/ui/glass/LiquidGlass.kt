package com.samasar.app.ui.glass

import android.graphics.RuntimeShader
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.samasar.app.ui.theme.NewsTheme
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

/** Corner radius sentinel that always resolves to a full capsule. */
val PillCorner: Dp = 1000.dp

/**
 * Physical description of a pane of glass.
 *
 * Think of these as material properties rather than styling: [refraction] and [thickness] set how
 * the rim bends light, [dispersion] how far the colour channels separate, [glare] how hot the
 * specular band burns.
 */
@Immutable
data class GlassStyle(
    val blur: Dp = 28.dp,
    val refraction: Dp = 12.dp,
    val thickness: Dp = 20.dp,
    val dispersion: Float = 0.05f,
    val glare: Float = 0.50f,
    val saturation: Float = 1.30f,
    val brightness: Float = 1.03f,
    val innerShade: Float = 0.12f,
    val tintAlpha: Float = 0.10f,
    /**
     * Overrides the theme's glass tint. Panels sitting over bright artwork need to pull *down*
     * rather than up, or white text on them stops being readable.
     */
    val tintOverride: Color? = null,
    val rimAlpha: Float = 0.55f,
    val rimWidth: Dp = 1.dp,
    /** Direction the key light comes from, in radians. Default is upper-left. */
    val lightAngle: Float = -2.20f,
    val elevation: Dp = 16.dp,
) {
    companion object {
        /** Cards, sheets, large panels. */
        val Regular = GlassStyle()

        /** Navigation and top bars — thicker, more presence, floats over the feed. */
        val Chrome = GlassStyle(
            blur = 30.dp,
            refraction = 16.dp,
            thickness = 24.dp,
            dispersion = 0.06f,
            glare = 0.60f,
            saturation = 1.42f,
            tintAlpha = 0.12f,
            rimAlpha = 0.62f,
            elevation = 24.dp,
        )

        /** Buttons and chips — small, so the bevel has to be tight to stay readable. */
        val Control = GlassStyle(
            blur = 16.dp,
            refraction = 9.dp,
            thickness = 11.dp,
            dispersion = 0.055f,
            glare = 0.78f,
            saturation = 1.35f,
            tintAlpha = 0.14f,
            rimAlpha = 0.70f,
            elevation = 10.dp,
        )

        /**
         * A near-clear lens: barely blurred, heavily refracting. This is the toggle thumb and the
         * nav selection capsule — you should be able to read what is underneath, distorted.
         */
        val Lens = GlassStyle(
            blur = 5.dp,
            refraction = 20.dp,
            thickness = 15.dp,
            dispersion = 0.10f,
            glare = 1.00f,
            saturation = 1.55f,
            brightness = 1.06f,
            innerShade = 0.06f,
            tintAlpha = 0.06f,
            rimAlpha = 0.85f,
            elevation = 12.dp,
        )
    }
}

/** What the device can actually render. */
private enum class GlassTier { Shader, BlurOnly, Flat }

private val tier: GlassTier = when {
    Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> GlassTier.Shader
    Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> GlassTier.BlurOnly
    else -> GlassTier.Flat
}

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
private class GlassProgram {
    private val shader = RuntimeShader(LIQUID_GLASS_AGSL)

    fun effect(
        width: Float,
        height: Float,
        margin: Float,
        radius: Float,
        thickness: Float,
        refraction: Float,
        style: GlassStyle,
        tint: Color,
        blurPx: Float,
    ): androidx.compose.ui.graphics.RenderEffect {
        shader.setFloatUniform("uSize", width, height)
        shader.setFloatUniform("uMargin", margin)
        shader.setFloatUniform("uRadius", radius)
        shader.setFloatUniform("uThickness", thickness)
        shader.setFloatUniform("uRefraction", refraction)
        shader.setFloatUniform("uDispersion", style.dispersion)
        shader.setFloatUniform("uGlare", style.glare)
        shader.setFloatUniform("uLight", cos(style.lightAngle), sin(style.lightAngle))
        shader.setFloatUniform("uTint", tint.red, tint.green, tint.blue, style.tintAlpha)
        shader.setFloatUniform("uSaturation", style.saturation)
        shader.setFloatUniform("uBrightness", style.brightness)
        shader.setFloatUniform("uInnerShade", style.innerShade)

        val refract = android.graphics.RenderEffect.createRuntimeShaderEffect(shader, "content")
        return if (blurPx >= 0.5f) {
            val blur = android.graphics.RenderEffect.createBlurEffect(
                blurPx,
                blurPx,
                android.graphics.Shader.TileMode.CLAMP,
            )
            // Chain applies the inner effect first: blur the backdrop, then refract it. This is
            // safe here because the layer being blurred holds only primitive draw commands.
            android.graphics.RenderEffect.createChainEffect(refract, blur).asComposeRenderEffect()
        } else {
            refract.asComposeRenderEffect()
        }
    }
}

/**
 * Renders [this] element as a pane of real, refracting liquid glass over [backdrop].
 *
 * The element's own children are drawn sharp on top of the glass. Anything passed as [refracted]
 * is painted *into* the material instead, so it gets blurred and bent along with the backdrop —
 * that's how the nav bar's selection glow ends up looking embedded rather than stuck on. The
 * lambda receives the panel's own size, since it runs inside the larger recorded layer.
 *
 * Each panel costs the backdrop one extra walk of its draw tree, so prefer
 * [Modifier.frostedSurface] for small controls and keep true glass for floating chrome.
 */
fun Modifier.liquidGlass(
    backdrop: BackdropState,
    cornerRadius: Dp = 28.dp,
    style: GlassStyle = GlassStyle.Regular,
    refracted: (DrawScope.(panel: Size) -> Unit)? = null,
): Modifier = composed {
    val layer = rememberGraphicsLayer()
    val panel = remember(layer) { GlassPanel(layer) }
    val program = remember { if (tier == GlassTier.Shader) GlassProgram() else null }
    val colors = NewsTheme.colors
    val density = LocalDensity.current

    val blurPx = with(density) { style.blur.toPx() }
    val refractionPx = with(density) { style.refraction.toPx() }

    // Keep the registration current; the backdrop reads it during its own draw pass.
    panel.canvasColor = colors.canvas
    panel.refracted = refracted
    panel.margin = (refractionPx * 1.8f + blurPx * 1.2f).roundToInt().coerceIn(0, 220)

    DisposableEffect(backdrop, panel) {
        backdrop.register(panel)
        onDispose { backdrop.unregister(panel) }
    }

    val shape = RoundedCornerShape(cornerRadius)
    val tint = style.tintOverride ?: colors.glassTint
    val rim = colors.glassRim
    val rimShadow = colors.glassRimShadow

    this
        .shadow(
            elevation = style.elevation,
            shape = shape,
            clip = false,
            ambientColor = colors.shadow.copy(alpha = 0.34f),
            spotColor = colors.shadow.copy(alpha = 0.44f),
        )
        .onGloballyPositioned {
            panel.position = it.positionInRoot()
            panel.size = it.size
        }
        .drawWithContent {
            val w = size.width
            val h = size.height
            if (w < 1f || h < 1f) {
                drawContent()
                return@drawWithContent
            }

            val radiusPx = min(cornerRadius.toPx(), min(w, h) * 0.5f)
            val thicknessPx = min(style.thickness.toPx(), min(w, h) * 0.45f)
            val refractPx = min(refractionPx, min(w, h) * 0.30f)
            val margin = panel.margin

            val path = Path().apply {
                addRoundRect(
                    RoundRect(
                        rect = Rect(Offset.Zero, Size(w, h)),
                        cornerRadius = CornerRadius(radiusPx),
                    ),
                )
            }

            if (tier != GlassTier.Flat) {
                // Reading the backdrop version here is what repaints the glass when the feed
                // scrolls underneath it.
                @Suppress("UNUSED_EXPRESSION")
                backdrop.version

                if (tier == GlassTier.Shader) {
                    layer.renderEffect = program!!.effect(
                        width = w,
                        height = h,
                        margin = margin.toFloat(),
                        radius = radiusPx,
                        thickness = thicknessPx,
                        refraction = refractPx,
                        style = style,
                        tint = tint,
                        blurPx = blurPx,
                    )
                    // The shader cuts its own antialiased silhouette, so no clip is needed.
                    translate(-margin.toFloat(), -margin.toFloat()) { drawLayer(layer) }
                } else {
                    layer.renderEffect = androidx.compose.ui.graphics.BlurEffect(blurPx, blurPx)
                    clipPath(path) {
                        translate(-margin.toFloat(), -margin.toFloat()) { drawLayer(layer) }
                        drawRoundRect(
                            color = tint.copy(alpha = style.tintAlpha + 0.06f),
                            cornerRadius = CornerRadius(radiusPx),
                        )
                    }
                }
            } else {
                drawRoundRect(
                    brush = Brush.verticalGradient(
                        listOf(
                            tint.copy(alpha = style.tintAlpha + 0.20f),
                            tint.copy(alpha = style.tintAlpha + 0.10f),
                        ),
                    ),
                    cornerRadius = CornerRadius(radiusPx),
                )
            }

            drawGlassRim(
                path = path,
                width = style.rimWidth.toPx(),
                rim = rim,
                rimShadow = rimShadow,
                alpha = style.rimAlpha,
                size = Size(w, h),
            )

            drawContent()
        }
}

/**
 * The cheap glass look: tint, bevel highlight and rim, with no backdrop sampling.
 *
 * Small controls sit *on* the chrome rather than floating independently over the feed, so a full
 * refraction pass buys almost nothing visually and costs a whole extra draw-tree walk. This keeps
 * them in the same material family for free.
 */
fun Modifier.frostedSurface(
    cornerRadius: Dp,
    tintAlpha: Float = 0.14f,
    rimAlpha: Float = 0.55f,
    fill: Brush? = null,
): Modifier = composed {
    val colors = NewsTheme.colors
    val tint = colors.glassTint
    val rim = colors.glassRim
    val rimShadow = colors.glassRimShadow

    drawBehind {
        val radiusPx = min(cornerRadius.toPx(), min(size.width, size.height) * 0.5f)
        val corner = CornerRadius(radiusPx)

        drawRoundRect(
            brush = Brush.verticalGradient(
                listOf(
                    tint.copy(alpha = tintAlpha * 1.5f),
                    tint.copy(alpha = tintAlpha * 0.7f),
                ),
            ),
            cornerRadius = corner,
        )
        if (fill != null) {
            drawRoundRect(brush = fill, cornerRadius = corner)
        }

        val path = Path().apply {
            addRoundRect(RoundRect(Rect(Offset.Zero, size), corner))
        }
        drawGlassRim(
            path = path,
            width = 1.dp.toPx(),
            rim = rim,
            rimShadow = rimShadow,
            alpha = rimAlpha,
            size = size,
        )
    }
}

/**
 * A crisp one-pixel edge on top of the shader's soft specular band.
 *
 * The shader lights the rim volumetrically; this adds the hard contact line that makes the panel
 * feel cut rather than painted. Bright at the light-facing corner, dark at the opposite one.
 */
private fun DrawScope.drawGlassRim(
    path: Path,
    width: Float,
    rim: Color,
    rimShadow: Color,
    alpha: Float,
    size: Size,
) {
    val brush = Brush.linearGradient(
        0.00f to rim.copy(alpha = alpha),
        0.28f to rim.copy(alpha = alpha * 0.18f),
        0.55f to rimShadow.copy(alpha = alpha * 0.22f),
        0.80f to rim.copy(alpha = alpha * 0.40f),
        1.00f to rim.copy(alpha = alpha * 0.75f),
        start = Offset.Zero,
        end = Offset(size.width, size.height),
    )
    drawPath(path = path, brush = brush, style = Stroke(width = width))
}

