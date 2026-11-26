package com.mirecek.siriisland;

/**
 * Strands lens (spec §1.1): the orb refracts the captured wallpaper with React Bits Strands' radial
 * remap and dispersion (https://reactbits.dev/animations/strands, Copyright (c) 2026 David Haz,
 * MIT + Commons Clause), then adds Strands' body, light and rim ({@link StrandsLight}). Used as a
 * RenderEffect on the backdrop ({@code uHasBackdrop} 1), as a paint without backdrop, or with
 * {@code uLightOnly} 1 as premultiplied light for the glow around the island.
 *
 * <p>Ported from React Bits Strands (https://reactbits.dev), Copyright (c) 2026 David Haz, MIT + Commons Clause.
 */
final class StrandsGlassShader {
    private StrandsGlassShader() {}

    static final String SOURCE = String.join("\n",
        StrandsLight.UNIFORMS,
        WaveStyles.UNIFORMS,
        "uniform float uN;",          // superellipse exponent (2 = orb)
        "uniform float uRefraction;",
        "uniform float uLightOnly;",
        WaveStyles.FUNCTIONS,
        StrandsLight.FUNCTIONS,
        "",
        "float superNorm(float2 q, float n) {",
        "    return pow(pow(abs(q.x), n) + pow(abs(q.y), n), 1.0 / n);",
        "}",
        "",
        "float2 remap(float2 v, float rho, float f, out float bend) {",
        "    float r = min(rho, 0.9995);",
        "    bend = pow(r, 5.17);",
        "    float k = clamp(uRefraction, 0.0, 1.8);",
        "    float sc = (1.0 - 0.278 * k * (1.0 - bend)) * (1.0 - 0.632 * k * bend) / (1.0 - 0.278 * k);",
        "    return v * sc * f;",
        "}",
        "",
        "float2 toBackdrop(float2 u, float2 hs) {",
        "    return jellyOut(hs + float2(u.x, -u.y) * hs.x) - uOffset;",
        "}",
        "",
        // Light coordinate of an island pixel (for the footprint below).
        "float2 lensAt(float2 ip, float2 hs) {",
        "    float2 d0 = ip - hs;",
        "    float b;",
        "    return remap(float2(d0.x, -d0.y) / hs.x, superNorm(d0 / hs, uN), 1.0, b);",
        "}",
        "",
        "half4 main(float2 fc) {",
        "    float2 ip = jellyIn(fc + uOffset);",
        "    float2 hs = uSize * 0.5;",
        "    float2 d0 = ip - hs;",
        "    float rho = superNorm(d0 / hs, uN);",
        "    float2 v = float2(d0.x, -d0.y) / hs.x;",
        "    float level = clamp(uLevel, 0.0, 1.0);",
        "    float bend;",
        "    float2 u = remap(v, rho, 1.0, bend);",
        // Strands' pixel footprint is 0.5 x the screen derivative of u (dFdx/dFdy, which AGSL lacks:
        // taken from the neighbour pixels). Near the rim the refraction squeezes many light units
        // into one pixel, so the strands widen there instead of aliasing; never sharper than before.
        "    float2 fu = lensAt(jellyIn(fc + uOffset + float2(1.0, 0.0)), hs) - u;",
        "    float2 fv = lensAt(jellyIn(fc + uOffset + float2(0.0, 1.0)), hs) - u;",
        "    float fp = max(1.0 / hs.x, 0.5 * max(length(fu), length(fv)));",
        "    float halo;",
        "    float3 L = sceneLight(u, fp, bend, level, halo);",
        "    float ca = 0.02 * uDispersion * bend;",
        "    float2 ur = u;",
        "    float2 ub = u;",
        "    if (ca > 0.0015) {",
        "        float b2;",
        "        float skip;",
        "        ur = remap(v, rho, 1.0 - ca, b2);",
        "        ub = remap(v, rho, 1.0 + ca, b2);",
        "        L.r = sceneLight(ur, fp, bend, level, skip).r;",
        "        L.b = sceneLight(ub, fp, bend, level, skip).b;",
        "    }",
        "    L += thinkGlow(v, rho);",
        "    float3 col = grade(1.0 - exp(-L));",
        "    if (uLightOnly > 0.5) return half4(half3(col), max(col.r, max(col.g, col.b)));",
        "    float3 bg = float3(0.0);",
        "    if (uHasBackdrop > 0.5) {",
        "        bg.r = content.eval(toBackdrop(ur, hs)).r;",
        "        bg.g = content.eval(toBackdrop(u, hs)).g;",
        "        bg.b = content.eval(toBackdrop(ub, hs)).b;",
        "    }",
        "    return half4(half3(orbShade(bg, col, L, halo, v, rho, bend, level)), 1.0);",
        "}");
}
