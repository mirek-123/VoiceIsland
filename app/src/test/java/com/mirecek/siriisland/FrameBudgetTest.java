package com.mirecek.siriisland;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class FrameBudgetTest {
    private static final long MS = 1_000_000L;

    private static long warm(FrameBudget b) {
        long t = 0;
        for (int i = 0; i <= FrameBudget.WARMUP_FRAMES; i++) {
            t += 16 * MS;
            b.frame(t);
        }
        return t;
    }

    @Test
    public void slowFramesDuringWarmUpAreIgnored() {
        FrameBudget b = new FrameBudget();
        long t = 0;
        for (int i = 0; i < FrameBudget.WARMUP_FRAMES; i++) {
            t += 300 * MS;
            assertFalse(b.frame(t));
        }
        assertFalse(b.tripped());
    }

    @Test
    public void oneSlowFrameIsNotEnough() {
        FrameBudget b = new FrameBudget();
        long t = warm(b);
        assertFalse(b.frame(t += 25 * MS));
        assertFalse(b.frame(t += 16 * MS));
        assertFalse(b.frame(t += 25 * MS));
        assertFalse(b.tripped());
    }

    @Test
    public void twoConsecutiveSlowFramesTripAndItStaysTrippedUntilReset() {
        FrameBudget b = new FrameBudget();
        long t = warm(b);
        assertFalse(b.frame(t += 25 * MS));
        assertTrue(b.frame(t += 25 * MS));
        assertTrue(b.frame(t += 16 * MS));
        assertTrue(b.tripped());
        b.reset();
        assertFalse(b.tripped());
    }
}
