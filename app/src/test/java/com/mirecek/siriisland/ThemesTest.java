package com.mirecek.siriisland;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class ThemesTest {
    private static final float EPS = 1e-6f;

    @Test
    public void siriUsesStrandsOwnColoursExactly() {
        int[] expected = {0xd95b56, 0xba5046, 0xffa24c, 0xffe0b3, 0x93f1ff, 0x3569fa, 0x193d9b};
        float[] c = Themes.strandsColors(Themes.byId("rainbow"));
        assertEquals(21, c.length);
        for (int i = 0; i < 7; i++) {
            assertEquals(((expected[i] >> 16) & 255) / 255f, c[i * 3], EPS);
            assertEquals(((expected[i] >> 8) & 255) / 255f, c[i * 3 + 1], EPS);
            assertEquals((expected[i] & 255) / 255f, c[i * 3 + 2], EPS);
        }
    }

    @Test
    public void otherThemesSpreadTheirFourColoursOverTheSevenRoles() {
        Themes.Theme cool = Themes.byId("cool");
        float[] c = Themes.strandsColors(cool);
        int c0 = cool.colors[0], c1 = cool.colors[1], c3 = cool.colors[3];
        assertEquals(((c0 >> 16) & 255) / 255f, c[0], EPS);
        // role 3 (warm white) = mix(white, c1, 0.25)
        assertEquals((255 + (((c1 >> 16) & 255) - 255) * 0.25f) / 255f, c[9], 1f / 255f);
        // role 6 (deep) = c3 x 0.6
        assertEquals(((c3 & 255) * 0.6f) / 255f, c[20], 1f / 255f);
    }

    @Test
    public void everyRoleOfEveryThemeIsInRange() {
        for (Themes.Theme t : Themes.ALL) {
            for (float v : Themes.strandsColors(t)) assertTrue(t.id + " " + v, v >= 0f && v <= 1f);
        }
    }

    @Test
    public void reactBitsPalettesKeepTheirSevenColoursExactly() {
        float[] c = Themes.strandsColors(Themes.byId("mint"));
        assertEquals(0x00 / 255f, c[0], EPS);
        assertEquals(0xe0 / 255f, c[1], EPS);
        assertEquals(0x6a / 255f, c[18], EPS);
        assertEquals(0xff / 255f, c[20], EPS);
        assertEquals(0xc4 / 255f, Themes.strandsColors(Themes.byId("ember"))[18], EPS);
    }
}
