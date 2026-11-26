package com.mirecek.siriisland;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.Toast;

import rikka.shizuku.Shizuku;

/**
 * Settings screen: a title bar, the shared live preview and a stack of iOS 27-style pages
 * ({@link SettingsNav}). Pages are built from {@link Ui} and write {@link Prefs} directly.
 */
public final class MainActivity extends Activity {
    private static final int REQ_AUDIO = 1;
    private static final int REQ_SOUND = 2;

    private final Handler main = new Handler(Looper.getMainLooper());
    private Ui ui;
    private PreviewHeader header;
    private FrameLayout titleBar;
    private SettingsNav nav;
    private MainPage mainPage;
    private LookPage lookPage;
    private WavesPage wavesPage;
    private GlassPage glassPage;
    private SoundPage soundPage;
    private SetupPage setupPage;
    private AdvancedPage advancedPage;
    private AssistantsPage assistantsPage;
    private boolean askedAudio;

    /** Keeps the preview and the visible page in sync with the settings. Held strongly: prefs keep weak refs. */
    private final SharedPreferences.OnSharedPreferenceChangeListener prefListener = (prefs, key) -> {
        header.applyPrefs();
        if (Prefs.AUDIO_SOURCE.equals(key) || Prefs.SENSITIVITY.equals(key)) {
            header.voice.stop();
            header.voice.start();
        }
        SettingsNav.Page top = nav.top();
        if (top != null) top.refresh();
    };

    private final Shizuku.OnRequestPermissionResultListener permissionListener =
            (code, result) -> setupPage().onPermissionResult(code, result);
    private final Shizuku.OnBinderReceivedListener binderListener = () -> main.post(this::refreshVisible);
    private final Shizuku.OnBinderDeadListener binderDeadListener = () -> main.post(this::refreshVisible);
    private final Runnable ticker = new Runnable() {
        @Override public void run() {
            refreshVisible();
            main.postDelayed(this, 1000);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(Color.BLACK);
        getWindow().setNavigationBarColor(Color.BLACK);
        ui = new Ui(this);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Ui.BG);
        root.setFitsSystemWindows(true);
        titleBar = new FrameLayout(this);
        root.addView(titleBar, new LinearLayout.LayoutParams(Ui.MATCH, Ui.WRAP));
        header = new PreviewHeader(this, ui);
        LinearLayout.LayoutParams hlp = new LinearLayout.LayoutParams(Ui.MATCH, header.heightFor(0f));
        hlp.setMargins(ui.px(16), ui.px(4), ui.px(16), ui.px(4));
        root.addView(header, hlp);
        FrameLayout container = new FrameLayout(this);
        root.addView(container, new LinearLayout.LayoutParams(Ui.MATCH, 0, 1f));
        setContentView(root);

        nav = new SettingsNav(this, container, this::onDepthChanged);
        mainPage = new MainPage(this);
        nav.setRoot(mainPage);

        Shizuku.addRequestPermissionResultListener(permissionListener);
        Shizuku.addBinderReceivedListenerSticky(binderListener);
        Shizuku.addBinderDeadListener(binderDeadListener);
        KeepAliveJob.schedule(this);
        Prefs.get(this).registerOnSharedPreferenceChangeListener(prefListener);
    }

    @Override
    protected void onDestroy() {
        Shizuku.removeRequestPermissionResultListener(permissionListener);
        Shizuku.removeBinderReceivedListener(binderListener);
        Shizuku.removeBinderDeadListener(binderDeadListener);
        Prefs.get(this).unregisterOnSharedPreferenceChangeListener(prefListener);
        super.onDestroy();
    }

