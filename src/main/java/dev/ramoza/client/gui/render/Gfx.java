package dev.ramoza.client.gui.render;

import dev.ramoza.client.util.ColorUtil;
import dev.ramoza.client.util.MathUtil;

/**
 * High level drawing on top of {@link Renderer2D}: anti-aliased rounded rectangles, borders,
 * soft shadows, gradients, text helpers, pixel icons and a global alpha stack.
 */
public final class Gfx {
    private final Renderer2D r;
    private float alpha = 1f;
    /** Extra multiplier for all text (GUI Settings -> Text Opacity). */
    private float textAlpha = 1f;
    /** Edge anti-aliasing for rounded shapes. Switched off internally for soft shadows (cheap + invisible). */
    private boolean antiAlias = true;
    private final float[] alphaStack = new float[24];
    private int alphaTop = 0;

    /** Corner inset tables per quantised radius (1/4 px steps). Avoids a sqrt per row per shape per frame. */
    private static final float[][] CORNER = new float[64][];

    private static final class Fit {
        int w;
        String out;
    }

    /** Cache for {@link #fit}: the ellipsis search measures text many times, so do it once per (string, width). */
    private final java.util.HashMap<String, Fit> fits = new java.util.HashMap<>();

    public Gfx(Renderer2D r) { this.r = r; }

    // ------------------------------------------------------------ alpha / state

    public void pushAlpha(float mul) {
        if (alphaTop < alphaStack.length) alphaStack[alphaTop++] = alpha;
        alpha *= MathUtil.saturate(mul);
    }

    public void popAlpha() {
        if (alphaTop > 0) alpha = alphaStack[--alphaTop];
    }

    public float alpha() { return alpha; }

    public void setTextAlpha(float a) { textAlpha = MathUtil.saturate(a); }

    private int tc(int color) {
        color = c(color);
        return textAlpha >= 0.999f ? color : ColorUtil.mulAlpha(color, textAlpha);
    }

    private int c(int color) {
        return alpha >= 0.999f ? color : ColorUtil.mulAlpha(color, alpha);
    }

    public void pushScissor(float x, float y, float w, float h) {
        int ix = Math.round(x), iy = Math.round(y);
        r.pushScissor(ix, iy, Math.max(0, Math.round(x + w) - ix), Math.max(0, Math.round(y + h) - iy));
    }

    public void popScissor() { r.popScissor(); }

    public void pushTransform(float tx, float ty, float sx, float sy) { r.pushTransform(tx, ty, sx, sy); }

    public void popTransform() { r.popTransform(); }

    // ------------------------------------------------------------ rectangles

    public void rect(float x, float y, float w, float h, int color) {
        int ix = Math.round(x), iy = Math.round(y);
        int iw = Math.round(x + w) - ix, ih = Math.round(y + h) - iy;
        if (iw <= 0 || ih <= 0) return;
        color = c(color);
        if (ColorUtil.alpha(color) == 0) return;
        r.fillRect(ix, iy, iw, ih, color);
    }

    public void roundRect(float x, float y, float w, float h, float radius, int color) {
        fillRounded(x, y, w, h, radius, color, color, true, true);
    }

    /** Rounded rectangle with a vertical gradient. */
    public void roundRectV(float x, float y, float w, float h, float radius, int top, int bottom) {
        fillRounded(x, y, w, h, radius, top, bottom, true, true);
    }

    /** Rounded rectangle where the top and/or bottom pair of corners can be left square (panel headers, bodies). */
    public void roundRectEx(float x, float y, float w, float h, float radius, int color,
                            boolean roundTop, boolean roundBottom) {
        fillRounded(x, y, w, h, radius, color, color, roundTop, roundBottom);
    }

