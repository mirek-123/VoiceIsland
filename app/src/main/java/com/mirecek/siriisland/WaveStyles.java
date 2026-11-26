package com.mirecek.siriisland;

/**
 * The four classic wave styles (Prism, Classic, Bubble, iOS 27 braid) as AGSL functions, drawn as the
 * light inside the Strands orb when the wave style is not Strands. {@link #UNIFORMS} must follow the
 * host's declarations of {@code uSize} and {@code uLevel}; {@link #FUNCTIONS} provides
 * {@code float3 waveStyle(float2 p)} with p in island half-widths, y down, centred on uCenter.
 */
final class WaveStyles {
    private WaveStyles() {}

    static final String UNIFORMS = String.join("\n",
        "uniform float uTime;",     // motion clock (StrandsMotion.clock)
        "uniform float4 uBands;",   // 0..1 bass, low-mid, high-mid, treble
        "uniform float uStyle;",    // 0 prism, 1 classic, 2 bubble, 3 iOS 27
        "uniform float uWavy;",     // wave frequency multiplier
        "uniform float uSpectral;", // 1: prism fringes through the full spectrum
        "uniform float uGlow;",     // Ribbon glow, 0..2
        "uniform float uCenter;",   // ribbon centre line, fraction of the height
        "uniform float3 uP0;",      // theme colours
        "uniform float3 uP1;",
        "uniform float3 uP2;",
        "uniform float3 uP3;");

