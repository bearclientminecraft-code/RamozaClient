package dev.ramoza.client.gui.component;

import dev.ramoza.client.gui.Focusable;
import dev.ramoza.client.gui.UiContext;
import dev.ramoza.client.gui.anim.SmoothFloat;
import dev.ramoza.client.gui.render.Gfx;
import dev.ramoza.client.gui.render.Icons;
import dev.ramoza.client.gui.theme.Palette;
import dev.ramoza.client.util.ColorUtil;

public final class SearchBar extends Component implements Focusable {
    private static final int MAX_LENGTH = 28;
    private static final float CLEAR_ZONE = 16f;

    private final StringBuilder text = new StringBuilder();
    private boolean focused;
    private final SmoothFloat focusAnim = new SmoothFloat(0f, 16f);
    private final SmoothFloat clearAnim = new SmoothFloat(0f, 18f);

    public String query() { return text.toString(); }

    public void clear() { text.setLength(0); }

    public void type(int codepoint) {
        if (codepoint < 32 || codepoint == 127 || text.length() >= MAX_LENGTH) return;
        text.appendCodePoint(codepoint);
    }

    public void backspace(boolean all) {
        if (all) text.setLength(0);
        else if (text.length() > 0) text.setLength(text.length() - 1);
    }

    public void render(UiContext ctx) {
        begin(ctx);
        Gfx g = ctx.g;
        Palette p = ctx.p;
        focusAnim.setTarget(focused ? 1f : 0f);
        focusAnim.update(ctx.dt);
        boolean has = text.length() > 0;
        clearAnim.setTarget(has ? 1f : 0f);
        clearAnim.update(ctx.dt);

        float r = Math.min(h / 2f, ctx.m.radiusSmall + 2f);
        float f = focusAnim.get();
        g.roundRect(x, y, w, h, r, p.field());
        g.roundBorder(x, y, w, h, r, 1,
                ColorUtil.mix(ColorUtil.mulAlpha(p.border(), 0.9f), p.primary(), Math.max(f, hover.get() * 0.4f)));

        int iconCol = ColorUtil.mix(p.muted(), p.primary(), f);
        g.bitmap(Icons.SEARCH, x + 8f, y + (h - 9f) / 2f, 1, iconCol);

        float tx = x + 22f;
        float maxW = w - 22f - CLEAR_ZONE - 4f;
        if (has) {
            g.text(g.fit(text.toString(), maxW), tx, y + (h - 8f) / 2f, p.text());
        } else {
            g.text("Search modules...", tx, y + (h - 8f) / 2f, p.dim());
        }
        if (focused && ((int) (ctx.time * 2f) % 2 == 0)) {
            float cx = has ? tx + Math.min(g.textWidth(text.toString()), maxW) + 1f : tx - 1f;
            g.rect(cx, y + 4f, 1f, h - 8f, p.text());
        }

        float ca = clearAnim.get();
        if (ca > 0.01f) {
            float cx = x + w - CLEAR_ZONE + 2f;
            boolean overClear = hoveredNow && ctx.mx >= x + w - CLEAR_ZONE;
            g.roundRect(cx - 1.5f, y + h / 2f - 5.5f, 11f, 11f, 3f, ColorUtil.mulAlpha(p.hover(), ca * (overClear ? 2f : 1f)));
            g.bitmap(Icons.CLOSE, cx + 1.5f, y + (h - 5f) / 2f, 1, ColorUtil.mulAlpha(overClear ? p.text() : p.muted(), ca));
        }
    }

    @Override
    public boolean mouseClicked(UiContext ctx, double mx, double my, int button) {
        if (button != 0 || !hit(mx, my)) return false;
        if (mx >= x + w - CLEAR_ZONE && text.length() > 0) clear();
        ctx.focus.set(this);
        return true;
    }

    @Override public float fx() { return x; }
    @Override public float fy() { return y; }
    @Override public float fw() { return w; }
    @Override public float fh() { return h; }
    @Override public void setFocused(boolean f) { focused = f; }
    @Override public boolean isFocused() { return focused; }
}
