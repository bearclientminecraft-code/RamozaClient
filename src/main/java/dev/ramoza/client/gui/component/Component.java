package dev.ramoza.client.gui.component;

import dev.ramoza.client.gui.UiContext;
import dev.ramoza.client.gui.anim.SmoothFloat;

/** Base for everything that has bounds, hover/press animation and mouse handling. */
public abstract class Component {
    public float x;
    public float y;
    public float w;
    public float h;

    private float clipL = -1e7f, clipT = -1e7f, clipR = 1e7f, clipB = 1e7f;
    protected final SmoothFloat hover = new SmoothFloat(0f, 18f);
    protected final SmoothFloat press = new SmoothFloat(0f, 30f);
    protected boolean hoveredNow;

    public void setBounds(float x, float y, float w, float h) {
        this.x = x;
        this.y = y;
        this.w = w;
        this.h = h;
    }

    /** Call at the start of rendering: snapshots the clip rect and updates hover/press animation. */
    protected void begin(UiContext ctx) {
        clipL = ctx.clipL();
        clipT = ctx.clipT();
        clipR = ctx.clipR();
        clipB = ctx.clipB();
        hoveredNow = !ctx.hoverBlocked && hit(ctx.mx, ctx.my);
        hover.setTarget(hoveredNow ? 1f : 0f);
        hover.update(ctx.dt);
        press.update(ctx.dt);
    }

    /** True if the point is inside the bounds AND inside the visible (clipped) area. */
    public boolean hit(double mx, double my) {
        return mx >= x && mx < x + w && my >= y && my < y + h
                && mx >= clipL && mx < clipR && my >= clipT && my < clipB;
    }

    public boolean mouseClicked(UiContext ctx, double mx, double my, int button) { return false; }

    public void mouseDragged(UiContext ctx, double mx, double my) {}

    public void mouseReleased(UiContext ctx, double mx, double my, int button) {}

    public void setPressed(boolean pressed) { press.setTarget(pressed ? 1f : 0f); }
}
