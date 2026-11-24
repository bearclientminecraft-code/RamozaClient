package dev.ramoza.client.gui.component;

import dev.ramoza.client.gui.Metrics;
import dev.ramoza.client.gui.UiContext;
import dev.ramoza.client.gui.anim.SmoothFloat;
import dev.ramoza.client.gui.render.Gfx;
import dev.ramoza.client.gui.theme.Palette;
import dev.ramoza.client.util.ColorUtil;

import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

public final class Toggle extends Control {
    public static final float PILL_W = 18f;
    public static final float PILL_H = 9f;

    private final BooleanSupplier getter;
    private final Consumer<Boolean> setter;
    private final SmoothFloat state;

    public Toggle(String label, String tip, BooleanSupplier getter, Consumer<Boolean> setter) {
        super(label, tip);
        this.getter = getter;
        this.setter = setter;
        this.state = new SmoothFloat(getter.getAsBoolean() ? 1f : 0f, 16f);
    }

    @Override public float preferredHeight(Metrics m) { return m.controlH; }

    @Override
    public void render(UiContext ctx) {
        frame(ctx);
        state.setTarget(getter.getAsBoolean() ? 1f : 0f);
        state.update(ctx.dt);
        Gfx g = ctx.g;
        Palette p = ctx.p;
        Metrics m = ctx.m;
        float hv = hover.get();
        if (hv > 0.01f) g.roundRect(x, y, w, h, m.radiusSmall, ColorUtil.mulAlpha(p.hover(), hv));
        g.textMid(label, x + m.pad, y, h, ColorUtil.mix(p.muted(), p.text(), Math.max(state.get() * 0.7f, hv)));
        drawPill(ctx, x + w - m.pad - PILL_W, y + (h - PILL_H) / 2f, state.get());
        drawFocus(ctx);
    }

    /** Shared pill switch drawing (also used by module rows). t = 0 off, 1 on. */
    public static void drawPill(UiContext ctx, float px, float py, float t) {
        Gfx g = ctx.g;
        Palette p = ctx.p;
        g.roundRect(px, py, PILL_W, PILL_H, PILL_H / 2f, ColorUtil.mix(p.toggleOff(), p.toggleOn(), t));
        float kd = PILL_H - 2f;
        float kx = px + 1f + t * (PILL_W - PILL_H);
        g.roundRect(kx, py + 1f, kd, kd, kd / 2f, p.toggleKnob());
    }

    @Override
    public boolean mouseClicked(UiContext ctx, double mx, double my, int button) {
        if (button != 0 || !hit(mx, my)) return false;
        setter.accept(!getter.getAsBoolean());
        ctx.focus.set(this);
        setPressed(true);
        ctx.pressTarget = this;
        return true;
    }

    @Override
    public boolean onEnter(UiContext ctx) {
        setter.accept(!getter.getAsBoolean());
        return true;
    }

    @Override
    public boolean onHorizontal(UiContext ctx, int dir) {
        setter.accept(dir > 0);
        return true;
    }
}
