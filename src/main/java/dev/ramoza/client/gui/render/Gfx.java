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
    /** Edge anti-aliasing for rounded shapes. Switched off internally for soft shadows (cheap + invisible). */
    private boolean antiAlias = true;
    private final float[] alphaStack = new float[24];
    private int alphaTop = 0;

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
        roundRectV(x, y, w, h, radius, color, color);
    }

    /** Rounded rectangle with a vertical gradient. */
    public void roundRectV(float x, float y, float w, float h, float radius, int top, int bottom) {
        int ix = Math.round(x), iy = Math.round(y);
        int iw = Math.round(x + w) - ix, ih = Math.round(y + h) - iy;
        if (iw <= 0 || ih <= 0) return;
        top = c(top);
        bottom = c(bottom);
        if (ColorUtil.alpha(top) == 0 && ColorUtil.alpha(bottom) == 0) return;
        boolean flat = top == bottom;
        float rad = MathUtil.clamp(radius, 0f, Math.min(iw, ih) / 2f);
        if (rad < 0.5f) {
            fillV(ix, iy, iw, ih, top, bottom, flat);
            return;
        }
        int rows = (int) Math.ceil(rad);
        int botStart = Math.max(rows, ih - rows);
        for (int j = 0; j < rows && j < ih; j++) roundRow(ix, iy, iw, ih, j, rad, top, bottom, flat);
        for (int j = botStart; j < ih; j++) roundRow(ix, iy, iw, ih, j, rad, top, bottom, flat);
        if (botStart > rows) {
            int midH = botStart - rows;
            if (flat) {
                r.fillRect(ix, iy + rows, iw, midH, top);
            } else {
                r.fillGradientV(ix, iy + rows, iw, midH,
                        ColorUtil.mix(top, bottom, (float) rows / ih),
                        ColorUtil.mix(top, bottom, (float) botStart / ih));
            }
        }
    }

    /** Rounded rectangle with a horizontal gradient (small heights only, e.g. slider fills). */
    public void roundRectH(float x, float y, float w, float h, float radius, int left, int right) {
        int ix = Math.round(x), iy = Math.round(y);
        int iw = Math.round(x + w) - ix, ih = Math.round(y + h) - iy;
        if (iw <= 0 || ih <= 0) return;
        left = c(left);
        right = c(right);
        float rad = MathUtil.clamp(radius, 0f, Math.min(iw, ih) / 2f);
        int segs = MathUtil.clamp(iw / 3, 1, 10);
        for (int j = 0; j < ih; j++) {
            float ins = rad < 0.5f ? 0f : inset(rad, j + 0.5f, ih);
            float xs = ix + ins;
            float xe = ix + iw - ins;
            float step = (xe - xs) / segs;
            for (int s = 0; s < segs; s++) {
                float a = xs + step * s;
                float b = s == segs - 1 ? xe : xs + step * (s + 1);
                if (s > 0) a = Math.round(a);
                if (s < segs - 1) b = Math.round(b);
                float t = ((a + b) * 0.5f - ix) / iw;
                span(iy + j, a, b, ColorUtil.mix(left, right, t), s == 0, s == segs - 1);
            }
        }
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

    public void text(String s, float x, float y, int color) {
        color = c(color);
        if (ColorUtil.alpha(color) < 4 || s.isEmpty()) return;
        r.drawText(s, Math.round(x), Math.round(y), color, false);
    }

    public void textShadow(String s, float x, float y, int color) {
        color = c(color);
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
        color = c(color);
        if (ColorUtil.alpha(color) < 4 || s.isEmpty()) return;
        r.pushTransform(x, y, scale, scale);
        r.drawText(s, 0, 0, color, shadow);
        r.popTransform();
    }

    /** Shortens the string with "..." so it fits into maxW pixels. */
    public String fit(String s, float maxW) {
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

    // ------------------------------------------------------------ internals

    private void fillV(int x, int y, int w, int h, int top, int bottom, boolean flat) {
        if (flat) r.fillRect(x, y, w, h, top);
        else r.fillGradientV(x, y, w, h, top, bottom);
    }

    private void roundRow(int ix, int iy, int iw, int ih, int j, float rad, int top, int bottom, boolean flat) {
        float ins = inset(rad, j + 0.5f, ih);
        int col = flat ? top : ColorUtil.mix(top, bottom, (j + 0.5f) / ih);
        span(iy + j, ix + ins, ix + iw - ins, col, true, true);
    }

    private void borderRow(int ix, int iy, int iw, int ih, int j, float rad, float radIn, int t,
                           int innerW, int innerH, int color) {
        float insO = rad < 0.5f ? 0f : inset(rad, j + 0.5f, ih);
        float oL = ix + insO;
        float oR = ix + iw - insO;
        boolean hasInner = innerW > 0 && innerH > 0 && j >= t && j < ih - t;
        if (!hasInner) {
            span(iy + j, oL, oR, color, true, true);
            return;
        }
        int jj = j - t;
        float insI = radIn < 0.5f ? 0f : inset(radIn, jj + 0.5f, innerH);
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
