package com.mirecek.siriisland;

/**
 * Numbers of the Strands bubble glass (spec docs/superpowers/specs/2026-11-17-strands-bubble-design.md).
 * Pure math with no Android dependencies.
 */
final class GlassProfile {
    private GlassProfile() {}

    /** Body over the wallpaper at Glass tint 0 % (iOS 27 Regular fill) and at the slider's 90 % maximum. */
    static final float BODY_CLEAR = 0.70f;
    static final float BODY_MAX = 0.97f;
    static final float TINT_MAX = 0.9f;
    /** Clear ↔ Frosted at 100 %: iOS 27 medium backdrop blur. */
    static final float FROSTED_BLUR_DP = 15f;

    /** Strands' refraction, dispersion and rim at their React Bits defaults. */
    static final float REFRACTION = 1f;
    static final float DISPERSION = 1f;
    static final float RIM = 1f;

    /** 3D glass: index of refraction (Fluid Glass's 1 + 0.5 x refraction), edge radius and gap as fractions of the height. */
    static final float FLUID_IOR = 1.5f;
    static final float FLUID_EDGE = 0.15f;
    static final float FLUID_GAP = 0.1f;

    /** iOS 27 Regular glass shadow: 0 8 48 (CSS blur 48 = Gaussian sigma 24), alpha 0.45 dark / 0.25 light. */
    static final float SHADOW_OFFSET_DP = 8f;
    static final float SHADOW_SIGMA_DP = 24f;
    static final float SHADOW_ALPHA_DARK = 0.45f;
    static final float SHADOW_ALPHA_LIGHT = 0.25f;

    /** Glow inside the island: blur radius as a fraction of the width, strength at the default Ribbon glow. */
    static final float BLOOM_RADIUS = 0.10f;
    static final float BLOOM_STRENGTH = 0.45f;
    /** Glow below the island: shift down (fraction of height), blur (fraction of width), strength (x inside). */
    static final float BLOOM_BELOW_SHIFT = 0.40f;
    static final float BLOOM_BELOW_RADIUS = 0.12f;
    static final float BLOOM_BELOW_STRENGTH = 1.4f;

    /** Body opacity over the wallpaper for a Glass tint of {@code tint} (0..0.9). */
    static float bodyAlpha(float tint) {
        return BODY_CLEAR + (BODY_MAX - BODY_CLEAR) * clamp01(tint / TINT_MAX);
    }

    static float blurDp(float frost) { return FROSTED_BLUR_DP * clamp01(frost); }

    /** Shadow alpha for {@code light} 0 (dark orb) .. 1 (light orb). */
    static float shadowAlpha(float light) {
        return SHADOW_ALPHA_DARK + (SHADOW_ALPHA_LIGHT - SHADOW_ALPHA_DARK) * clamp01(light);
    }

    private static float clamp01(float x) { return Math.max(0f, Math.min(1f, x)); }
}
