package com.mirecek.siriisland;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class StrandsMotionTest {
    private static final float DT = 1f / 240f;

    @Test
    public void thinkingEasesTheLevelDownWithinPointFourSeconds() {
        StrandsMotion m = new StrandsMotion();
        for (int i = 0; i < 480; i++) m.step(DT, 1f, false, false);
        assertTrue(m.level() > 0.95f);
        for (int i = 0; i < 96; i++) m.step(DT, 1f, true, false); // 0.4 s
        assertTrue("level " + m.level(), Math.abs(m.level() - StrandsMotion.THINK_LEVEL) < 0.06f);
    }

    @Test
    public void normalSpeechSpansTheStrandsShaderRange() {
        // The shader's strands grow over level 0..0.65; silence must sit low and normal speech reach the top.
        StrandsMotion quiet = new StrandsMotion(), speech = new StrandsMotion();
        for (int i = 0; i < 480; i++) {
            quiet.step(DT, 0f, false, false);
            speech.step(DT, 0.4f, false, false);
        }
        assertTrue("silent " + quiet.level(), quiet.level() < 0.3f);
        assertTrue("speech " + speech.level(), speech.level() >= 0.65f);
    }

    @Test
    public void thinkingAndReduceMotionHalveTheClock() {
        StrandsMotion a = new StrandsMotion(), b = new StrandsMotion(), c = new StrandsMotion();
        for (int i = 0; i < 240; i++) {
            a.step(DT, 0f, false, false);
            b.step(DT, 0f, true, false);
            c.step(DT, 0f, false, true);
        }
        // Same pace history for a and c; b also eases to a lower pace, so it is at most half.
        assertEquals(a.clock * 0.5f, c.clock, 1e-4f);
        assertTrue(b.clock <= a.clock * 0.5f + 1e-4f);
    }

    @Test
    public void motionMatchesStrandsFormula() {
        StrandsMotion m = new StrandsMotion();
        float[] out = new float[3];
        m.motion(out); // clock 0, pace 0 -> sway 0.75
        assertEquals((float) (0.1 * Math.sin(1.3) * 0.75), out[0], 1e-6f);
        assertEquals((float) (1 + 0.035 * Math.sin(0.4) * 0.75), out[1], 1e-6f);
        assertEquals((float) (0.06 * Math.sin(2) * 0.75), out[2], 1e-6f);
    }

    @Test
    public void curvesStartAndEndWhereTheyShould() {
        assertEquals(0f, StrandsMotion.introEase(0f), 1e-6f);
        assertEquals(1f, StrandsMotion.introEase(1f), 1e-6f);
        assertEquals(1f, StrandsMotion.introEase(2f), 1e-6f);
        assertEquals(0f, StrandsMotion.thinkPulse(0f), 1e-6f);
        assertEquals(1f, StrandsMotion.thinkPulse(0.5f), 1e-6f);
    }

    @Test
    public void clickPulseSwellsOnceThenSettlesWithinASecond() {
        float[] xv = {0f, StrandsMotion.CLICK_KICK};
        float peak = 0f;
        for (int i = 0; i < 240; i++) {
            StrandsMotion.stepClick(xv, DT);
            peak = Math.max(peak, xv[0]);
        }
        assertTrue("peak " + peak, peak > 0.15f && peak < 0.4f);
        assertTrue("rest " + xv[0], Math.abs(xv[0]) < 0.01f && Math.abs(xv[1]) < 0.1f);
    }

    @Test
    public void simulatedSpeechHasPausesAndPeaks() {
        float min = 9f, max = 0f;
        for (int i = 0; i < 2000; i++) {
            float v = StrandsMotion.speech(i * 0.01f);
            min = Math.min(min, v);
            max = Math.max(max, v);
        }
        assertEquals(0f, min, 1e-6f);
        assertTrue("max " + max, max > 0.8f && max <= 1.25f);
    }

    @Test
    public void jellySettlesWithinPointEightSecondsAndAtMostTwoSwings() {
        Spring s = new Spring();
        s.x = 0.3f; // 30 % of the bubble height
        int swings = 0;
        float prevV = 0f;
        for (int i = 0; i < 192; i++) { // 0.8 s
            s.step(DT, StrandsMotion.JELLY_OMEGA, StrandsMotion.JELLY_ZETA);
            if (prevV != 0f && Math.signum(prevV) != Math.signum(s.v) && Math.abs(s.x) > 0.02f) swings++;
            prevV = s.v;
        }
        assertTrue("x " + s.x, Math.abs(s.x) < 0.005f);
        assertTrue("swings " + swings, swings <= 2);
    }

    @Test
    public void reduceMotionJellyNeverOvershoots() {
        Spring s = new Spring();
        s.x = 0.3f;
        for (int i = 0; i < 480; i++) {
            s.step(DT, StrandsMotion.JELLY_OMEGA, StrandsMotion.JELLY_ZETA_REDUCED);
            assertTrue("x " + s.x, s.x >= -1e-6f);
        }
    }

    @Test
    public void energyZeroRestsLowAndATapBurstLiftsTheLevel() {
        StrandsMotion calm = new StrandsMotion(), tapped = new StrandsMotion();
        for (int i = 0; i < 480; i++) {
            calm.step(DT, 0f, false, false, 0f, 1f, 0f);
            tapped.step(DT, 0f, false, false, 0f, 1f, 0.9f);
        }
        assertTrue("calm " + calm.level(), calm.level() < 0.02f);
        assertTrue("tapped " + tapped.level(), tapped.level() > 0.85f);
    }
}
