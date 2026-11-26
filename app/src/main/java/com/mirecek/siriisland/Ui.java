package com.mirecek.siriisland;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.InsetDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.os.SystemClock;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;

import java.util.Locale;
import java.util.function.Function;
import java.util.function.IntFunction;

/**
 * iOS 27 building blocks for the settings pages, after the Liquid Glass apps ClearPDF, Convx and
 * BitChord (Kyant's AndroidLiquidGlass components): content on black in solid grouped cards; glass
 * only on controls (switch thumb, slider thumb, segmented lens, back button). Pressed controls swell
 * and lean toward the finger, rows highlight; nothing ripples. Controls that edit a setting write
 * their {@link Prefs} key directly.
 */
final class Ui {
    static final int BG = 0xFF000000;
    static final int CARD = 0xFF1C1C1E;
    static final int RAISED = 0xFF2C2C2E;
    static final int SEPARATOR = 0xFF38383A;
    static final int SECONDARY = 0xFF8E8E93;
    static final int CHEVRON = 0xFF5A5A5E;
    static final int ACCENT = 0xFF0A84FF;
    static final int SUCCESS = 0xFF30D158;
    static final int WARNING = 0xFFFF9F0A;
    static final int DESTRUCTIVE = 0xFFFF453A;
    static final int WRAP = ViewGroup.LayoutParams.WRAP_CONTENT;
    static final int MATCH = ViewGroup.LayoutParams.MATCH_PARENT;

    /** A picker or segmented control whose selection can be re-synced from the prefs. */
    interface Choice {
        View view();

        void select(String id);
    }

    final Context c;
    private final float dp;

    Ui(Context c) {
        this.c = c;
        dp = c.getResources().getDisplayMetrics().density;
    }

    int px(float v) { return Math.round(v * dp); }

    TextView text(String s, float sp, int color, boolean bold) {
        TextView t = new TextView(c);
        t.setText(s);
        t.setTextSize(TypedValue.COMPLEX_UNIT_SP, sp);
        t.setTextColor(color);
        if (bold) t.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        return t;
    }

    LinearLayout column() {
        LinearLayout col = new LinearLayout(c);
        col.setOrientation(LinearLayout.VERTICAL);
        col.setPadding(0, 0, 0, px(32));
        return col;
    }

    ScrollView scroll(View content) {
        ScrollView s = new ScrollView(c);
        s.setBackgroundColor(BG);
        s.setOverScrollMode(View.OVER_SCROLL_IF_CONTENT_SCROLLS);
        s.addView(content);
        return s;
    }

    /** Liquid glass material: translucent fill, top sheen, specular rim. */
    GlassDrawable glass(float radiusDp, int fill, float rim) {
        return new GlassDrawable(px(radiusDp), fill, rim, Math.max(1f, 0.9f * dp));
    }

    /**
     * Kyant's InteractiveHighlight (LiquidButton): while pressed the control swells by 4 dp and
     * leans toward the finger (rubber-banded with tanh), stretching along the drag; on release it
     * springs back. Only observes touches, so clicks work as before.
     */
    <T extends View> T pressable(T v) {
        v.setOnTouchListener(new View.OnTouchListener() {
            private float x0, y0;

            @Override public boolean onTouch(View view, android.view.MotionEvent e) {
                float w = view.getWidth(), h = view.getHeight();
                if (w <= 0 || h <= 0) return false;
                switch (e.getActionMasked()) {
                    case android.view.MotionEvent.ACTION_DOWN -> {
                        x0 = e.getRawX();
                        y0 = e.getRawY();
                        float s = 1f + px(4) / h;
                        view.animate().scaleX(s).scaleY(s).translationX(0f).translationY(0f)
                                .setDuration(160).setInterpolator(new DecelerateInterpolator()).start();
                    }
                    case android.view.MotionEvent.ACTION_MOVE -> {
                        float ox = e.getRawX() - x0, oy = e.getRawY() - y0;
                        float max = Math.min(w, h), maxDim = Math.max(w, h);
                        view.animate().cancel();
                        view.setTranslationX(max * (float) Math.tanh(0.05f * ox / max));
                        view.setTranslationY(max * (float) Math.tanh(0.05f * oy / max));
                        double a = Math.atan2(oy, ox);
                        float s = 1f + px(4) / h, drag = px(4) / h;
                        view.setScaleX(s + drag * (float) Math.abs(Math.cos(a) * ox / maxDim) * Math.min(1f, w / h));
                        view.setScaleY(s + drag * (float) Math.abs(Math.sin(a) * oy / maxDim) * Math.min(1f, h / w));
                    }
                    case android.view.MotionEvent.ACTION_UP, android.view.MotionEvent.ACTION_CANCEL ->
                            view.animate().scaleX(1f).scaleY(1f).translationX(0f).translationY(0f)
                                    .setDuration(420).setInterpolator(new OvershootInterpolator(1.8f)).start();
                    default -> { }
                }
                return false;
            }
        });
        return v;
    }

