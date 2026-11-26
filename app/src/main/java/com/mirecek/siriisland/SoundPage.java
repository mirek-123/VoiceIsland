package com.mirecek.siriisland;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.net.Uri;
import android.provider.OpenableColumns;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

/** The launch sound: on/off, which sound, a custom file, volume and a test button. */
final class SoundPage implements SettingsNav.Page {
    private final MainActivity a;
    private final View root;
    private final Ui.Choice sound;
    private final TextView customName;
    private String nameUri;
    private String cachedName;

    SoundPage(MainActivity a) {
        this.a = a;
        Ui ui = a.ui();
        LinearLayout col = ui.column();
        LinearLayout on = ui.section(col, null);
        ui.add(on, ui.switchRow("Play when an assistant opens", Prefs.LAUNCH_SOUND, true));

        LinearLayout sg = ui.section(col, "Sound");
        sound = ui.cardPicker(Prefs.SOUND_STYLE, LaunchSound.STYLES, LaunchSound.NAMES, Prefs.soundStyle(a), id -> {
            FrameLayout art = new FrameLayout(a);
            art.setBackground(ui.rounded(0xFF000000, 10));
            ImageView icon = new ImageView(a);
            icon.setImageResource(R.drawable.ic_sound);
            icon.setImageTintList(ColorStateList.valueOf(Color.WHITE));
            icon.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
            art.addView(icon, new FrameLayout.LayoutParams(ui.px(26), ui.px(26), Gravity.CENTER));
            return art;
        });
        sg.addView(sound.view());
        customName = ui.text("", 13, Ui.SECONDARY, false);
        ui.add(sg, ui.infoRow("Custom sound", customName, ui.pill("Choose", a::pickSound)));

        LinearLayout vol = ui.section(col, null);
        ui.add(vol, ui.sliderRow("Volume", Prefs.SOUND_VOLUME, 1, 10, 7, String::valueOf, null));
        col.addView(ui.button("Play", Ui.ACCENT,
                () -> LaunchSound.play(a, Prefs.soundStyle(a), Prefs.soundVolume(a))), ui.fullWidth(16));
        root = ui.scroll(col);
        refresh();
    }

    @Override public String title() { return "Launch sound"; }

    @Override public View view() { return root; }

    @Override public void refresh() {
        sound.select(Prefs.soundStyle(a));
        String uri = Prefs.customSound(a);
        if (uri == null) {
            nameUri = null;
            customName.setText("None chosen");
            return;
        }
        if (!uri.equals(nameUri)) {
            String name = Uri.parse(uri).getLastPathSegment();
            try (android.database.Cursor cur = a.getContentResolver().query(Uri.parse(uri),
                    new String[]{OpenableColumns.DISPLAY_NAME}, null, null, null)) {
                if (cur != null && cur.moveToFirst()) name = cur.getString(0);
            } catch (RuntimeException e) {
                name = "File no longer available";
            }
            nameUri = uri;
            cachedName = name;
        }
        customName.setText(cachedName);
    }

    /** Drops the cached display name so the next refresh asks the ContentResolver again (the file may have changed). */
    void forgetName() { nameUri = null; }
}
