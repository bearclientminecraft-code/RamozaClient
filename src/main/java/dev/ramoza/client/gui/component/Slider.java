package dev.ramoza.client.gui.component;

import dev.ramoza.client.gui.Metrics;
import dev.ramoza.client.gui.UiContext;
import dev.ramoza.client.gui.anim.SmoothFloat;
import dev.ramoza.client.gui.render.Gfx;
import dev.ramoza.client.gui.theme.Palette;
import dev.ramoza.client.util.ColorUtil;
import dev.ramoza.client.util.MathUtil;

import java.util.function.DoubleConsumer;
import java.util.function.DoubleFunction;
import java.util.function.DoubleSupplier;

public final class Slider extends Control {
    private static final float TRACK_H = 4f;

    private final DoubleSupplier getter;
    private final DoubleConsumer setter;
    private final double min;
    private final double max;
    private final double step;
    private final DoubleFunction<String> format;
    private final SmoothFloat shown = new SmoothFloat(-1f, 22f);
    private boolean dragging;
    private float lastPad = 6f;

    public Slider(String label, String tip, DoubleSupplier getter, DoubleConsumer setter,
                  double min, double max, double step, DoubleFunction<String> format) {
        super(label, tip);
        this.getter = getter;
        this.setter = setter;
        this.min = min;
        this.max = max;
        this.step = step;
        this.format = format;
    }

    @Override public float preferredHeight(Metrics m) { return m.sliderH; }

    private float fraction() {
        double range = max - min;
        return range <= 0 ? 0f : (float) MathUtil.clamp((getter.getAsDouble() - min) / range, 0.0, 1.0);
    }

    @Override
    public void render(UiContext ctx) {
        frame(ctx);
        Gfx g = ctx.g;
        Palette p = ctx.p;
        Metrics m = ctx.m;
        lastPad = m.pad;

        float frac = fraction();
        if (shown.get() < 0f) shown.snap(frac);
        shown.setTarget(frac);
        shown.update(ctx.dt);

        float hv = hover.get();
        if (hv > 0.01f) g.roundRect(x, y, w, h, m.radiusSmall, ColorUtil.mulAlpha(p.hover(), hv * 0.7f));

        float act = Math.max(hv * 0.6f, press.get());
        g.text(label, x + m.pad, y + 2f, ColorUtil.mix(p.muted(), p.text(), act));
        g.textRight(format.apply(getter.getAsDouble()), x + w - m.pad, y + 2f, ColorUtil.mix(p.muted(), p.primary(), Math.max(act, 0.5f)));

        float tx = x + m.pad;
        float tw = w - 2f * m.pad;
        float ty = y + h - 8f;
        g.roundRect(tx, ty, tw, TRACK_H, TRACK_H / 2f, p.sliderTrack());
        float fillW = tw * MathUtil.saturate(shown.get());
        if (fillW >= 1f) g.roundRectH(tx, ty, fillW, TRACK_H, TRACK_H / 2f, p.sliderFill(), p.secondary());

        float kr = 3.5f + hv * 0.8f + press.get() * 0.7f;
        float kcx = tx + fillW;
        float kcy = ty + TRACK_H / 2f;
        g.circle(kcx, kcy, kr + 1f, ColorUtil.withAlpha(ColorUtil.BLACK, 0x50));
        g.circle(kcx, kcy, kr, p.sliderKnob());
        drawFocus(ctx);
    }

    private void setFromMouse(double mx) {
        double t = MathUtil.clamp((mx - (x + lastPad)) / (w - 2.0 * lastPad), 0.0, 1.0);
        apply(min + t * (max - min));
    }

    private void apply(double v) {
        double c = MathUtil.clamp(v, min, max);
        if (step > 0) c = MathUtil.clamp(Math.round((c - min) / step) * step + min, min, max);
        setter.accept(c);
    }

    @Override
    public boolean mouseClicked(UiContext ctx, double mx, double my, int button) {
        if (button != 0 || !hit(mx, my)) return false;
        dragging = true;
        ctx.dragTarget = this;
        ctx.pressTarget = this;
        setPressed(true);
        ctx.focus.set(this);
        setFromMouse(mx);
        return true;
    }

    @Override
    public void mouseDragged(UiContext ctx, double mx, double my) {
        if (dragging) setFromMouse(mx);
    }

    @Override
    public void mouseReleased(UiContext ctx, double mx, double my, int button) {
        dragging = false;
    }

    @Override
    public boolean onHorizontal(UiContext ctx, int dir) {
        double s = step > 0 ? step : (max - min) / 20.0;
        apply(getter.getAsDouble() + dir * s);
        return true;
    }
}
