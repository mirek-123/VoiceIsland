package com.mirecek.siriisland;

/**
 * Ribbon colour themes: four colours each, blended along and across the ribbons.
 *
 * The palettes are the six from 超级岛 PRO's Siri page (constants in its palette table, which maps
 * "rainbow", "cool", "warm", "aurora", "magenta" and "ice" to four ARGB colours). As in that app,
 * "Castle Night" is sampled from the user's night-castle wallpaper. "Siri" (its "rainbow") is drawn spectrally: the four colours act as anchors and the ribbon edges
 * split into a full prism.
 */
final class Themes {
    private Themes() {}

    static final class Theme {
        final String id;
        final String name;
        final int[] colors;
        /** Prism-split edges through the whole spectrum instead of the four colours. */
        final boolean spectral;
        /** Strands' seven colour roles given directly (React Bits palettes), or null to derive them. */
        final int[] roles;

        Theme(String id, String name, boolean spectral, int... colors) {
            this(id, name, spectral, null, colors);
        }

        Theme(String id, String name, boolean spectral, int[] roles, int... colors) {
            this.id = id;
            this.name = name;
            this.spectral = spectral;
            this.roles = roles;
            this.colors = colors;
        }
    }

    static final Theme[] ALL = {
            new Theme("rainbow", "Rainbow", true, 0xFF0000, 0xFFFF00, 0x00FF00, 0x00FFFF),
            new Theme("cool", "Cool", false, 0x16D5F2, 0x3478F6, 0x6F4CFF, 0xC13CFF),
            new Theme("warm", "Warm", false, 0xFFC62B, 0xFF8A2B, 0xFF4E3A, 0xFF2F65),
            new Theme("aurora", "Aurora", false, 0x28F0B2, 0x36D9FF, 0x665CFF, 0xDC55FF),
            new Theme("magenta", "Magenta", false, 0xFF48CB, 0xB45CFF, 0x36D9FF, 0x45E3A2),
            new Theme("ice", "Ice", false, 0xD9FF00, 0x7DFF00, 0x00FF5A, 0x00FFA8),
            // From the night-castle wallpaper: moonlit cloud, misty teal sky, night-blue glow and the
            // warm candle light of the castle windows, brightened so they glow on the ribbon.
            new Theme("castle_night", "Castle Night", false, 0x4FA3E3, 0x2A7FB0, 0x5CC8F0, 0xFFB347),
            // Wizarding-world colour sets (palettes only, no artwork), bright enough to glow.
            new Theme("phoenix", "Phoenix", false, 0xFF3B1F, 0xFF8A1F, 0xFFC53D, 0xD4142E),
            new Theme("scarlet_gold", "Scarlet & Gold", false, 0xE0242B, 0xFFC234, 0xB0101C, 0xF5D06A),
            new Theme("emerald_silver", "Emerald & Silver", false, 0x1FBF6A, 0xC9D3DC, 0x4FE39A, 0x9AA7B3),
            new Theme("bronze_blue", "Bronze & Blue", false, 0x2F6FE0, 0xD08A3E, 0x6FA4FF, 0xE6B07A),
            new Theme("gold_black", "Gold & Black", false, 0xFFD21F, 0xFFE57A, 0xC9A20F, 0xFFF1B0),
            new Theme("silver_spirit", "Silver Spirit", false, 0x8FD0FF, 0xD2ECFF, 0x4FA8FF, 0xB0E2FF),
            new Theme("green_flame", "Green Flame", false, 0x2DFF86, 0x15D46A, 0x9BFFC8, 0x0FB85A),
            // React Bits Strands palettes (seven roles: red top edge, orange, amber, warm core, cyan,
            // blue, deep); the four wave-style colours are roles 0, 2, 4 and 6.
            new Theme("mint", "Mint", false,
                    new int[]{0x00e08a, 0x3dffb0, 0x00c2a8, 0xc8fff0, 0x7fe3ff, 0x2f6bff, 0x6a4dff},
                    0x00e08a, 0x00c2a8, 0x7fe3ff, 0x6a4dff),
            new Theme("ember", "Ember", false,
                    new int[]{0xff2a1f, 0xff5a2e, 0xff7a1a, 0xffe2a8, 0xffb37a, 0xff3d2e, 0xc42a6a},
                    0xff2a1f, 0xff7a1a, 0xffb37a, 0xc42a6a),
    };

    static Theme byId(String id) {
        for (Theme t : ALL) if (t.id.equals(id)) return t;
        return ALL[0];
    }

    /** React Bits Strands' default palette: the "Siri" theme. */
    private static final int[] STRANDS_DEFAULT = {0xd95b56, 0xba5046, 0xffa24c, 0xffe0b3, 0x93f1ff, 0x3569fa, 0x193d9b};

    /**
     * Strands' seven colour roles (red, orange, amber, warm white, cyan, blue, deep) as RGB 0..1,
     * 21 floats. Spectral themes use Strands' own palette; others spread their four colours: warm
     * half from c0/c1, cool half from c2/c3.
     */
    static float[] strandsColors(Theme t) {
        int[] roles;
        if (t.roles != null) {
            roles = t.roles;
        } else if (t.spectral) {
            roles = STRANDS_DEFAULT;
        } else {
            int c0 = t.colors[0], c1 = t.colors[1], c2 = t.colors[2], c3 = t.colors[3];
            roles = new int[]{c0, mix(c0, c1, 0.5f), c1, mix(0xffffff, c1, 0.25f), c2, mix(c2, c3, 0.5f), scale(c3, 0.6f)};
        }
        float[] out = new float[21];
        for (int i = 0; i < 7; i++) {
            out[i * 3] = ((roles[i] >> 16) & 255) / 255f;
            out[i * 3 + 1] = ((roles[i] >> 8) & 255) / 255f;
            out[i * 3 + 2] = (roles[i] & 255) / 255f;
        }
        return out;
    }

    private static int mix(int a, int b, float t) {
        int r = Math.round(((a >> 16) & 255) + (((b >> 16) & 255) - ((a >> 16) & 255)) * t);
        int g = Math.round(((a >> 8) & 255) + (((b >> 8) & 255) - ((a >> 8) & 255)) * t);
        int bl = Math.round((a & 255) + ((b & 255) - (a & 255)) * t);
        return (r << 16) | (g << 8) | bl;
    }

    private static int scale(int c, float k) {
        return mix(0, c, k);
    }
}