    private void fillRounded(float x, float y, float w, float h, float radius, int top, int bottom,
                             boolean roundTop, boolean roundBottom) {
        int ix = Math.round(x), iy = Math.round(y);
        int iw = Math.round(x + w) - ix, ih = Math.round(y + h) - iy;
        if (iw <= 0 || ih <= 0) return;
        top = c(top);
        bottom = c(bottom);
        if (ColorUtil.alpha(top) == 0 && ColorUtil.alpha(bottom) == 0) return;
        boolean flat = top == bottom;
        float rad = clampRad(radius, iw, ih);
        if (rad < 0.5f || (!roundTop && !roundBottom)) {
            fillV(ix, iy, iw, ih, top, bottom, flat);
            return;
        }
        float[] tb = corner(rad);
        int rows = tb.length;
        boolean oldAa = antiAlias;
        if (rad <= 2.5f) antiAlias = false; // 2px corners: AA edge pixels cost 3x the quads and are barely visible
        int topEnd = roundTop ? Math.min(rows, ih) : 0;
        int botStart = roundBottom ? Math.max(topEnd, ih - rows) : ih;
        for (int j = 0; j < topEnd; j++) roundRow(ix, iy, iw, ih, j, tb[j], top, bottom, flat);
        for (int j = botStart; j < ih; j++) roundRow(ix, iy, iw, ih, j, tb[ih - 1 - j], top, bottom, flat);
        if (botStart > topEnd) {
            int midH = botStart - topEnd;
            if (flat) {
                r.fillRect(ix, iy + topEnd, iw, midH, top);
            } else {
                r.fillGradientV(ix, iy + topEnd, iw, midH,
                        ColorUtil.mix(top, bottom, (float) topEnd / ih),
                        ColorUtil.mix(top, bottom, (float) botStart / ih));
            }
        }
        antiAlias = oldAa;
    }

    /**
     * Rounded rectangle with a horizontal gradient. Drawn as full-height vertical strips for the
     * interior (one quad per strip) plus solid end-colour corner caps, instead of one quad per row per
     * segment as before (a 10-theme grid cost ~2000 quads per frame).
     */
    public void roundRectH(float x, float y, float w, float h, float radius, int left, int right) {
        int ix = Math.round(x), iy = Math.round(y);
        int iw = Math.round(x + w) - ix, ih = Math.round(y + h) - iy;
        if (iw <= 0 || ih <= 0) return;
        left = c(left);
        right = c(right);
        if (ColorUtil.alpha(left) == 0 && ColorUtil.alpha(right) == 0) return;
        float rad = clampRad(radius, iw, ih);
        int rc = rad < 0.5f ? 0 : (int) Math.ceil(rad);
        if (iw <= 2 * rc + 1) {
            int mid = ColorUtil.mix(left, right, 0.5f);
            fillRounded(ix, iy, iw, ih, rad, mid, mid, true, true);
            return;
        }
        if (rc > 0) {
            float[] tb = corner(rad);
            int rows = Math.min(tb.length, ih);
            int lc = ColorUtil.mix(left, right, rc * 0.5f / iw);
            int rcCol = ColorUtil.mix(left, right, 1f - rc * 0.5f / iw);
            int botStart = Math.max(rows, ih - tb.length);
            for (int j = 0; j < rows; j++) capRow(ix, iy, iw, rc, j, tb[j], lc, rcCol);
            for (int j = botStart; j < ih; j++) capRow(ix, iy, iw, rc, j, tb[ih - 1 - j], lc, rcCol);
            if (botStart > rows) {
                r.fillRect(ix, iy + rows, rc, botStart - rows, lc);
                r.fillRect(ix + iw - rc, iy + rows, rc, botStart - rows, rcCol);
            }
        }
        int innerW = iw - 2 * rc;
        int segs = MathUtil.clamp(innerW / 3, 1, 12);
        int prev = ix + rc;
        for (int s = 1; s <= segs; s++) {
            int next = s == segs ? ix + iw - rc : ix + rc + Math.round(innerW * (float) s / segs);
            if (next > prev) {
                float t = ((prev + next) * 0.5f - ix) / iw;
                r.fillRect(prev, iy, next - prev, ih, ColorUtil.mix(left, right, t));
            }
            prev = next;
        }
    }

    private void capRow(int ix, int iy, int iw, int rc, int j, float ins, int leftCol, int rightCol) {
        span(iy + j, ix + ins, ix + rc, leftCol, true, false);
        span(iy + j, ix + iw - rc, ix + iw - ins, rightCol, false, true);
    }

