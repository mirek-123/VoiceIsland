package com.mirecek.siriisland;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class GlassProfileTest {
    private static final float EPS = 1e-4f;

    @Test
    public void bodyIsIos27RegularFillAtTintZeroAndCapsAt97Percent() {
        assertEquals(0.70f, GlassProfile.bodyAlpha(0f), EPS);
        assertEquals(0.97f, GlassProfile.bodyAlpha(0.9f), EPS);
        assertEquals(0.97f, GlassProfile.bodyAlpha(2f), EPS);
        assertEquals(0.70f, GlassProfile.bodyAlpha(-1f), EPS);
        float prev = 0f;
        for (int i = 0; i <= 90; i++) {
            float b = GlassProfile.bodyAlpha(i / 100f);
            assertTrue(b >= prev);
            prev = b;
        }
    }

    @Test
    public void frostBlursUpToIos27MediumBlur() {
        assertEquals(0f, GlassProfile.blurDp(0f), EPS);
        assertEquals(15f, GlassProfile.blurDp(1f), EPS);
        assertEquals(15f, GlassProfile.blurDp(3f), EPS);
        assertEquals(0f, GlassProfile.blurDp(-1f), EPS);
    }

    @Test
    public void shadowFollowsIos27DarkAndLight() {
        assertEquals(0.45f, GlassProfile.shadowAlpha(0f), EPS);
        assertEquals(0.25f, GlassProfile.shadowAlpha(1f), EPS);
        assertEquals(0.35f, GlassProfile.shadowAlpha(0.5f), EPS);
    }
}