    @Override
    protected void onResume() {
        super.onResume();
        AccessibilityToggle.ensureRunning(this);
        main.post(ticker);
        if (!askedAudio && checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            askedAudio = true;
            requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, REQ_AUDIO);
        }
        header.applyPrefs();
        if (soundPage != null) soundPage.forgetName();
        header.voice.start();
    }

    @Override
    protected void onPause() {
        main.removeCallbacks(ticker);
        header.voice.stop();
        super.onPause();
    }

    private void onDepthChanged(SettingsNav.Page top, int depth) {
        titleBar.removeAllViews();
        if (depth == 0) {
            titleBar.addView(ui.largeTitle(getString(R.string.app_name)));
            header.animateCollapse(mainPage.collapse());
        } else {
            titleBar.addView(ui.navBar(getString(R.string.app_name), top.title(), () -> nav.pop()));
            header.animateCollapse(1f);
        }
    }

    private void refreshVisible() {
        mainPage.refresh();
        SettingsNav.Page top = nav.top();
        if (top != null && top != mainPage) top.refresh();
    }

    // ---- used by the pages ----

    Ui ui() { return ui; }

    PreviewHeader header() { return header; }

    boolean atRoot() { return nav.depth() == 0; }

    void open(SettingsNav.Page page) { nav.push(page); }

    LookPage lookPage() { if (lookPage == null) lookPage = new LookPage(this); return lookPage; }

    WavesPage wavesPage() { if (wavesPage == null) wavesPage = new WavesPage(this); return wavesPage; }

    GlassPage glassPage() { if (glassPage == null) glassPage = new GlassPage(this); return glassPage; }

    SoundPage soundPage() { if (soundPage == null) soundPage = new SoundPage(this); return soundPage; }

    SetupPage setupPage() { if (setupPage == null) setupPage = new SetupPage(this); return setupPage; }

    AdvancedPage advancedPage() { if (advancedPage == null) advancedPage = new AdvancedPage(this); return advancedPage; }

    AssistantsPage assistantsPage() { if (assistantsPage == null) assistantsPage = new AssistantsPage(this); return assistantsPage; }

    void previewOnScreen() {
        SiriAccessibilityService s = SiriAccessibilityService.running();
        if (s == null) {
            Toast.makeText(this, "Enable the accessibility service first", Toast.LENGTH_SHORT).show();
            return;
        }
        s.preview();
    }

    /** After adjusting a slider, show the real overlay so the change is visible in place. */
    void previewQuietly() {
        SiriAccessibilityService s = SiriAccessibilityService.running();
        if (s != null) s.preview();
    }

    /** Lets the user pick any audio file they own for the launch sound. */
    void pickSound() {
        Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        i.addCategory(Intent.CATEGORY_OPENABLE);
        i.setType("audio/*");
        startActivityForResult(i, REQ_SOUND);
    }

    /** Asks first, then clears every saved setting (old keys too) and reloads the screen. */
    void confirmReset() {
        new AlertDialog.Builder(this)
                .setTitle("Reset to defaults?")
                .setMessage("All Voice Island settings go back to their defaults.")
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Reset", (d, w) -> {
                    String sound = Prefs.customSound(this);
                    if (sound != null) {
                        try {
                            getContentResolver().releasePersistableUriPermission(Uri.parse(sound),
                                    Intent.FLAG_GRANT_READ_URI_PERMISSION);
                        } catch (SecurityException ignored) {
                            // Permission was already gone.
                        }
                    }
                    Prefs.get(this).edit().clear().commit();
                    Toast.makeText(this, "Settings reset", Toast.LENGTH_SHORT).show();
                    recreate();
                })
                .show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != REQ_SOUND || resultCode != RESULT_OK || data == null || data.getData() == null) return;
        Uri uri = data.getData();
        getContentResolver().takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
        Prefs.get(this).edit()
                .putString(Prefs.CUSTOM_SOUND_URI, uri.toString())
                .putString(Prefs.SOUND_STYLE, LaunchSound.CUSTOM)
                .apply();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == REQ_AUDIO && (grantResults.length == 0 || grantResults[0] != PackageManager.PERMISSION_GRANTED)) {
            Toast.makeText(this, "Without audio access the ribbon uses its built-in motion", Toast.LENGTH_LONG).show();
        } else if (requestCode == REQ_AUDIO) {
            header.voice.stop();
            header.voice.start();
        }
    }
}
