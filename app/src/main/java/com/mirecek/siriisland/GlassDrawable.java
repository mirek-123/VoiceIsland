package com.mirecek.siriisland;

import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.LinearGradient;
import android.graphics.Outline;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;

/**
 * iOS 27 Liquid Glass material for the settings screen: a translucent fill over the ambient
 * background, a soft sheen from the top, and a specular rim that is bright at the top-left and
 * fades toward the bottom-right. The ambient behind is already a soft blur of colour, so a
 * translucent fill reads exactly like frosted glass over it without a backdrop blur pass.
 * An optional accent ring marks a selected card.
 */
final class GlassDrawable extends Drawable {
    private final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint sheen = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint rim = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint ring = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF r = new RectF();
    private final float radius;
    private final float stroke;
    private final float rimAlpha;
    private int accent;

    /**
     * @param radiusPx  corner radius; use a huge value for a capsule
     * @param fillColor ARGB fill, usually translucent white
     * @param rimAlpha  brightness of the specular rim at its brightest point, 0..1
     * @param strokePx  rim width
     */
    GlassDrawable(float radiusPx, int fillColor, float rimAlpha, float strokePx) {
        radius = radiusPx;
        stroke = strokePx;
        this.rimAlpha = rimAlpha;
        fill.setColor(fillColor);
        rim.setStyle(Paint.Style.STROKE);
        rim.setStrokeWidth(strokePx);
        ring.setStyle(Paint.Style.STROKE);
    }

    /** Accent ring for a selected card; 0 removes it. */
    void setAccent(int color) {
        if (accent == color) return;
        accent = color;
        invalidateSelf();
    }

    @Override
    protected void onBoundsChange(Rect b) {
        int a = Math.round(255 * rimAlpha);
        rim.setShader(new LinearGradient(b.left, b.top, b.right, b.bottom,
                new int[]{a << 24 | 0xFFFFFF, (a / 5) << 24 | 0xFFFFFF, (a / 2) << 24 | 0xFFFFFF},
                new float[]{0f, 0.6f, 1f}, Shader.TileMode.CLAMP));
        sheen.setShader(new LinearGradient(0, b.top, 0, b.top + Math.max(1, b.height() * 0.45f),
                0x14FFFFFF, 0x00FFFFFF, Shader.TileMode.CLAMP));
    }

    private float cornerFor(RectF rect) {
        return Math.min(radius, Math.min(rect.width(), rect.height()) / 2f);
    }

    @Override
    public void draw(Canvas canvas) {
        r.set(getBounds());
        float cr = cornerFor(r);
        canvas.drawRoundRect(r, cr, cr, fill);
        canvas.drawRoundRect(r, cr, cr, sheen);
        r.inset(stroke / 2f, stroke / 2f);
        canvas.drawRoundRect(r, cr, cr, rim);
        if (accent != 0) {
            r.inset(stroke, stroke);
            // A soft halo inside a crisp ring.
            ring.setColor(accent & 0x55FFFFFF);
            ring.setStrokeWidth(stroke * 5f);
            canvas.drawRoundRect(r, cr, cr, ring);
            ring.setColor(accent);
            ring.setStrokeWidth(stroke * 2f);
            canvas.drawRoundRect(r, cr, cr, ring);
        }
    }

    @Override
    public void getOutline(Outline outline) {
        Rect b = getBounds();
        outline.setRoundRect(b, Math.min(radius, Math.min(b.width(), b.height()) / 2f));
    }

    @Override public void setAlpha(int alpha) { fill.setAlpha(alpha); invalidateSelf(); }

    @Override public void setColorFilter(ColorFilter cf) { fill.setColorFilter(cf); invalidateSelf(); }

    @Override public int getOpacity() { return PixelFormat.TRANSLUCENT; }
}