    /** Anti-aliased rounded outline, {@code t} pixels thick, drawn inside the given bounds. */
    public void roundBorder(float x, float y, float w, float h, float radius, int t, int color) {
        int ix = Math.round(x), iy = Math.round(y);
        int iw = Math.round(x + w) - ix, ih = Math.round(y + h) - iy;
        if (iw <= 0 || ih <= 0 || t <= 0) return;
        color = c(color);
        if (ColorUtil.alpha(color) == 0) return;
        t = Math.min(t, Math.min(iw, ih) / 2);
        if (t <= 0) return;
        float rad = MathUtil.clamp(radius, 0f, Math.min(iw, ih) / 2f);
        float radIn = Math.max(0f, rad - t);
        int innerW = iw - 2 * t;
        int innerH = ih - 2 * t;
        int rows = Math.max((int) Math.ceil(rad), t);
        int botStart = Math.max(rows, ih - rows);
        for (int j = 0; j < rows && j < ih; j++) borderRow(ix, iy, iw, ih, j, rad, radIn, t, innerW, innerH, color);
        for (int j = botStart; j < ih; j++) borderRow(ix, iy, iw, ih, j, rad, radIn, t, innerW, innerH, color);
        if (botStart > rows) {
            int midH = botStart - rows;
            r.fillRect(ix, iy + rows, t, midH, color);
            r.fillRect(ix + iw - t, iy + rows, t, midH, color);
        }
    }

    /**
     * Soft shadow made of fading outlines around the shape. Only draws outside the bounds, so it
     * stays clean under translucent (glass) surfaces.
     */
    public void shadow(float x, float y, float w, float h, float radius, int spread, int color) {
        if (ColorUtil.alpha(color) < 3) return;
        boolean old = antiAlias;
        antiAlias = false;
        spread = Math.min(spread, 2);
        for (int i = 1; i <= spread; i++) {
            float f = 1f - (i - 1f) / spread;
            f *= f;
            roundBorder(x - i, y - i, w + 2 * i, h + 2 * i, radius + i, 1, ColorUtil.mulAlpha(color, f * 0.6f));
        }
        antiAlias = old;
    }

    public void circle(float cx, float cy, float radius, int color) {
        roundRect(cx - radius, cy - radius, radius * 2f, radius * 2f, radius, color);
    }

    // ------------------------------------------------------------ text

    public int fontHeight() { return r.fontHeight(); }

    public float textWidth(String s) { return r.textWidth(s); }

    public float textWidth(String s, UiFont f) { return r.textWidth(s, f); }

    public void text(String s, float x, float y, int color) {
        color = tc(color);
        if (ColorUtil.alpha(color) < 4 || s.isEmpty()) return;
        r.drawText(s, Math.round(x), Math.round(y), color, false);
    }

    public void textShadow(String s, float x, float y, int color) {
        color = tc(color);
        if (ColorUtil.alpha(color) < 4 || s.isEmpty()) return;
        r.drawText(s, Math.round(x), Math.round(y), color, true);
    }

    /** Vertically centers the text inside a box of the given height starting at y. */
    public void textMid(String s, float x, float y, float h, int color) {
        text(s, x, y + (h - 8f) / 2f, color);
    }

    public void textCenter(String s, float cx, float y, int color) {
        text(s, cx - textWidth(s) / 2f, y, color);
    }

    public void textRight(String s, float rx, float y, int color) {
        text(s, rx - textWidth(s), y, color);
    }

    public void textScaled(String s, float x, float y, float scale, int color, boolean shadow) {
        textScaled(s, x, y, scale, color, shadow, UiFont.INTER);
    }

    public void textScaled(String s, float x, float y, float scale, int color, boolean shadow, UiFont f) {
        color = tc(color);
        if (ColorUtil.alpha(color) < 4 || s.isEmpty()) return;
        r.pushTransform(x, y, scale, scale);
        r.drawText(s, 0, 0, color, shadow, f);
        r.popTransform();
    }

    /** Shortens the string with "..." so it fits into maxW pixels. Cached per (string, width). */
    public String fit(String s, float maxW) {
        int mw = (int) maxW;
        Fit f = fits.get(s);
        if (f != null && f.w == mw) return f.out;
        String out = fitSlow(s, mw);
        if (f == null) {
            if (fits.size() > 512) fits.clear();
            f = new Fit();
            fits.put(s, f);
        }
        f.w = mw;
        f.out = out;
        return out;
    }

    private String fitSlow(String s, float maxW) {
        if (textWidth(s) <= maxW) return s;
        String dots = "...";
        float dw = textWidth(dots);
        int end = s.length();
        while (end > 0 && textWidth(s.substring(0, end)) + dw > maxW) end--;
        return s.substring(0, end) + dots;
    }

    // ------------------------------------------------------------ icons

    public void bitmap(String[] rows, float x, float y, int scale, int color) {
        color = c(color);
        if (ColorUtil.alpha(color) == 0) return;
        int ix = Math.round(x), iy = Math.round(y);
        for (int row = 0; row < rows.length; row++) {
            String line = rows[row];
            int col = 0;
            while (col < line.length()) {
                if (line.charAt(col) != 'X') {
                    col++;
                    continue;
                }
                int start = col;
                while (col < line.length() && line.charAt(col) == 'X') col++;
                r.fillRect(ix + start * scale, iy + row * scale, (col - start) * scale, scale, color);
            }
        }
    }

