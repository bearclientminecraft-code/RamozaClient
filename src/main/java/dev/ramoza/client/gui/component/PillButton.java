package dev.ramoza.client.gui.component;

import dev.ramoza.client.gui.Metrics;
import dev.ramoza.client.gui.UiContext;
import dev.ramoza.client.gui.anim.SmoothFloat;
import dev.ramoza.client.gui.render.Gfx;
import dev.ramoza.client.gui.render.Icons;
import dev.ramoza.client.gui.theme.Palette;
import dev.ramoza.client.util.ColorUtil;

import java.util.function.IntConsumer;

/** Compact icon + text button used in the bottom bar and in dialogs. */
public final class PillButton extends Control {
    private static final int DANGER = 0xFFFF5470;

    private final String[] icon;
    private final String widestText;
    private final Runnable action;
    private String text;
    private boolean danger;
    private IntConsumer onArrow;
    private final SmoothFloat lit = new SmoothFloat(0f, 16f);
    private boolean highlighted;

    public PillButton(String[] icon, String text, String tip, Runnable action) {
        this(icon, text, text, tip, action);
    }

    public PillButton(String[] icon, String text, String widestText, String tip, Runnable action) {
        super(text, tip);
        this.icon = icon;
        this.text = text;
        this.widestText = widestText;
        this.action = action;
    }

    public void setText(String t) { this.text = t; }
    public void setDanger(boolean d) { this.danger = d; }
    public void setHighlighted(boolean v) { this.highlighted = v; }
    public void setOnArrow(IntConsumer c) { this.onArrow = c; }

    public float preferredWidth(Gfx g, Metrics m) {
        return m.pad + Icons.width(icon) + 4f + g.textWidth(widestText) + m.pad;
    }

    @Override public float preferredHeight(Metrics m) { return 20f; }

    @Override
    public void render(UiContext ctx) {
        frame(ctx);
        lit.setTarget(highlighted ? 1f : 0f);
        lit.update(ctx.dt);
        Gfx g = ctx.g;
        Palette p = ctx.p;
        Metrics m = ctx.m;
        float hv = hover.get();
        float r = Math.min(h / 2f, m.radius + 1f);
        int accent = danger ? DANGER : p.primary();

        if (lit.get() > 0.01f) g.roundRect(x, y, w, h, r, ColorUtil.mulAlpha(p.active(), lit.get()));
        if (hv > 0.01f) g.roundRect(x, y, w, h, r, ColorUtil.mulAlpha(danger ? ColorUtil.withAlpha(DANGER, 0x26) : p.hover(), hv));
        if (press.get() > 0.01f) g.roundRect(x, y, w, h, r, ColorUtil.withAlpha(ColorUtil.BLACK, Math.round(0x30 * press.get())));

        float k = Math.max(hv, lit.get());
        int col = ColorUtil.mix(p.muted(), danger ? DANGER : p.text(), k);
        int iconCol = ColorUtil.mix(p.muted(), accent, k);
        float ix = x + m.pad;
        float iy = y + (h - Icons.height(icon)) / 2f;
        g.bitmap(icon, ix, iy, 1, iconCol);
        g.text(text, ix + Icons.width(icon) + 4f, y + (h - 8f) / 2f, col);

        if (focusAnim.get() > 0.02f) g.roundBorder(x, y, w, h, r, 1, ColorUtil.mulAlpha(accent, focusAnim.get()));
    }

    @Override
    public boolean mouseClicked(UiContext ctx, double mx, double my, int button) {
        if (button != 0 || !hit(mx, my)) return false;
        ctx.focus.set(this);
        setPressed(true);
        ctx.pressTarget = this;
        action.run();
        return true;
    }

    @Override
    public boolean onEnter(UiContext ctx) {
        action.run();
        return true;
    }

    @Override
    public boolean onHorizontal(UiContext ctx, int dir) {
        if (onArrow == null) return false;
        onArrow.accept(dir);
        return true;
    }
}
