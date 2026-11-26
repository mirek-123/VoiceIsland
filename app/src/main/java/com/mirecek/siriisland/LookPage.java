package com.mirecek.siriisland;

import android.view.View;
import android.widget.LinearLayout;

import java.util.ArrayList;
import java.util.List;

/** Theme cards with live mini-previews, wave-style grid, shape, size and position. */
final class LookPage implements SettingsNav.Page {
    private static final String[] STYLE_IDS = {Prefs.STYLE_STRANDS, Prefs.STYLE_IOS27, Prefs.STYLE_BUBBLE, Prefs.STYLE_PRISM, Prefs.STYLE_CLASSIC};
    private static final String[] STYLE_NAMES = {"Strands", "iOS 27", "Bubble", "Prism", "Classic"};

    private final MainActivity a;
    private final View root;
    private final Ui.Choice theme;
    private final Ui.Choice style;
    private final Ui.Choice shape;
    private final List<MiniWaveView> themeArt = new ArrayList<>();
    private final List<MiniWaveView> styleArt = new ArrayList<>();

    LookPage(MainActivity a) {
        this.a = a;
        Ui ui = a.ui();
        LinearLayout col = ui.column();
        String[] ids = new String[Themes.ALL.length];
        String[] names = new String[Themes.ALL.length];
        for (int i = 0; i < Themes.ALL.length; i++) {
            ids[i] = Themes.ALL[i].id;
            names[i] = Themes.ALL[i].name;
        }
        int mode = MiniWaveView.modeFor(Prefs.waveStyle(a));
        LinearLayout tg = ui.section(col, "Theme");
        theme = ui.cardPicker(Prefs.THEME, ids, names, Prefs.theme(a).id, id -> {
            MiniWaveView m = new MiniWaveView(a, Themes.byId(id), mode);
            themeArt.add(m);
            return m;
        });
        tg.addView(theme.view());

        LinearLayout sg = ui.section(col, "Wave style");
        style = ui.gridPicker(Prefs.WAVE_STYLE, STYLE_IDS, STYLE_NAMES, Prefs.waveStyle(a), id -> {
            MiniWaveView m = new MiniWaveView(a, Prefs.theme(a), MiniWaveView.modeFor(id));
            styleArt.add(m);
            return m;
        });
        sg.addView(style.view());

        LinearLayout shp = ui.section(col, "Shape");
        shape = ui.segmented(Prefs.SHAPE, new String[]{Prefs.SHAPE_ISLAND, Prefs.SHAPE_ORB},
                new String[]{"Island", "Orb"}, Prefs.orb(a) ? Prefs.SHAPE_ORB : Prefs.SHAPE_ISLAND);
        shp.addView(shape.view());

        LinearLayout size = ui.section(col, "Size & position");
        ui.add(size, ui.sliderRow("Size", Prefs.SIZE_PERCENT, Prefs.SIZE_MIN, Prefs.SIZE_MAX, 100,
                v -> v + " %", a::previewQuietly));
        ui.add(size, ui.sliderRow("Vertical position", Prefs.OFFSET_DP, Prefs.OFFSET_MIN, Prefs.OFFSET_MAX, 0,
                v -> v + " dp", a::previewQuietly));
        root = ui.scroll(col);
    }

    @Override public String title() { return "Look"; }

    @Override public View view() { return root; }

    @Override public void refresh() {
        Themes.Theme t = Prefs.theme(a);
        String st = Prefs.waveStyle(a);
        theme.select(t.id);
        style.select(st);
        shape.select(Prefs.orb(a) ? Prefs.SHAPE_ORB : Prefs.SHAPE_ISLAND);
        int mode = MiniWaveView.modeFor(st);
        for (MiniWaveView m : themeArt) m.setMode(mode);
        for (MiniWaveView m : styleArt) m.setTheme(t);
    }
}
