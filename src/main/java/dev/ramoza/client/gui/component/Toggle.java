package dev.ramoza.client.gui.component;

import dev.ramoza.client.gui.Metrics;
import dev.ramoza.client.gui.UiContext;
import dev.ramoza.client.gui.anim.SmoothFloat;
import dev.ramoza.client.gui.render.Gfx;
import dev.ramoza.client.gui.render.Icons;
import dev.ramoza.client.gui.theme.Palette;
import dev.ramoza.client.util.ColorUtil;

import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

/** Boolean setting: label + a small rounded checkbox (no oval switch). */
public final class Toggle extends Control {
    public static final float BOX = 9f;
    private static final float BOX_RADIUS = 2f;

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
        float t = state.get();
        if (hv > 0.01f) g.roundRect(x, y, w, h, m.radiusSmall, ColorUtil.mulAlpha(p.hover(), hv));
        g.textMid(label, x + m.pad, y, h, ColorUtil.mix(p.muted(), p.text(), Math.max(t * 0.7f, hv)));

        float bx = x + w - m.pad - BOX;
        float by = y + (h - BOX) / 2f;
        g.roundRect(bx, by, BOX, BOX, BOX_RADIUS, ColorUtil.mix(p.field(), p.primary(), t));
        if (t < 0.98f) {
            g.roundBorder(bx, by, BOX, BOX, BOX_RADIUS, 1, ColorUtil.mulAlpha(ColorUtil.mix(p.dim(), p.primary(), t), 1f - t * 0.5f));
        }
        if (t > 0.05f) g.bitmap(Icons.CHECK, bx + 2f, by + 3f, 1, ColorUtil.mulAlpha(ColorUtil.WHITE, t));
        drawFocus(ctx);
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
