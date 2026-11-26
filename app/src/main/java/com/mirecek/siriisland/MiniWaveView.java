package com.mirecek.siriisland;

import android.content.Context;
import android.graphics.BlurMaskFilter;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Shader;
import android.view.View;

import java.util.Arrays;

/** Small black tile with a glowing wave in a theme's colours: the art on theme and wave-style cards. */
final class MiniWaveView extends View {
    static final int IOS27 = 0, BUBBLE = 1, PRISM = 2, CLASSIC = 3, STRANDS = 4;
    private static final int[] SPECTRUM = {0xFFFF9F43, 0xFFFF5EA8, 0xFFFFFFFF, 0xFF6AD5FF, 0xFF7B7BFF};

    private final float dp;
    private final Paint tile = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint glow = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint core = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path path = new Path();
    private int[] colors;
    private int mode;
    private Shader shader;

    MiniWaveView(Context c, Themes.Theme theme, int mode) {
        super(c);
        dp = c.getResources().getDisplayMetrics().density;
        tile.setColor(0xFF000000);
        glow.setStyle(Paint.Style.STROKE);
        glow.setStrokeCap(Paint.Cap.ROUND);
        glow.setStrokeWidth(5 * dp);
        glow.setMaskFilter(new BlurMaskFilter(5 * dp, BlurMaskFilter.Blur.NORMAL));
        core.setStyle(Paint.Style.STROKE);
        core.setStrokeCap(Paint.Cap.ROUND);
        core.setStrokeWidth(1.8f * dp);
        this.mode = mode;
        setTheme(theme);
        setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
    }

    static int modeFor(String style) {
        if (Prefs.STYLE_BUBBLE.equals(style)) return BUBBLE;
        if (Prefs.STYLE_PRISM.equals(style)) return PRISM;
        if (Prefs.STYLE_CLASSIC.equals(style)) return CLASSIC;
        if (Prefs.STYLE_IOS27.equals(style)) return IOS27;
        return STRANDS;
    }

    void setTheme(Themes.Theme t) {
        int[] next = t.spectral ? SPECTRUM : new int[]{
                0xFF000000 | t.colors[0], 0xFF000000 | t.colors[1], 0xFFFFFFFF,
                0xFF000000 | t.colors[2], 0xFF000000 | t.colors[3]};
        if (Arrays.equals(next, colors)) return;
        colors = next;
        lobeColors = t.spectral ? BRAID_SPECTRAL : new int[]{
                0xFF000000 | t.colors[0], 0xFF000000 | t.colors[1], 0xFF000000 | t.colors[2], 0xFF000000 | t.colors[3]};
        strandsRgb = Themes.strandsColors(t);
        buildShader();
        invalidate();
    }

    void setMode(int m) {
        if (m == mode) return;
        mode = m;
        invalidate();
    }

    @Override
    protected void onSizeChanged(int w, int h, int ow, int oh) {
        super.onSizeChanged(w, h, ow, oh);
        buildShader();
    }