    TextView largeTitle(String s) {
        TextView t = text(s, 34, Color.WHITE, true);
        t.setPadding(px(16), px(6), px(16), px(8));
        return t;
    }

    /** Sub-page bar: a round glass back button with a chevron on the left, centred title. */
    View navBar(String back, String title, Runnable onBack) {
        FrameLayout bar = new FrameLayout(c);
        ImageView b = new ImageView(c);
        b.setImageResource(R.drawable.ic_chevron_back);
        b.setImageTintList(ColorStateList.valueOf(Color.WHITE));
        b.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        b.setPadding(px(11), px(11), px(11), px(11));
        b.setBackground(glass(22, 0xFF232325, 0.35f));
        b.setOnClickListener(v -> onBack.run());
        b.setContentDescription("Back to " + back);
        pressable(b);
        FrameLayout.LayoutParams blp = new FrameLayout.LayoutParams(px(44), px(44), Gravity.START | Gravity.CENTER_VERTICAL);
        blp.setMarginStart(px(16));
        bar.addView(b, blp);
        TextView t = text(title, 17, Color.WHITE, true);
        bar.addView(t, new FrameLayout.LayoutParams(WRAP, WRAP, Gravity.CENTER));
        bar.setMinimumHeight(px(58));
        return bar;
    }

    /** Uppercase section header (or a gap when null) plus a grouped card, both added to {@code col}. */
    LinearLayout section(LinearLayout col, String header) {
        if (header != null) {
            TextView h = text(header.toUpperCase(Locale.ROOT), 13, SECONDARY, false);
            h.setPadding(px(32), px(22), px(32), px(7));
            col.addView(h);
        } else {
            col.addView(new View(c), new LinearLayout.LayoutParams(MATCH, px(18)));
        }
        LinearLayout g = new LinearLayout(c);
        g.setOrientation(LinearLayout.VERTICAL);
        g.setBackground(rounded(CARD, 26));
        g.setClipToOutline(true);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(MATCH, WRAP);
        lp.setMargins(px(16), 0, px(16), 0);
        col.addView(g, lp);
        return g;
    }

    /** Adds a row with an iOS hairline separator above every row but the first. */
    void add(LinearLayout group, View row, int separatorInsetDp) {
        if (group.getChildCount() > 0) {
            View sep = new View(c);
            sep.setBackgroundColor(SEPARATOR);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(MATCH, Math.max(1, px(0.5f)));
            lp.setMarginStart(px(separatorInsetDp));
            group.addView(sep, lp);
        }
        group.addView(row, new LinearLayout.LayoutParams(MATCH, WRAP));
    }

    void add(LinearLayout group, View row) { add(group, row, 16); }

    TextView footnote(LinearLayout col, String s) {
        TextView t = text(s, 13, SECONDARY, false);
        t.setPadding(px(32), px(7), px(32), 0);
        t.setLineSpacing(0, 1.15f);
        col.addView(t);
        return t;
    }

