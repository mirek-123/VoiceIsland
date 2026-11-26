package com.mirecek.siriisland;

import android.view.View;
import android.widget.LinearLayout;

/** What the waves listen to, and how strongly they react and glow. */
final class WavesPage implements SettingsNav.Page {
    private final MainActivity a;
    private final View root;
    private final Ui.Choice source;
    /** Shown only while the wave style is Strands. */
    private final LinearLayout strandsBox;

    WavesPage(MainActivity a) {
        this.a = a;
        Ui ui = a.ui();
        LinearLayout col = ui.column();
        LinearLayout src = ui.section(col, "Listen to");
        source = ui.segmented(Prefs.AUDIO_SOURCE,
                new String[]{VoiceActivity.SOURCE_MIC, VoiceActivity.SOURCE_OUTPUT, VoiceActivity.SOURCE_SYNTHETIC},
                new String[]{"Voice + assistant", "Assistant", "Motion"}, Prefs.audioSource(a));
        src.addView(source.view());
        ui.footnote(col, "If the assistant ever stops hearing you, pick \"Assistant\".");

        LinearLayout g = ui.section(col, "Reaction");
        ui.add(g, ui.sliderRow("Sensitivity", Prefs.SENSITIVITY, 15, 150, 54,
                v -> Math.round(v * 100f / 54f) + " %", null));
        ui.add(g, ui.sliderRow("Waviness", Prefs.WAVINESS, 50, 110, 82,
                v -> Math.round(v * 100f / 82f) + " %", a::previewQuietly));
        ui.add(g, ui.sliderRow("Ribbon glow", Prefs.GLOW, 0, 100, 35, v -> v + " %", a::previewQuietly));
        ui.footnote(col, "The preview reacts to your voice while this screen is open.");

        strandsBox = new LinearLayout(a);
        strandsBox.setOrientation(LinearLayout.VERTICAL);
        col.addView(strandsBox);
        LinearLayout st = ui.section(strandsBox, "Strands");
        ui.add(st, ui.sliderRow("Strands reaction", Prefs.STRANDS_REACTION, 25, 300, 100,
                v -> v + " %", a::previewQuietly));
        ui.footnote(strandsBox, "How much the strands swell when you or the assistant speak. Raise it if they barely move.");

        LinearLayout shape = ui.section(strandsBox, "Strands shape");
        ui.add(shape, ui.sliderRow("Strands", Prefs.STRANDS_COUNT, 1, 3, 3, v -> String.valueOf(v), a::previewQuietly));
        ui.add(shape, ui.sliderRow("Arch height", Prefs.STRANDS_AMPLITUDE, 25, 200, 100, v -> v + " %", a::previewQuietly));
        ui.add(shape, ui.sliderRow("Spread", Prefs.STRANDS_SPREAD, 25, 200, 100, v -> v + " %", a::previewQuietly));
        ui.add(shape, ui.sliderRow("Edge thickness", Prefs.STRANDS_THICKNESS, 25, 250, 100, v -> v + " %", a::previewQuietly));
        ui.add(shape, ui.sliderRow("Taper", Prefs.STRANDS_TAPER, 40, 200, 100, v -> v + " %", a::previewQuietly));
        ui.add(shape, ui.sliderRow("Idle energy", Prefs.STRANDS_ENERGY, 0, 100, Math.round(StrandsMotion.REST * 100),
                v -> v + " %", a::previewQuietly));
        ui.footnote(strandsBox, "Taper sets how far the light reaches toward the sides. Idle energy is how lively the strands stay while nobody speaks: 0 rests as a calm white line.");

        LinearLayout lightSec = ui.section(col, "Light");
        ui.add(lightSec, ui.sliderRow("Brightness", Prefs.STRANDS_INTENSITY, 30, 200, 100, v -> v + " %", a::previewQuietly));
        ui.add(lightSec, ui.sliderRow("Saturation", Prefs.STRANDS_SATURATION, 0, 200, 100, v -> v + " %", a::previewQuietly));
        ui.add(lightSec, ui.sliderRow("Hue shift", Prefs.STRANDS_HUE, 0, 100, 0,
                v -> Math.round(v * 3.6f) + "°", a::previewQuietly));
        ui.add(lightSec, ui.sliderRow("Speed", Prefs.STRANDS_SPEED, 0, 300, 100, v -> v + " %", a::previewQuietly));
        ui.footnote(col, "These apply to every wave style. Hue shift turns the theme's colours around the colour wheel.");

        LinearLayout th = ui.section(col, "While the assistant thinks");
        ui.add(th, ui.switchRow("Thinking animation", Prefs.THINKING_ANIM, true));
        ui.footnote(col, "On: the strands dim and two coloured comets circle the rim while a soft light breathes in the middle. Off: the bubble keeps listening.");
        root = ui.scroll(col);
        refresh();
    }

    @Override public String title() { return "Waves & glow"; }

    @Override public View view() { return root; }

    @Override public void refresh() {
        source.select(Prefs.audioSource(a));
        boolean strands = MiniWaveView.modeFor(Prefs.waveStyle(a)) == MiniWaveView.STRANDS;
        strandsBox.setVisibility(strands ? View.VISIBLE : View.GONE);
    }
}