    static final String FUNCTIONS = String.join("\n",
        "",
        "float3 hue(float h) {",
        "    return clamp(abs(fract(h + float3(0.0, 0.6667, 0.3333)) * 6.0 - 3.0) - 1.0, 0.0, 1.0);",
        "}",
        "",
        // Theme colour at a continuous position k (wraps every 4).
        "float3 pal(float k) {",
        "    k = mod(k, 4.0);",
        "    float f = smoothstep(0.0, 1.0, fract(k));",
        "    if (k < 1.0) return mix(uP0, uP1, f);",
        "    if (k < 2.0) return mix(uP1, uP2, f);",
        "    if (k < 3.0) return mix(uP2, uP3, f);",
        "    return mix(uP3, uP0, f);",
        "}",
        "",
        "float taper(float x) {",
        "    float t = clamp(1.0 - x * x, 0.0, 1.0);",
        "    return t * t;",
        "}",
        "",
        // ---- Prism style ----
        // Coverage of one ribbon at p. e: its band energy. The ribbon's centre weaves around the core
        // line and its half-thickness swells with energy, so each ribbon reads as a moving lens.
        "float ribbonMask(float2 p, float e, float freq, float speed, float phase, float aa) {",
        "    float tp = taper(p.x / 0.94);",
        "    float k = freq * uWavy;",
        "    float off = (0.02 + 0.16 * e) * tp * sin(p.x * k + uTime * speed + phase);",
        "    float swell = 0.55 + 0.45 * sin(p.x * k * 0.63 - uTime * speed * 0.8 + phase * 1.7);",
        "    float halfW = (0.006 + 0.15 * e * swell) * tp + 0.002;",
        "    float d = abs(p.y - off);",
        "    return 1.0 - smoothstep(halfW * 0.35, halfW + aa, d);",
        "}",
        "",
        "float3 prism(float2 p) {",
        "    float aa = 3.0 / uSize.x;",
        "    float e0 = clamp(0.12 + uBands.x * 1.1, 0.0, 1.3);",
        "    float e1 = clamp(0.12 + uBands.y * 1.1, 0.0, 1.3);",
        "    float e2 = clamp(0.12 + (uBands.z + uBands.w) * 0.6, 0.0, 1.3);",
        "    float3 col = float3(0.0);",
        "    float cover = 0.0;",
        "    for (int i = 0; i < 3; i++) {",
        "        float fi = float(i);",
        "        float e = i == 0 ? e0 : (i == 1 ? e1 : e2);",
        "        float freq = 3.2 + fi * 1.15;",
        "        float speed = 1.1 + fi * 0.37;",
        "        float phase = fi * 2.1;",
        // Dispersion: red and blue sample the ribbon slightly above and below green.
        "        float split = (0.006 + 0.03 * e) * taper(p.x);",
        "        float mR = ribbonMask(p + float2(0.0, split), e, freq, speed, phase, aa);",
        "        float mG = ribbonMask(p, e, freq, speed, phase, aa);",
        "        float mB = ribbonMask(p - float2(0.0, split), e, freq, speed, phase, aa);",
        "        float3 c = pal(fi * 1.3 + p.x * 1.4 + uTime * 0.12);",
        "        float3 fringe = hue(fi * 0.33 + p.x * 0.25 + uTime * 0.05);",
        "        c = mix(c, fringe, uSpectral * 0.55);",
        "        float3 m = float3(mR, mG, mB);",
        // Edges keep the colour, the middle of a thick ribbon lifts towards white glass light.
        "        float edge = clamp(abs(mR - mB) * 2.0, 0.0, 1.0);",
        "        col += m * c * (0.8 + 0.5 * edge) + mG * (1.0 - edge) * 0.22;",
        "        cover += mG;",
        "    }",
        "    col += float3(1.0) * clamp(cover - 1.0, 0.0, 1.5) * 0.55;",
        "    float core = exp(-abs(p.y) / (0.0035 + 0.006 * uLevel)) * taper(p.x / 0.9);",
        "    col += float3(1.0) * core * (0.45 + 0.75 * uLevel);",
        "    return col;",
        "}",
        "",
        // ---- Classic style ----
        "float3 lobe(float2 p, float3 col, float band, float speed, float phase, float width, float up, float down) {",
        "    float cx = 0.58 * sin(uTime * speed + phase) + 0.12 * sin(uTime * speed * 2.3 + phase * 1.7);",
        "    float w = width * (0.85 + 0.15 * sin(uTime * 0.9 + phase)) * (1.0 + 0.25 * band);",
        "    float env = exp(-pow((p.x - cx) / w, 2.0)) * (1.0 - smoothstep(0.62, 0.95, abs(p.x)));",
        "    float breathe = 0.65 + 0.35 * sin(uTime * (speed * 1.9 + 0.6) * uWavy + phase * 2.1);",
        "    float energy = 0.28 + 0.3 * uLevel + 0.9 * band;",
        "    float a = (p.y < 0.0 ? up : down) * 2.6 * env * breathe * energy + 0.0005;",
        "    float d = abs(p.y);",
        "    float fill = 1.0 - smoothstep(a * 0.6, a, d);",
        "    float glow = exp(-d / (a * 0.35 + 0.006)) * env * 0.12;",
        "    return col * (fill + glow);",
        "}",
        "",
        "float3 classic(float2 p) {",
        "    float3 c = float3(0.0);",
        "    c += lobe(p, mix(uP3, float3(0.05, 0.80, 1.00), uSpectral), uBands.x, 0.53, 0.0, 0.34, 0.22, 0.10);",
        "    c += lobe(p, mix(uP2, float3(0.10, 0.30, 1.00), uSpectral), uBands.y, 0.41, 1.9, 0.30, 0.17, 0.13);",
        "    c += lobe(p, mix(uP0, float3(1.00, 0.18, 0.72), uSpectral), uBands.z, 0.47, 3.7, 0.28, 0.19, 0.09);",
        "    c += lobe(p, mix(uP1, float3(1.00, 0.22, 0.12), uSpectral), uBands.w, 0.61, 5.1, 0.24, 0.12, 0.11);",
        "    c += lobe(p, mix(pal(0.5), float3(1.00, 0.80, 0.10), uSpectral), uBands.y, 0.37, 2.6, 0.31, 0.10, 0.16);",
        "    c += lobe(p, mix(pal(2.5), float3(0.15, 1.00, 0.40), uSpectral), uBands.z, 0.57, 4.4, 0.26, 0.15, 0.08);",
        "    float core = exp(-abs(p.y) / (0.006 + 0.006 * uLevel)) * (1.0 - smoothstep(0.35, 0.92, abs(p.x)));",
        "    c += float3(1.0) * core * (0.5 + 0.8 * uLevel);",
        "    float m = max(c.r, max(c.g, c.b));",
        "    c += float3(0.06) * m * m;",
        "    return c;",
        "}",
        "",
        // ---- Bubble style (default): one soft spectral glow with stacked colours and a white core ----
        // Light split like a prism: red and orange on one side of the band, cyan and blue on the other.
        "float3 prismRamp(float v) {",
        "    v = clamp(v, -1.0, 1.0);",
        "    float3 red = float3(1.0, 0.13, 0.08);",
        "    float3 orange = float3(1.0, 0.55, 0.12);",
        "    float3 white = float3(1.0, 0.95, 0.9);",
        "    float3 cyan = float3(0.25, 0.8, 1.0);",
        "    float3 blue = float3(0.12, 0.3, 1.0);",
        "    if (v < -0.45) return mix(red, orange, smoothstep(-1.0, -0.45, v));",
        "    if (v < 0.0) return mix(orange, white, smoothstep(-0.2, 0.0, v));",
        "    if (v < 0.45) return mix(white, cyan, smoothstep(0.0, 0.2, v));",
        "    return mix(cyan, blue, smoothstep(0.45, 1.0, v));",
        "}",
        "",
        "float3 bubble(float2 p) {",
        "    float lvl = 0.3 + 0.7 * uLevel;",
        "    float3 col = float3(0.0);",
        "    float cover = 0.0;",
        "    for (int i = 0; i < 3; i++) {",
        "        float fi = float(i);",
        "        float e = i == 0 ? uBands.x : (i == 1 ? uBands.y : 0.5 * (uBands.z + uBands.w));",
        "        float cx = 0.14 * sin(uTime * (0.55 + 0.21 * fi) + fi * 2.1);",
        "        float w = 0.42 + 0.06 * sin(uTime * 0.7 + fi * 1.3) + 0.1 * e;",
        "        float xn = (p.x - cx) / w;",
        "        float env = exp(-xn * xn * 1.6) * (1.0 - smoothstep(0.78, 0.98, abs(p.x)));",
        "        float flip = sin(uTime * 0.35 + fi * 2.1);",
        "        float twist = (0.02 + 0.06 * e) * sin(p.x * 2.2 * uWavy + uTime * (1.2 + 0.35 * fi) + fi * 1.9);",
        "        float h = (0.045 + 0.12 * e) * env * lvl + 0.003;",
        "        float yn = (p.y - twist - (fi - 1.0) * 0.035 * flip) / h;",
        "        float m = exp(-yn * yn * 1.2) * env;",
        // Which side is red slowly swaps per ribbon, so the colours roll over each other.
        
        "        float3 spec = prismRamp(yn * 1.3 * sign(flip + 0.0001));",
        "        float3 themed = pal(fi * 1.33 + yn * 0.7 * flip + uTime * 0.12);",
        "        float3 c = mix(themed, spec, uSpectral);",
        "        col += c * m * (0.75 + 0.5 * abs(flip));",
        "        cover += m;",
        "    }",
        "    col += float3(1.0) * clamp(cover - 1.2, 0.0, 2.0) * 0.12;",
        "    float xc = exp(-p.x * p.x * 3.0);",
        "    col += float3(1.0) * exp(-p.y * p.y / (0.0006 + 0.0015 * lvl)) * xc * 0.3 * lvl;",
        "    return col;",
        "}",
        "",
        // ---- iOS 27 style (default) ----
        // Two or three crisp translucent sheets twisting over a bright core line. Inside each sheet the
        // light is near white; its two edges split into warm and cool spectra, and the order of those
        // fringes swaps wherever the sheet turns over. Crossings burn white, edges get a thin bright lip.
        // Colour stops along the ribbon, left to right, sampled from Apple's iOS 27 footage
        // (docs/ios27-siri-research.md section 6.4): amber, violet-blue, cyan, white, yellow,
        // red-orange, pink-red. Themes replace them with their own palette.
        "float3 siriStops(float s) {",
        "    float3 amber = float3(0.70, 0.49, 0.20);",
        "    float3 violet = float3(0.54, 0.63, 0.84);",
        "    float3 cyan = float3(0.62, 0.89, 0.93);",
        "    float3 white = float3(0.97, 0.98, 0.98);",
        "    float3 yellow = float3(0.92, 0.87, 0.60);",
        "    float3 redOrange = float3(0.76, 0.40, 0.36);",
        "    float3 pinkRed = float3(0.73, 0.43, 0.51);",
        "    if (s < 0.10) return amber;",
        "    if (s < 0.30) return mix(amber, violet, smoothstep(0.10, 0.20, s));",
        "    if (s < 0.45) return mix(violet, cyan, smoothstep(0.30, 0.40, s));",
        "    if (s < 0.60) return mix(cyan, white, smoothstep(0.45, 0.52, s));",
        "    if (s < 0.72) return mix(white, yellow, smoothstep(0.60, 0.66, s));",
        "    if (s < 0.88) return mix(yellow, redOrange, smoothstep(0.72, 0.82, s));",
        "    return mix(redOrange, pinkRed, smoothstep(0.88, 0.95, s));",
        "}",
        "",
        // iOS 27 ribbon, rebuilt from the research spec:
        // - one S-shaped band spanning 0.97 of the orb width, tails sagging down ~0.12 of its height;
        // - full thickness at half maximum 0.08 Wo when quiet, 0.145 speaking, 0.20 loud
        //   (p-space thickness = 2 x the Wo fraction); 1.5-1.9 Hz modulation plus slow drift;
        // - clipped white core with a soft glow (100/255 at 0.05 Wo, 40/255 at 0.165 Wo);
        // - colour stops along x, upper edges leaning blue/cyan and lower edges amber.
        // Prism split of white light, u = signed distance from the ribbon core in half-widths:
        // u < 0 (above the core) runs red -> orange -> yellow, u > 0 (below) cyan -> blue -> violet.
        "float3 prism(float u) {",
        "    float s = clamp(abs(u) / 2.4, 0.0, 1.0);",
        "    if (u < 0.0) {",
        "        float3 c = mix(float3(1.0, 0.95, 0.35), float3(1.0, 0.58, 0.12), smoothstep(0.35, 0.6, s));",
        "        return mix(c, float3(0.98, 0.22, 0.20), smoothstep(0.6, 0.9, s));",
        "    }",
        "    float3 c = mix(float3(0.30, 0.92, 1.0), float3(0.22, 0.45, 1.0), smoothstep(0.35, 0.6, s));",
        "    return mix(c, float3(0.58, 0.30, 1.0), smoothstep(0.6, 0.9, s));",
        "}",
        "",
        "float3 ios27(float2 p) {",
        // iOS 27 wave v3 (from Apple's Dynamic Island footage): 4 lens-shaped colour lobes that taper
        // to points, braid across each other and add up to white where they overlap. Each lobe drifts
        // on its own phase and cycles hue; loudness swells them and spreads them wider.
        "    float lvl = clamp(uLevel, 0.0, 1.0);",
        "    float e = clamp(0.5 * lvl + 0.5 * max(uBands.x, uBands.y), 0.0, 1.0);",
        "    float k = uWavy;",
        "    float3 col = float3(0.0);",
        "    for (int i = 0; i < 4; i++) {",
        "        float fi = float(i);",
        "        float ph = uTime * (1.15 + 0.37 * fi) + fi * 1.71;",
        // Each lobe spans 0.62-0.8 of the width (iOS 27: ~0.7), centred with a slow sideways drift.
        "        float ext = 0.66 + 0.1 * e + 0.06 * sin(ph * 0.53 + fi);",
        "        float x0 = 0.1 * sin(ph * 0.41 + fi * 2.3);",
        "        float xn = (p.x - x0) / ext;",
        "        float env = pow(clamp(1.0 - xn * xn, 0.0, 1.0), 0.8);",
        // Centre line: a gentle S that crosses the other lobes; amplitude grows with the voice.
        "        float amp = (0.035 + 0.05 * e) * (0.6 + 0.4 * sin(ph * 0.8));",
        "        float c = amp * sin(xn * (1.9 + 0.5 * fi) * k + ph) * env + 0.03 * xn * xn;",
        // Thickness: rounded lens profile; total ~14 % of the orb height when quiet, ~23 % when loud (iOS 27: ~25 %).
        "        float hw = (0.105 + 0.07 * e) * (0.65 + 0.35 * sin(ph * 0.67 + fi * 0.9)) * env + 0.0005;",
        "        float r = abs(p.y - c) / hw;",
        "        float fill = smoothstep(1.3, 0.25, r);",
        "        float core = exp(-r * r * 3.0);",
        // Hue: the Siri spectrum spaced around the wheel and slowly turning; themes use their palette.
        "        float h = fi * 0.27 + uTime * 0.06 + 0.12 * xn;",
        "        float3 lobeCol = mix(pal(fi * 0.85 + xn * 0.6 + uTime * 0.08), hue(h), uSpectral);",
        "        col += lobeCol * (fill * 1.15 + core * 0.45) * env;",
        // Faint light around each lobe, scaled by Ribbon glow.
        "        col += lobeCol * 0.08 * exp(-max(r - 1.0, 0.0) * 1.0) * env * uGlow;",
        "    }",
        "    return col;",
        "}",
        "",
        "float3 waveStyle(float2 p) {",
        "    return uStyle < 0.5 ? prism(p) : (uStyle < 1.5 ? classic(p) : (uStyle < 2.5 ? bubble(p) : ios27(p)));",
        "}");
}