    /** Row: coloured rounded-square icon, label, optional value, chevron. */
    View navRow(int iconRes, int iconColor, String label, TextView value, Runnable onClick) {
        LinearLayout r = hRow();
        FrameLayout icon = new FrameLayout(c);
        icon.setBackground(rounded(iconColor, 7));
        ImageView glyph = new ImageView(c);
        glyph.setImageResource(iconRes);
        glyph.setImageTintList(ColorStateList.valueOf(Color.WHITE));
        glyph.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        icon.addView(glyph, new FrameLayout.LayoutParams(px(19), px(19), Gravity.CENTER));
        LinearLayout.LayoutParams ilp = new LinearLayout.LayoutParams(px(30), px(30));
        ilp.setMarginEnd(px(14));
        r.addView(icon, ilp);
        r.addView(text(label, 17, Color.WHITE, false), new LinearLayout.LayoutParams(0, WRAP, 1f));
        if (value != null) r.addView(value);
        ImageView chev = new ImageView(c);
        chev.setImageResource(R.drawable.ic_chevron);
        chev.setImageTintList(ColorStateList.valueOf(CHEVRON));
        chev.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        LinearLayout.LayoutParams clp = new LinearLayout.LayoutParams(px(18), px(18));
        clp.setMarginStart(px(8));
        r.addView(chev, clp);
        r.setBackground(ripple());
        r.setOnClickListener(v -> onClick.run());
        return r;
    }

    TextView value(String s) { return text(s, 17, SECONDARY, false); }

    /** Label with a small status line on the left and an optional action on the right. */
    LinearLayout infoRow(String label, TextView status, View action) {
        LinearLayout r = hRow();
        LinearLayout texts = new LinearLayout(c);
        texts.setOrientation(LinearLayout.VERTICAL);
        texts.addView(text(label, 17, Color.WHITE, false));
        texts.addView(status);
        r.addView(texts, new LinearLayout.LayoutParams(0, WRAP, 1f));
        if (action != null) r.addView(action, new LinearLayout.LayoutParams(WRAP, px(48)));
        return r;
    }

    /** Label with an iOS 27 liquid toggle; tapping the label toggles it too. */
    View switchRow(String label, String key, boolean def) {
        return toggleRow(null, label, null, Prefs.get(c).getBoolean(key, def),
                on -> Prefs.get(c).edit().putBoolean(key, on).apply());
    }

    /** Optional app icon, label with an optional second line, and a liquid toggle. */
    View toggleRow(Drawable icon, String label, String detail, boolean on, LiquidToggle.OnChange onChange) {
        LinearLayout r = hRow();
        r.setPadding(px(16), 0, px(4), 0);
        if (icon != null) {
            ImageView iv = new ImageView(c);
            iv.setImageDrawable(icon);
            iv.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
            LinearLayout.LayoutParams ilp = new LinearLayout.LayoutParams(px(30), px(30));
            ilp.setMarginEnd(px(14));
            r.addView(iv, ilp);
        }
        LinearLayout texts = new LinearLayout(c);
        texts.setOrientation(LinearLayout.VERTICAL);
        TextView t = text(label, 17, Color.WHITE, false);
        t.setSingleLine(true);
        t.setEllipsize(android.text.TextUtils.TruncateAt.END);
        texts.addView(t);
        if (detail != null) texts.addView(text(detail, 13, SECONDARY, false));
        r.addView(texts, new LinearLayout.LayoutParams(0, WRAP, 1f));
        LiquidToggle s = new LiquidToggle(c, on);
        s.setContentDescription(label);
        s.setOnChange(onChange);
        r.addView(s, new LinearLayout.LayoutParams(WRAP, WRAP));
        r.setBackground(ripple());
        r.setOnClickListener(v -> s.performClick());
        // The switch itself carries the label for TalkBack; the row is only a bigger tap target.
        r.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        return r;
    }

