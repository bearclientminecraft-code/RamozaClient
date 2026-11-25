package dev.ramoza.client.gui.component;

import dev.ramoza.client.gui.UiContext;
import dev.ramoza.client.gui.anim.SmoothFloat;
import dev.ramoza.client.gui.render.Gfx;
import dev.ramoza.client.util.ColorUtil;
import dev.ramoza.client.util.MathUtil;

/** Delayed, fading tooltip. Components call {@link #request} every frame while hovered. */
public final class Tooltip {
    private static final float DELAY = 0.45f;

    private Object key;
    private String text = "";
    private float timer;
    private boolean requested;
    private float ax;
    private float ay;
    private final SmoothFloat alpha = new SmoothFloat(0f, 18f);

    public void beginFrame() { requested = false; }

    public void request(Object owner, String t, double mx, double my) {
        if (t == null || t.isEmpty()) return;
        if (owner != key) {
            key = owner;
            timer = 0f;
        }
        text = t;
        ax = (float) mx;
        ay = (float) my;
        requested = true;
    }

    public void update(float dt, boolean enabled) {
        if (requested && enabled) {
            timer += dt;
        } else {
            timer = 0f;
            if (!requested) key = null;
        }
        alpha.setTarget(timer > DELAY ? 1f : 0f);
        alpha.update(dt);
    }

    public void render(UiContext ctx, float vw, float vh) {
        float a = alpha.get();
        if (a < 0.01f || text.isEmpty()) return;
        Gfx g = ctx.g;
        float w = g.textWidth(text) + 12f;
        float h = 16f;
        float x = MathUtil.clamp(ax + 10f, 4f, Math.max(4f, vw - w - 4f));
        float y = MathUtil.clamp(ay + 14f + (1f - a) * 3f, 4f, Math.max(4f, vh - h - 4f));
        g.pushAlpha(a);
        g.roundRect(x, y, w, h, ctx.m.radiusSmall + 1f, ctx.p.tooltip());
        g.roundBorder(x, y, w, h, ctx.m.radiusSmall + 1f, 1, ColorUtil.withAlpha(ColorUtil.WHITE, 0x30));
        g.text(text, x + 6f, y + 4f, 0xFFF2F2F7);
        g.popAlpha();
    }
}
