package dev.ramoza.client.gui.component;

import dev.ramoza.client.gui.Focusable;
import dev.ramoza.client.gui.Metrics;
import dev.ramoza.client.gui.UiContext;
import dev.ramoza.client.gui.anim.SmoothFloat;
import dev.ramoza.client.util.ColorUtil;

/** A labelled, focusable control row (toggle, slider, dropdown, keybind...). */
public abstract class Control extends Component implements Focusable {
    protected final String label;
    protected final String tip;
    protected boolean focused;
    protected final SmoothFloat focusAnim = new SmoothFloat(0f, 18f);

    protected Control(String label, String tip) {
        this.label = label;
        this.tip = tip;
    }

    public abstract float preferredHeight(Metrics m);

    public abstract void render(UiContext ctx);

    /** Standard per-frame bookkeeping; call first in {@link #render}. */
    protected void frame(UiContext ctx) {
        begin(ctx);
        focusAnim.setTarget(focused ? 1f : 0f);
        focusAnim.update(ctx.dt);
        if (hoveredNow && ctx.settings.showTooltips) ctx.tooltip.request(this, tip, ctx.mx, ctx.my);
    }

    protected void drawFocus(UiContext ctx) {
        float f = focusAnim.get();
        if (f < 0.02f) return;
        ctx.g.roundBorder(fx(), fy(), fw(), fh(), ctx.m.radiusSmall, 1, ColorUtil.mulAlpha(ctx.p.primary(), f));
    }

    @Override public float fx() { return x; }
    @Override public float fy() { return y; }
    @Override public float fw() { return w; }
    @Override public float fh() { return h; }
    @Override public void setFocused(boolean f) { focused = f; }
    @Override public boolean isFocused() { return focused; }
}