    /** Slider bound to an int pref; the formatted value is shown on the right and updates while dragging. */
    View sliderRow(String label, String key, int min, int max, int def, IntFunction<String> format, Runnable onRelease) {
        LinearLayout r = new LinearLayout(c);
        r.setOrientation(LinearLayout.VERTICAL);
        r.setPadding(px(16), px(10), px(16), px(4));
        LinearLayout top = new LinearLayout(c);
        top.setOrientation(LinearLayout.HORIZONTAL);
        top.addView(text(label, 17, Color.WHITE, false), new LinearLayout.LayoutParams(0, WRAP, 1f));
        int start = Prefs.get(c).getInt(key, def);
        TextView val = text(format.apply(start), 17, SECONDARY, false);
        top.addView(val);
        r.addView(top);
        // Spans the full row (negative margins) so the lens has room at both ends; the padding keeps
        // the track aligned with the label.
        SeekBar bar = new GlassSlider(c);
        bar.setPadding(px(32), 0, px(32), 0);
        bar.setMin(min);
        bar.setMax(max);
        bar.setProgress(start);
        bar.setContentDescription(label);
        bar.setStateDescription(format.apply(start));
        bar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar s, int p, boolean user) {
                val.setText(format.apply(p));
                bar.setStateDescription(format.apply(p));
                if (user) Prefs.get(c).edit().putInt(key, p).apply();
            }
            @Override public void onStartTrackingTouch(SeekBar s) {}
            @Override public void onStopTrackingTouch(SeekBar s) { if (onRelease != null) onRelease.run(); }
        });
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(MATCH, px(48));
        lp.leftMargin = -px(16);
        lp.rightMargin = -px(16);
        r.addView(bar, lp);
        return r;
    }

    Choice segmented(String key, String[] ids, String[] names, String selected) {
        return new Segments(key, ids, names, selected);
    }

    Choice cardPicker(String key, String[] ids, String[] names, String selected, Function<String, View> art) {
        return new Cards(key, ids, names, selected, art, false);
    }

    Choice gridPicker(String key, String[] ids, String[] names, String selected, Function<String, View> art) {
        return new Cards(key, ids, names, selected, art, true);
    }

    /** Small capsule action button for rows: 48 dp tap area, drawn 34 dp tall, accent filled. */
    TextView pill(String label, Runnable onClick) {
        TextView b = text(label, 15, Color.WHITE, true);
        b.setGravity(Gravity.CENTER);
        b.setMinHeight(px(48));
        b.setBackground(new InsetDrawable(glass(17, ACCENT, 0.3f), 0, px(7), 0, px(7)));
        b.setPadding(px(16), 0, px(16), 0); // after setBackground, which resets padding to the drawable's
        b.setOnClickListener(v -> onClick.run());
        return pressable(b);
    }

    /** Full-width 50 dp capsule button (Kyant LiquidButton look: filled, lit rim, no ripple). */
    TextView button(String label, int color, Runnable onClick) {
        TextView b = text(label, 17, Color.WHITE, true);
        b.setGravity(Gravity.CENTER);
        b.setMinHeight(px(50));
        b.setBackground(glass(25, color, 0.3f));
        b.setOnClickListener(v -> onClick.run());
        return pressable(b);
    }

    LinearLayout.LayoutParams fullWidth(int topDp) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(MATCH, WRAP);
        lp.setMargins(px(16), px(topDp), px(16), 0);
        return lp;
    }

    GradientDrawable rounded(int color, float radiusDp) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(color);
        g.setCornerRadius(px(radiusDp));
        return g;
    }

    /** Row press: a flat highlight that fades out (iOS cells), no ripple. */
    Drawable ripple() {
        android.graphics.drawable.StateListDrawable s = new android.graphics.drawable.StateListDrawable();
        s.addState(new int[]{android.R.attr.state_pressed}, new ColorDrawable(RAISED));
        s.addState(new int[0], new ColorDrawable(Color.TRANSPARENT));
        s.setExitFadeDuration(250);
        return s;
    }

    private LinearLayout hRow() {
        LinearLayout r = new LinearLayout(c);
        r.setOrientation(LinearLayout.HORIZONTAL);
        r.setGravity(Gravity.CENTER_VERTICAL);
        r.setMinimumHeight(px(52));
        r.setPadding(px(16), px(8), px(16), px(8));
        return r;
    }

    /**
     * iOS 27 segmented control: the selected segment is a lit capsule that slides on a spring,
     * swells as it leaves (Kyant's pressed scale) and stretches along its motion (volume kept).
     */
    private final class Segments implements Choice {
        private final FrameLayout root = new FrameLayout(c);
        private final View lens = new View(c);
        private final String[] ids;
        private final TextView[] items;
        private final Spring pos = new Spring();
        private final Spring swell = new Spring();
        private int selected = -1;
        private boolean placed;
        private long last;
        private final Runnable tick = this::step;

        Segments(String key, String[] ids, String[] names, String selected) {
            this.ids = ids;
            FrameLayout frame = new FrameLayout(c);
            frame.setBackground(rounded(RAISED, 100));
            frame.setPadding(px(3), px(3), px(3), px(3)); // after setBackground, which resets padding
            lens.setBackground(glass(100, 0xFF636366, 0.35f));
            lens.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
            frame.addView(lens, new FrameLayout.LayoutParams(0, MATCH));
            LinearLayout seg = new LinearLayout(c);
            seg.setOrientation(LinearLayout.HORIZONTAL);
            items = new TextView[ids.length];
            for (int i = 0; i < ids.length; i++) {
                String id = ids[i];
                TextView t = text(names[i], 14, Color.WHITE, false);
                t.setGravity(Gravity.CENTER);
                t.setMinHeight(px(42));
                t.setContentDescription(names[i]);
                t.setOnClickListener(v -> {
                    Prefs.get(c).edit().putString(key, id).apply();
                    select(id);
                });
                items[i] = t;
                seg.addView(t, new LinearLayout.LayoutParams(0, WRAP, 1f));
            }
            frame.addView(seg, new FrameLayout.LayoutParams(MATCH, WRAP));
            // Segments share the width equally: size the lens to one and place it once laid out.
            seg.addOnLayoutChangeListener((v, l, t, r, b, ol, ot, or, ob) -> {
                if (this.selected < 0 || items[0].getWidth() == 0) return;
                ViewGroup.LayoutParams lp = lens.getLayoutParams();
                if (lp.width != items[0].getWidth()) {
                    lp.width = items[0].getWidth();
                    lens.setLayoutParams(lp);
                }
                if (!placed || r - l != or - ol) {
                    placed = true;
                    pos.snap(items[this.selected].getLeft());
                    lens.setTranslationX(pos.x);
                }
            });
            root.setPadding(px(16), px(8), px(16), px(8));
            root.addView(frame, new FrameLayout.LayoutParams(MATCH, WRAP));
            select(selected);
        }

        @Override public View view() { return root; }

        @Override public void select(String id) {
            for (int i = 0; i < items.length; i++) {
                boolean on = ids[i].equals(id);
                items[i].setTypeface(Typeface.create(on ? "sans-serif-medium" : "sans-serif", Typeface.NORMAL));
                items[i].setTextColor(on ? Color.WHITE : 0xCCFFFFFF);
                items[i].setSelected(on);
                if (on) selected = i;
            }
            if (!placed || selected < 0) return;
            pos.target = items[selected].getLeft();
            if (Prefs.reduceMotion(c)) {
                pos.snap(pos.target);
                lens.setTranslationX(pos.x);
                return;
            }
            last = 0;
            swell.x = 1.08f;
            swell.v = 0f;
            swell.target = 1f;
            lens.removeCallbacks(tick);
            lens.postOnAnimation(tick);
        }

        private void step() {
            long now = SystemClock.uptimeMillis();
            float dt = last == 0 ? 1f / 120f : Math.min(0.05f, (now - last) / 1000f);
            last = now;
            // A slightly underdamped spring: the lens overshoots a hair and settles in about 0.4 s.
            pos.step(dt, 20f, 0.72f);
            swell.step(dt, 16f, 0.6f);
            float s = 1f + Math.min(0.35f, Math.abs(pos.v) / (2400f * dp));
            lens.setTranslationX(pos.x);
            lens.setScaleX(s * swell.x);
            lens.setScaleY(swell.x / (float) Math.sqrt(s));
            if (!pos.settled(0.5f) || !swell.settled(0.002f)) lens.postOnAnimation(tick);
            else {
                pos.snap(pos.target);
                swell.snap(1f);
                lens.setTranslationX(pos.x);
                lens.setScaleX(1f);
                lens.setScaleY(1f);
            }
        }
    }

    private final class Cards implements Choice {
        private final View root;
        private final String[] ids;
        private final View[] cards;

        Cards(String key, String[] ids, String[] names, String selected, Function<String, View> art, boolean grid) {
            this.ids = ids;
            cards = new View[ids.length];
            for (int i = 0; i < ids.length; i++) {
                String id = ids[i];
                View card = card(names[i], art.apply(id), grid ? 46 : 56);
                card.setOnClickListener(v -> {
                    Prefs.get(c).edit().putString(key, id).apply();
                    select(id);
                });
                cards[i] = card;
            }
            if (grid) {
                LinearLayout g = new LinearLayout(c);
                g.setOrientation(LinearLayout.VERTICAL);
                g.setPadding(px(8), px(8), px(8), px(8));
                for (int i = 0; i < cards.length; i += 2) {
                    LinearLayout row = new LinearLayout(c);
                    row.setOrientation(LinearLayout.HORIZONTAL);
                    row.setWeightSum(2f);
                    for (int k = i; k < Math.min(i + 2, cards.length); k++) {
                        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, WRAP, 1f);
                        lp.setMargins(px(4), px(4), px(4), px(4));
                        row.addView(cards[k], lp);
                    }
                    g.addView(row);
                }
                root = g;
            } else {
                LinearLayout strip = new LinearLayout(c);
                strip.setOrientation(LinearLayout.HORIZONTAL);
                strip.setPadding(px(12), px(12), px(2), px(12));
                for (View card : cards) {
                    LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(px(104), WRAP);
                    lp.setMarginEnd(px(10));
                    strip.addView(card, lp);
                }
                HorizontalScrollView hs = new HorizontalScrollView(c);
                hs.setHorizontalScrollBarEnabled(false);
                hs.addView(strip);
                root = hs;
            }
            select(selected);
        }

        private View card(String name, View art, int artHeightDp) {
            LinearLayout card = new LinearLayout(c);
            card.setOrientation(LinearLayout.VERTICAL);
            card.setBackground(glass(18, RAISED, 0.1f));
            card.setPadding(px(8), px(8), px(8), px(8)); // after setBackground, which resets padding
            card.setClipToOutline(true);
            card.addView(art, new LinearLayout.LayoutParams(MATCH, px(artHeightDp)));
            TextView t = text(name, 13, Color.WHITE, false);
            t.setGravity(Gravity.CENTER);
            t.setSingleLine(true);
            t.setPadding(0, px(6), 0, 0);
            card.addView(t, new LinearLayout.LayoutParams(MATCH, WRAP));
            card.setContentDescription(name);
            return pressable(card);
        }

        @Override public View view() { return root; }

        @Override public void select(String id) {
            for (int i = 0; i < cards.length; i++) {
                boolean on = ids[i].equals(id);
                ((GlassDrawable) cards[i].getBackground()).setAccent(on ? ACCENT : 0);
                cards[i].setSelected(on);
            }
        }
    }
}