    private void buildShader() {
        int w = getWidth(), h = getHeight();
        shader = w > 0 && colors != null
                ? new LinearGradient(w * 0.1f, 0, w * 0.9f, 0, colors, null, Shader.TileMode.CLAMP) : null;
        if (h > 0 && strandsRgb != null) {
            float cy = h * 0.5f;
            sheetUp = new LinearGradient(0, cy, 0, cy - 0.32f * h, rgb(2), rgb(0), Shader.TileMode.CLAMP);
            sheetDown = new LinearGradient(0, cy, 0, cy + 0.32f * h, rgb(4), rgb(6), Shader.TileMode.CLAMP);
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        float w = getWidth(), h = getHeight(), cy = h * 0.5f;
        canvas.drawRoundRect(0, 0, w, h, 10 * dp, 10 * dp, tile);
        if (shader == null) return;
        glow.setShader(shader);
        core.setShader(shader);
        switch (mode) {
            case BUBBLE:
                glow.setStyle(Paint.Style.FILL);
                canvas.drawOval(w * 0.2f, cy - h * 0.2f, w * 0.8f, cy + h * 0.2f, glow);
                glow.setStyle(Paint.Style.STROKE);
                wave(w, cy, 0.08f * h, 1);
                canvas.drawPath(path, core);
                break;
            case PRISM:
                for (int i = -1; i <= 1; i++) {
                    wave(w, cy + i * 0.12f * h, 0.1f * h, 1);
                    canvas.drawPath(path, glow);
                    canvas.drawPath(path, core);
                }
                break;
            case CLASSIC:
                wave(w, cy, 0.2f * h, 3);
                canvas.drawPath(path, glow);
                canvas.drawPath(path, core);
                break;
            case IOS27:
                braid(canvas, w, cy, h);
                break;
            default:
                strandsArt(canvas, w, cy, h);
        }
    }

    /** Lobe colours for the iOS 27 braid: the Siri spectrum, or the theme's four colours. */
    private static final int[] BRAID_SPECTRAL = {0xFFFF3B30, 0xFFB4FF3C, 0xFF2EC5FF, 0xFFD24DFF};
    private final Paint lobe = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint lobeGlow = new Paint(Paint.ANTI_ALIAS_FLAG);
    private int[] lobeColors = BRAID_SPECTRAL;
    private float[] strandsRgb;
    private Shader sheetUp, sheetDown;

    /** iOS 27 art: four lens-shaped lobes braided across each other, white where they overlap. */
    private void braid(Canvas canvas, float w, float cy, float h) {
        lobe.setShader(null);
        lobe.setBlendMode(android.graphics.BlendMode.PLUS);
        lobeGlow.setBlendMode(android.graphics.BlendMode.PLUS);
        if (lobeGlow.getMaskFilter() == null) lobeGlow.setMaskFilter(new BlurMaskFilter(4 * dp, BlurMaskFilter.Blur.NORMAL));
        float[][] spec = {{0.66f, 0.08f, 1.0f, 0.085f}, {0.6f, -0.08f, -1.2f, 0.075f}, {0.72f, 0.03f, 1.6f, 0.065f}, {0.56f, -0.03f, -0.7f, 0.08f}};
        for (int i = 0; i < 4; i++) {
            float ext = spec[i][0] * w / 2f, x0 = w / 2f + spec[i][1] * w;
            float freq = spec[i][2], thick = spec[i][3] * h;
            path.rewind();
            for (int k = 0; k <= 32; k++) {
                float xn = -1f + 2f * k / 32f;
                float env = (float) Math.pow(Math.max(0, 1 - xn * xn), 0.8);
                float c = cy + 0.11f * h * env * (float) Math.sin(xn * freq * 2f);
                float px = x0 + xn * ext, py = c - thick * env;
                if (k == 0) path.moveTo(px, py); else path.lineTo(px, py);
            }
            for (int k = 32; k >= 0; k--) {
                float xn = -1f + 2f * k / 32f;
                float env = (float) Math.pow(Math.max(0, 1 - xn * xn), 0.8);
                float c = cy + 0.11f * h * env * (float) Math.sin(xn * freq * 2f);
                path.lineTo(x0 + xn * ext, c + thick * env);
            }
            path.close();
            int col = lobeColors[i];
            lobeGlow.setColor(col);
            lobeGlow.setAlpha(90);
            canvas.drawPath(path, lobeGlow);
            lobe.setColor(col);
            lobe.setAlpha(170);
            canvas.drawPath(path, lobe);
        }
    }

    /** Strands art: a warm sheet above and a cool sheet below the axis, swelling into a bulge left of centre. */
    private void strandsArt(Canvas canvas, float w, float cy, float h) {
        lobe.setBlendMode(android.graphics.BlendMode.PLUS);
        sheet(canvas, w, cy, h, -1, sheetUp);
        sheet(canvas, w, cy, h, 1, sheetDown);
        path.rewind();
        for (int k = 0; k <= 32; k++) {
            float xn = -1f + 2f * k / 32f;
            float px = w * (0.1f + 0.4f * (xn + 1f)), py = cy + 0.06f * h * xn;
            if (k == 0) path.moveTo(px, py); else path.lineTo(px, py);
        }
        core.setShader(null);
        core.setColor(rgb(3));
        canvas.drawPath(path, core);
    }

    /** One sheet: {@code dir} -1 grows up, +1 down; gradient from the axis colour to the outer colour. */
    private void sheet(Canvas canvas, float w, float cy, float h, int dir, Shader fill) {
        path.rewind();
        for (int k = 0; k <= 32; k++) {
            float xn = -1f + 2f * k / 32f;
            float env = (float) Math.pow(Math.max(0, 1 - xn * xn), 1.2);
            float bump = 1f + 0.8f * (float) Math.exp(-Math.pow((xn + 0.15) / 0.35, 2));
            float px = w * (0.1f + 0.4f * (xn + 1f));
            float py = cy + 0.06f * h * xn + dir * 0.17f * h * env * bump;
            if (k == 0) path.moveTo(px, py); else path.lineTo(px, py);
        }
        for (int k = 32; k >= 0; k--) {
            float xn = -1f + 2f * k / 32f;
            path.lineTo(w * (0.1f + 0.4f * (xn + 1f)), cy + 0.06f * h * xn);
        }
        path.close();
        lobe.setShader(fill);
        lobe.setAlpha(210);
        canvas.drawPath(path, lobe);
    }

    private int rgb(int role) {
        return 0xFF000000 | (Math.round(strandsRgb[role * 3] * 255) << 16)
                | (Math.round(strandsRgb[role * 3 + 1] * 255) << 8) | Math.round(strandsRgb[role * 3 + 2] * 255);
    }

    /** A sine wave across the tile with {@code lobes} half-periods, fading in from both ends. */
    private void wave(float w, float cy, float amp, int lobes) {
        path.rewind();
        for (int i = 0; i <= 40; i++) {
            float x = i / 40f;
            float env = (float) Math.sin(Math.PI * x);
            float y = cy - amp * env * (float) Math.sin(Math.PI * lobes * x);
            float px = w * (0.1f + 0.8f * x);
            if (i == 0) path.moveTo(px, y); else path.lineTo(px, y);
        }
    }
}
