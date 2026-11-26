package com.mirecek.siriisland;

/**
 * Damped spring for frame-driven animation (no dependencies). {@code omega} is the natural
 * angular frequency in rad/s, {@code zeta} the damping ratio: below 1 overshoots and wobbles.
 */
final class Spring {
    float x;
    float v;
    float target;

    /** Advances by {@code dt} seconds in fixed sub-steps so stiff springs stay stable at 60-120 Hz. */
    void step(float dt, float omega, float zeta) {
        float k = omega * omega, c = 2f * zeta * omega;
        while (dt > 0f) {
            float h = Math.min(dt, 1f / 240f);
            v += (-k * (x - target) - c * v) * h;
            x += v * h;
            dt -= h;
        }
    }

    /** True once both the offset from the target and the speed are below {@code eps} (per second for speed). */
    boolean settled(float eps) {
        return Math.abs(x - target) < eps && Math.abs(v) < eps * 10f;
    }

    void snap(float value) {
        x = target = value;
        v = 0f;
    }
}
