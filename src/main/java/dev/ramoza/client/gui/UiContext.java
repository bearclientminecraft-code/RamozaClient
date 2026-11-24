package dev.ramoza.client.gui;

import dev.ramoza.client.config.GuiSettings;
import dev.ramoza.client.gui.component.Component;
import dev.ramoza.client.gui.component.Tooltip;
import dev.ramoza.client.gui.render.Gfx;
import dev.ramoza.client.gui.theme.Palette;
import dev.ramoza.client.gui.theme.ThemeManager;

/** Per-frame shared state handed to every component. */
public final class UiContext {
    private static final float INF = 1e7f;
    private static final int MAX_CLIPS = 24;

    public final Gfx g;
    public final GuiSettings settings;
    public final ThemeManager themes;
    public final FocusManager focus = new FocusManager();
    public final Tooltip tooltip = new Tooltip();

    public Palette p;
    public Metrics m;
    /** Animation delta time in seconds (already scaled by the Animation Speed setting). */
    public float dt;
    /** Real elapsed time in seconds (for caret blinking). */
    public float time;
    /** Mouse position in virtual GUI coordinates. */
    public double mx;
    public double my;
    /** When true, components do not react to hover (a popup is above them). */
    public boolean hoverBlocked;
    public Component dragTarget;
    public Component pressTarget;

    private float cl = -INF, ct = -INF, cr = INF, cb = INF;
    private final float[] stack = new float[MAX_CLIPS * 4];
    private int top;

    public UiContext(Gfx g, GuiSettings settings, ThemeManager themes) {
        this.g = g;
        this.settings = settings;
        this.themes = themes;
        this.p = themes.palette();
        this.m = new Metrics(settings, 0f);
    }

    public void resetClip() {
        top = 0;
        cl = ct = -INF;
        cr = cb = INF;
    }

    public void pushClip(float x, float y, float w, float h) {
        if (top < MAX_CLIPS) {
            int i = top * 4;
            stack[i] = cl;
            stack[i + 1] = ct;
            stack[i + 2] = cr;
            stack[i + 3] = cb;
        }
        top++;
        cl = Math.max(cl, x);
        ct = Math.max(ct, y);
        cr = Math.min(cr, x + w);
        cb = Math.min(cb, y + h);
    }

    public void popClip() {
        top--;
        if (top < 0) {
            top = 0;
            return;
        }
        if (top < MAX_CLIPS) {
            int i = top * 4;
            cl = stack[i];
            ct = stack[i + 1];
            cr = stack[i + 2];
            cb = stack[i + 3];
        }
    }

    public float clipL() { return cl; }
    public float clipT() { return ct; }
    public float clipR() { return cr; }
    public float clipB() { return cb; }
}
