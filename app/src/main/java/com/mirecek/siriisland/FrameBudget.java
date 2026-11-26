package com.mirecek.siriisland;

/**
 * Decides when the 3D glass is too slow (spec §1.2): after a warm-up, two consecutive frame
 * intervals over 20 ms trip it, and it stays tripped until {@link #reset()}.
 */
final class FrameBudget {
    static final long SLOW_NS = 20_000_000L;
    static final int STRIKES = 2;
    /** Frames ignored first: they include compiling the 3D shader. */
    static final int WARMUP_FRAMES = 30;

    private long last;
    private int frames;
    private int strikes;
    private boolean tripped;

    /** Feeds a vsync timestamp (ns); returns whether the budget is tripped. */
    boolean frame(long nanos) {
        if (tripped) return true;
        if (last != 0 && frames >= WARMUP_FRAMES) {
            if (nanos - last > SLOW_NS) {
                if (++strikes >= STRIKES) tripped = true;
            } else {
                strikes = 0;
            }
        }
        frames++;
        last = nanos;
        return tripped;
    }

    boolean tripped() { return tripped; }

    void reset() {
        last = 0;
        frames = 0;
        strikes = 0;
        tripped = false;
    }
}
