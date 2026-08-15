package com.samasar.app.ui.glass

/**
 * The liquid-glass fragment shader (AGSL, Android 13+).
 *
 * It receives the already-blurred backdrop as `content` and re-samples it as if it were being
 * viewed through a thick slab of glass with a rounded bevel around the rim:
 *
 *  1. A signed-distance field describes the panel's rounded rectangle. Distance is negative
 *     inside, zero on the edge, positive outside.
 *  2. The gradient of that field gives the surface normal — the direction the glass "tilts".
 *  3. The bevel profile is flat across the middle and ramps up hard near the rim, so refraction
 *     only happens at the edges. This is the single most important detail: real glass looks
 *     undistorted in the centre and bends violently in the last few millimetres.
 *  4. Samples are pushed *outward* along the normal, which squeezes the surrounding world into
 *     the rim band — the lensing you see where a shape passes under the edge of the panel.
 *  5. Red and blue are pushed by slightly different amounts, producing the chromatic fringe that
 *     separates convincing glass from a plain frosted rectangle.
 *  6. A specular band rides the rim where the normal faces the light, with a weaker
 *     counter-highlight on the opposite edge.
 *
 * Coordinates arrive in *layer* space. The layer is recorded larger than the panel by `uMargin`
 * on every side so that step 4 has real backdrop to reach for instead of clamped edge pixels.
 */
internal const val LIQUID_GLASS_AGSL = """
uniform shader content;

uniform float2 uSize;        // panel size in px (excluding margin)
uniform float  uMargin;      // px of extra backdrop recorded on each side
uniform float  uRadius;      // corner radius in px
uniform float  uThickness;   // width of the refracting bevel band, px
uniform float  uRefraction;  // peak sample displacement at the rim, px
uniform float  uDispersion;  // chromatic split, fraction of displacement
uniform float  uGlare;       // specular strength
uniform float2 uLight;       // unit vector toward the light
uniform float4 uTint;        // rgb tint + strength in .a
uniform float  uSaturation;
uniform float  uBrightness;
uniform float  uInnerShade;

float sdRoundRect(float2 p, float2 halfSize, float r) {
    float2 q = abs(p) - halfSize + r;
    return min(max(q.x, q.y), 0.0) + length(max(q, float2(0.0, 0.0))) - r;
}

float2 sdNormal(float2 p, float2 halfSize, float r) {
    float e = 1.0;
    float dx = sdRoundRect(p + float2(e, 0.0), halfSize, r)
             - sdRoundRect(p - float2(e, 0.0), halfSize, r);
    float dy = sdRoundRect(p + float2(0.0, e), halfSize, r)
             - sdRoundRect(p - float2(0.0, e), halfSize, r);
    float2 g = float2(dx, dy);
    float len = length(g);
    if (len < 0.0001) {
        return float2(0.0, 0.0);
    }
    return g / len;
}

half4 main(float2 coord) {
    float2 halfSize = uSize * 0.5;
    float2 p = coord - float2(uMargin, uMargin) - halfSize;

    float r = min(uRadius, min(halfSize.x, halfSize.y));
    float d = sdRoundRect(p, halfSize, r);
    float2 n = sdNormal(p, halfSize, r);

    // 0 across the flat centre, 1 at the outer rim.
    float t = clamp(1.0 + d / uThickness, 0.0, 1.0);
    // Cubic profile keeps the middle honest and loads all the bending into the edge.
    float bend = t * t * t;

    float2 push = n * bend * uRefraction;

    // Chromatic dispersion: the three channels take slightly different paths through the bevel.
    half4 sampleR = content.eval(coord + push * (1.0 + uDispersion));
    half4 sampleG = content.eval(coord + push);
    half4 sampleB = content.eval(coord + push * (1.0 - uDispersion));
    float3 rgb = float3(float(sampleR.r), float(sampleG.g), float(sampleB.b));

    // Glass body: lift saturation and brightness a touch, then wash with the tint.
    float luma = dot(rgb, float3(0.2126, 0.7152, 0.0722));
    rgb = mix(float3(luma, luma, luma), rgb, uSaturation);
    rgb = rgb * uBrightness;
    rgb = mix(rgb, uTint.rgb, uTint.a);

    // Thickness shading — the body falls off slightly as the surface curves away from the viewer.
    rgb = rgb * (1.0 - uInnerShade * smoothstep(0.15, 0.95, t));

    // Specular rim.
    float facing = dot(n, uLight);
    float band = smoothstep(0.40, 0.95, t) * (1.0 - smoothstep(0.93, 1.0, t));
    float key = pow(clamp(facing, 0.0, 1.0), 3.0);
    float fill = pow(clamp(-facing, 0.0, 1.0), 5.0) * 0.55;
    rgb = rgb + float3((key + fill) * band * uGlare);

    // Antialiased outer boundary; everything beyond the panel is cut away.
    float alpha = 1.0 - smoothstep(-1.0, 0.5, d);

    return half4(half3(clamp(rgb, 0.0, 1.0) * alpha), half(alpha));
}
"""