    /** Category icon (PNG texture, tinted). */
    public void categoryIcon(dev.ramoza.client.module.Category c, float x, float y, int size, int color) {
        color = c(color);
        if (ColorUtil.alpha(color) == 0) return;
        r.drawTexture("category/" + Icons.textureName(c), Math.round(x), Math.round(y), size, color);
    }

    // ------------------------------------------------------------ internals

    private void fillV(int x, int y, int w, int h, int top, int bottom, boolean flat) {
        if (flat) r.fillRect(x, y, w, h, top);
        else r.fillGradientV(x, y, w, h, top, bottom);
    }

    private void roundRow(int ix, int iy, int iw, int ih, int j, float ins, int top, int bottom, boolean flat) {
        int col = flat ? top : ColorUtil.mix(top, bottom, (j + 0.5f) / ih);
        span(iy + j, ix + ins, ix + iw - ins, col, true, true);
    }

    private static float clampRad(float radius, int iw, int ih) {
        return MathUtil.clamp(Math.min(radius, 15f), 0f, Math.min(iw, ih) / 2f);
    }

    /** Horizontal edge inset per row of one rounded corner, for rows 0..ceil(rad)-1 (cached). */
    private static float[] corner(float rad) {
        int q = Math.min(CORNER.length - 1, Math.max(1, Math.round(rad * 4f)));
        float[] t = CORNER[q];
        if (t == null) {
            float rr = q / 4f;
            t = new float[(int) Math.ceil(rr)];
            for (int j = 0; j < t.length; j++) {
                float dy = rr - (j + 0.5f);
                t[j] = rr - (float) Math.sqrt(Math.max(0f, rr * rr - dy * dy));
            }
            CORNER[q] = t;
        }
        return t;
    }

    private static float cornerInset(float rad, int j, int h) {
        float[] tb = corner(rad);
        if (j < tb.length) return tb[j];
        int k = h - 1 - j;
        return k >= 0 && k < tb.length ? tb[k] : 0f;
    }

    private void borderRow(int ix, int iy, int iw, int ih, int j, float rad, float radIn, int t,
                           int innerW, int innerH, int color) {
        float insO = rad < 0.5f ? 0f : cornerInset(rad, j, ih);
        float oL = ix + insO;
        float oR = ix + iw - insO;
        boolean hasInner = innerW > 0 && innerH > 0 && j >= t && j < ih - t;
        if (!hasInner) {
            span(iy + j, oL, oR, color, true, true);
            return;
        }
        int jj = j - t;
        float insI = radIn < 0.5f ? 0f : cornerInset(radIn, jj, innerH);
        float iL = ix + t + insI;
        float iR = ix + iw - t - insI;
        span(iy + j, oL, iL, color, true, true);
        span(iy + j, iR, oR, color, true, true);
    }

    /** Horizontal inset of a rounded rect's edge at vertical position yc (0..h). */
    private static float inset(float rad, float yc, float h) {
        float dy;
        if (yc < rad) dy = rad - yc;
        else if (yc > h - rad) dy = yc - (h - rad);
        else return 0f;
        float d = rad * rad - dy * dy;
        return rad - (float) Math.sqrt(Math.max(0f, d));
    }

    /** One-pixel-high horizontal span with fractional (anti-aliased) end pixels. */
    private void span(int y, float xs, float xe, int color, boolean aaLeft, boolean aaRight) {
        if (xe <= xs) return;
        int x0 = (int) Math.ceil(xs);
        int x1 = (int) Math.floor(xe);
        if (x1 < x0) {
            if (antiAlias) r.fillRect((int) Math.floor(xs), y, 1, 1, ColorUtil.mulAlpha(color, xe - xs));
            return;
        }
        if (x1 > x0) r.fillRect(x0, y, x1 - x0, 1, color);
        if (!antiAlias) return;
        float la = x0 - xs;
        if (aaLeft && la > 0.03f) r.fillRect(x0 - 1, y, 1, 1, ColorUtil.mulAlpha(color, la));
        float ra = xe - x1;
        if (aaRight && ra > 0.03f) r.fillRect(x1, y, 1, 1, ColorUtil.mulAlpha(color, ra));
    }
}
