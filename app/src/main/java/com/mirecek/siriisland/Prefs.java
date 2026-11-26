package com.mirecek.siriisland;

import android.content.Context;
import android.content.SharedPreferences;

/** User settings, shared by the settings screen and the accessibility service (same process). */
final class Prefs {
    static final String FILE = "siri_island";
    static final String ENABLED = "enabled";
    static final String SIZE_PERCENT = "size_percent";
    static final String OFFSET_DP = "offset_dp";
    static final String LIQUID_GLASS = "liquid_glass";
    static final String TINT_PERCENT = "tint_percent_v3";
    static final String GLASS_FROST = "glass_frost";
    static final String THEME = "theme";
    static final String WAVE_STYLE = "wave_style";
    static final String SHAPE = "shape";
    static final String AUDIO_SOURCE = "audio_source";
    static final String SENSITIVITY = "sensitivity";
    static final String WAVINESS = "waviness";
    static final String STRANDS_REACTION = "strands_reaction";
    // React Bits Strands props (sliders store percent; 100 = the React Bits default).
    static final String STRANDS_COUNT = "strands_count";
    static final String STRANDS_AMPLITUDE = "strands_amplitude";
    static final String STRANDS_SPREAD = "strands_spread";
    static final String STRANDS_THICKNESS = "strands_thickness";
    static final String STRANDS_TAPER = "strands_taper";
    static final String STRANDS_INTENSITY = "strands_intensity";
    static final String STRANDS_SATURATION = "strands_saturation";
    static final String STRANDS_HUE = "strands_hue";
    static final String STRANDS_SPEED = "strands_speed";
    static final String STRANDS_ENERGY = "strands_energy";
    static final String GLASS_REFRACTION = "glass_refraction";
    static final String GLASS_DISPERSION = "glass_dispersion";
    static final String GLASS_RIM = "glass_rim";
    static final String STRETCH_REACT = "stretch_react";
    static final String LAUNCH_SOUND = "launch_sound";
    static final String SOUND_STYLE = "sound_style";
    static final String SOUND_VOLUME = "sound_volume";
    /** Packages whose windows open the island (string set). */
    static final String TRIGGER_APPS = "trigger_apps";

    static final String STYLE_PRISM = "prism";
    static final String STYLE_CLASSIC = "classic";
    static final String STYLE_BUBBLE = "bubble";
    static final String STYLE_IOS27 = "ios27";
    static final String CUSTOM_SOUND_URI = "custom_sound_uri";
    static final String GLOW = "glow_percent";
    static final String THINKING_ANIM = "thinking_animation";
    static final String GLASS_KIND = "glass_kind";
    static final String GLASS_STRANDS = "strands";
    static final String GLASS_3D = "3d";
    static final String STYLE_STRANDS = "strands";
    static final String SHAPE_ISLAND = "island";
    static final String SHAPE_ORB = "orb";

    static final int SIZE_MIN = 70;
    static final int SIZE_MAX = 140;
    static final int OFFSET_MIN = -20;
    static final int OFFSET_MAX = 40;

    private Prefs() {}

    static SharedPreferences get(Context c) {
        return c.getSharedPreferences(FILE, Context.MODE_PRIVATE);
    }

    static boolean enabled(Context c) { return get(c).getBoolean(ENABLED, true); }

    static java.util.Set<String> triggerApps(Context c) {
        return java.util.Set.copyOf(get(c).getStringSet(TRIGGER_APPS, Assistants.DEFAULT));
    }

    static void setTrigger(Context c, String pkg, boolean on) {
        java.util.Set<String> s = new java.util.HashSet<>(triggerApps(c));
        if (on) s.add(pkg);
        else s.remove(pkg);
        get(c).edit().putStringSet(TRIGGER_APPS, s).apply();
    }

    /** Island size multiplier, 0.7–1.4. */
    static float scale(Context c) { return get(c).getInt(SIZE_PERCENT, 100) / 100f; }

    /** Extra vertical shift of the expanded island in dp; the collapsed state always sits on the camera. */
    static int offsetDp(Context c) { return get(c).getInt(OFFSET_DP, 0); }

    
    /** Glass body (blur + refraction of what is behind) instead of a solid black island. */
    static boolean liquidGlass(Context c) { return get(c).getBoolean(LIQUID_GLASS, true); }

    static Themes.Theme theme(Context c) { return Themes.byId(get(c).getString(THEME, "rainbow")); }

