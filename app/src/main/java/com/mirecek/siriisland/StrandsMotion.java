package com.mirecek.siriisland;

/**
 * Motion of the Strands bubble: the React Bits Strands clock and activity (its frame loop, ported),
 * plus every timing constant of the bubble's states (spec §2). Pure Java.
 */
final class StrandsMotion {
    /** Jelly release spring (spec §2): ω 17 rad/s (response 0.37 s), ζ 0.55; Reduce Motion ζ 1. */
    static final float JELLY_OMEGA = 17f;
    static final float JELLY_ZETA = 0.55f;
    static final float JELLY_ZETA_REDUCED = 1f;

    static final long MORPH_OPEN_MS = 350;
    static final long MORPH_CLOSE_MS = 160;
    /** Strands' intro starts at this morph progress and runs this long (ease-out cubic); fade-out on close. */
    static final float INTRO_START = 0.6f;
    static final long INTRO_MS = 800;
    static final long INTRO_OUT_MS = 200;
    /** Reduce Motion: opening and closing become a fade of this length. */
    static final long REDUCED_FADE_MS = 150;
    /** Dark ↔ light crossfade. */
    static final long LIGHT_FADE_MS = 300;

    /**
     * Activity while listening: REST when silent, rising with the voice to 1.2. The Strands shader's
     * strands grow over activity 0..0.65 and barely change above it, so the rest level sits low and
     * normal speech (voice ≈ 0.3-0.5 after Sensitivity) is boosted by VOICE_GAIN to reach the top.
     */
    static final float REST = 0.2f;
    static final float VOICE_GAIN = 1.6f;
    static final float THINK_LEVEL = 0.15f;
    /** Thinking: ease time constant (≈ 0.4 s to settle), clock speed, pulse rate. */
    static final float THINK_TAU = 0.13f;
    static final float THINK_SPEED = 0.5f;
    static final float THINK_PULSE_HZ = 1f;
    /** Pulses swell the orb by this fraction (Strands' click pulse is 3 %). */
    static final float PULSE_SWELL = 0.03f;
    /** Strands' click pulse: a touch kicks the spring's velocity by CLICK_KICK; stiffness 170, damping 14. */
    static final float CLICK_KICK = 5f;
    static final float CLICK_K = 170f;
    static final float CLICK_C = 14f;

    float clock;
    private float activity;
    private float pace;

    /** Advances by {@code dt} seconds. {@code voice} 0..1 is the (sensitivity-scaled) voice level. */
    void step(float dt, float voice, boolean thinking, boolean reduced) {
        step(dt, voice, thinking, reduced, REST, 1f, 0f);
    }

    /**
     * As above with Strands' props: {@code rest} is its energy (activity in silence), {@code speed}
     * multiplies the clock, {@code boost} is added to the target like Strands' hover (0.18) and
     * click burst (up to 1.2); the target is capped at 1.2 as in Strands.
     */
    void step(float dt, float voice, boolean thinking, boolean reduced, float rest, float speed, float boost) {
        float listen = rest + (1.2f - rest) * Math.max(0f, Math.min(1f, voice * VOICE_GAIN));
        float target = thinking ? THINK_LEVEL : Math.min(1.2f, listen + boost);
        float tau = thinking ? THINK_TAU : (target > activity ? 0.12f : 0.4f);
        activity += (target - activity) * (1f - (float) Math.exp(-dt / tau));
        pace += (Math.min(1f, activity) - pace) * (1f - (float) Math.exp(-dt / 0.8f));
        float s = (thinking ? THINK_SPEED : 1f) * (reduced ? 0.5f : 1f) * speed;
        clock += dt * s * (0.6f + 1.6f * pace);
    }

    /** Strands' uMotion: strand phase, bulge amount, bulge x. */
    void motion(float[] out) {
        double c = clock, sway = 0.75 + 0.45 * pace;
        out[0] = (float) ((0.26 * Math.sin(0.53 * c) + 0.1 * Math.sin(1.19 * c + 1.3)) * sway);
        out[1] = (float) (1 + (0.06 * Math.sin(0.71 * c) + 0.035 * Math.sin(1.37 * c + 0.4)) * sway);
        out[2] = (float) (0.06 * Math.sin(0.41 * c + 2) * sway);
    }

    /** Strands' level uniform, 0..1. */
    float level() { return Math.max(0f, Math.min(1f, activity)); }

    /** Strands' intro curve: ease-out cubic over t = 0..1. */
    static float introEase(float t) {
        float u = 1f - Math.max(0f, Math.min(1f, t));
        return 1f - u * u * u;
    }

    /** Thinking pulse 0..1 at {@code t} seconds into thinking: one gentle swell per second. */
    static float thinkPulse(float t) {
        return (float) (0.5 - 0.5 * Math.cos(2 * Math.PI * THINK_PULSE_HZ * t));
    }

    /** Advances Strands' click-pulse spring {x, v} by {@code dt} (its frame loop, ported). */
    static void stepClick(float[] xv, float dt) {
        xv[1] += (-CLICK_K * xv[0] - CLICK_C * xv[1]) * dt;
        xv[0] += xv[1] * dt;
    }

    private static float smooth(float a, float b, float x) {
        float k = Math.max(0f, Math.min(1f, (x - a) / (b - a)));
        return k * k * (3f - 2f * k);
    }

    /** React Bits Strands' simulated voice for its {@code listening} prop: phrases, syllables, stress. */
    static float speech(float t) {
        float phrase = smooth(-0.8f, -0.3f, (float) (0.6 * Math.sin(t * 2.7) + 0.5 * Math.sin(t * 1.53 + 1.2)));
        float syllable = (float) Math.max(0, Math.sin(t * 17 + 1.6 * Math.sin(t * 5.3)));
        float stress = (float) (0.6 + 0.4 * Math.sin(t * 3.7 + 0.7));
        return phrase * (0.35f + 0.65f * syllable) * (0.25f + stress);
    }
}