    static String waveStyle(Context c) { return get(c).getString(WAVE_STYLE, STYLE_STRANDS); }

    /** Audio file picked by the user for the launch sound, or null. */
    static String customSound(Context c) { return get(c).getString(CUSTOM_SOUND_URI, null); }

    /** Orb: a round iPad/Mac-style drop instead of the iPhone island capsule. */
    static boolean orb(Context c) { return SHAPE_ORB.equals(get(c).getString(SHAPE, SHAPE_ISLAND)); }

    static String audioSource(Context c) { return get(c).getString(AUDIO_SOURCE, VoiceActivity.SOURCE_MIC); }

    /** Sound gain; the default 54 maps to 1.0 (same default point as 超级岛 PRO's 0.54). */
    static float sensitivity(Context c) { return get(c).getInt(SENSITIVITY, 54) / 54f; }

    /** Wave frequency multiplier; default 82 maps to 1.0. */
    static float waviness(Context c) { return get(c).getInt(WAVINESS, 82) / 82f; }

    /** How strongly the voice drives the Strands wave, 0.25..3 (100 % on the slider = 1). */
    static float strandsReaction(Context c) { return get(c).getInt(STRANDS_REACTION, 100) / 100f; }

    /** Integer slider value / 100 (Strands props: 100 % = React Bits default). */
    static float pct(Context c, String key, int def) { return get(c).getInt(key, def) / 100f; }

    /** Strands shown inside the bulge, 1..3. */
    static int strandsCount(Context c) { return Math.max(1, Math.min(3, get(c).getInt(STRANDS_COUNT, 3))); }

    /** Idle energy while listening in silence, 0..1 (Strands' energy; 0 rests as a calm white line). */
    static float strandsEnergy(Context c) { return pct(c, STRANDS_ENERGY, Math.round(StrandsMotion.REST * 100)); }

    /** Glass and light react to the jelly stretch (lean, refraction, rim). */
    static boolean stretchReact(Context c) { return get(c).getBoolean(STRETCH_REACT, true); }

    static boolean launchSound(Context c) { return get(c).getBoolean(LAUNCH_SOUND, true); }

    static String soundStyle(Context c) { return get(c).getString(SOUND_STYLE, LaunchSound.STYLES[0]); }

    /** Launch sound volume, 1..10. */
    static int soundVolume(Context c) { return get(c).getInt(SOUND_VOLUME, 7); }

    /** Strength of the light the ribbon throws onto the island, 0..2 (50% on the slider = original). */
    static float glow(Context c) { return get(c).getInt(GLOW, 35) / 50f; }

    /** While Xiao Ai thinks: calm, pulsing strands (on) or keep listening (off). */
    static boolean thinkingAnimation(Context c) { return get(c).getBoolean(THINKING_ANIM, true); }

    /** Extra darkness of the clear glass, 0..1. Default 0: Apple's clear iOS 27 glass. */
    static float tint(Context c) { return get(c).getInt(TINT_PERCENT, 0) / 100f; }

    /**
     * Clear (0, Apple's lens) to frosted (1, blurred). Forced to 1 while Android's High contrast
     * text is on, so text behind the island stays readable.
     */
    static float frost(Context c) {
        if (highContrastText(c)) return 1f;
        return get(c).getInt(GLASS_FROST, 0) / 100f;
    }

    /** 3D glass (React Bits Fluid Glass) instead of the Strands lens. */
    static boolean glass3d(Context c) { return GLASS_3D.equals(get(c).getString(GLASS_KIND, GLASS_STRANDS)); }

    /** The system is in dark mode: dark orb; otherwise the light frosted orb. */
    static boolean night(Context c) {
        int mode = c.getResources().getConfiguration().uiMode & android.content.res.Configuration.UI_MODE_NIGHT_MASK;
        return mode == android.content.res.Configuration.UI_MODE_NIGHT_YES;
    }

    /** Android's "Remove animations" (animator duration scale 0). */
    static boolean reduceMotion(Context c) {
        return android.provider.Settings.Global.getFloat(c.getContentResolver(),
                android.provider.Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f;
    }

    static boolean highContrastText(Context c) {
        try {
            return android.provider.Settings.Secure.getInt(
                    c.getContentResolver(), "high_text_contrast_enabled", 0) == 1;
        } catch (SecurityException e) {
            // Not readable on this build: the user's slider applies.
            return false;
        }
    }
}
