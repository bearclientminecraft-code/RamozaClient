package dev.ramoza.client.gui.component;

import dev.ramoza.client.config.GuiSettings;
import dev.ramoza.client.gui.FocusManager;
import dev.ramoza.client.gui.Focusable;
import dev.ramoza.client.gui.Metrics;
import dev.ramoza.client.gui.UiContext;
import dev.ramoza.client.gui.anim.SmoothFloat;
import dev.ramoza.client.gui.render.Gfx;
import dev.ramoza.client.gui.render.Icons;
import dev.ramoza.client.gui.theme.Palette;
import dev.ramoza.client.util.ColorUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * The "Gui Settings" entry of the Misc panel. Right-click (or Enter / Right arrow) expands it:
 * Corner Rounding, Menu Opacity and Text Opacity, each 0-100.
 */
public final class GuiSettingsSection extends PanelEntry implements Focusable {
    private static final float CHILD_PAD = 2f;
    private static final String NAME = "Gui Settings";

    private final List<Control> controls = new ArrayList<>();
    private final SmoothFloat expand = new SmoothFloat(0f, 14f);
    private final SmoothFloat focusAnim = new SmoothFloat(0f, 18f);
    private boolean expanded;
    private boolean focused;

    public GuiSettingsSection(GuiSettings s) {
        controls.add(percent("Corner Rounding", "Corner roundness of the GUI (0-100)",
                () -> s.cornerRounding, v -> s.cornerRounding = (float) v));
        controls.add(percent("Menu Opacity", "Opacity of the menu panels (0-100)",
                () -> s.menuOpacity, v -> s.menuOpacity = (float) v));
        controls.add(percent("Text Opacity", "Opacity of the GUI text (0-100)",
                () -> s.textOpacity, v -> s.textOpacity = (float) v));
    }

    private static Slider percent(String label, String tip,
                                  java.util.function.DoubleSupplier get, java.util.function.DoubleConsumer set) {
        return new Slider(label, tip, get, set, GuiSettings.PERCENT_MIN, GuiSettings.PERCENT_MAX, 1,
                v -> Math.round(v) + "%");
    }

    @Override
    public boolean matches(String query) {
        return query.isEmpty() || NAME.toLowerCase(java.util.Locale.ROOT).contains(query);
    }

    private float childrenHeight(Metrics m) {
        float t = 0f;
        for (Control c : controls) t += c.preferredHeight(m);
        return t;
    }

    @Override
    public float contentHeight(UiContext ctx) {
        Metrics m = ctx.m;
        return m.rowH + expand.get() * (childrenHeight(m) + 2f * CHILD_PAD);
    }

    @Override
    public void renderEntry(UiContext ctx, float x, float y, float w, boolean draw) {
        Metrics m = ctx.m;
        Gfx g = ctx.g;
        Palette p = ctx.p;

        expand.setTarget(expanded ? 1f : 0f);
        expand.update(ctx.dt);
        focusAnim.setTarget(focused ? 1f : 0f);
        focusAnim.update(ctx.dt);
        setBounds(x, y, w, m.rowH);

        float childH = 0f;
        for (Control c : controls) {
            float ch = c.preferredHeight(m);
            c.setBounds(x + CHILD_PAD, y + m.rowH + CHILD_PAD + childH, w - 2f * CHILD_PAD, ch);
            childH += ch;
        }
        if (!draw) return;

        begin(ctx);
        float hv = hover.get();
        float ex = expand.get();
        float rs = m.radiusSmall;

        if (hv > 0.01f) g.roundRect(x, y, w, m.rowH, rs, ColorUtil.mulAlpha(p.hover(), hv));
        g.textMid(NAME, x + m.pad, y, m.rowH, ColorUtil.mix(p.muted(), p.text(), Math.max(hv, 0.6f)));

        int chevron = ColorUtil.mix(p.dim(), p.primary(), ex);
        float cx = x + w - m.pad - 5f;
        g.bitmap(Icons.CHEVRON_RIGHT, cx + 1f, y + (m.rowH - 5f) / 2f, 1, ColorUtil.mulAlpha(chevron, 1f - ex));
        g.bitmap(Icons.CHEVRON_DOWN, cx - 1f, y + (m.rowH - 3f) / 2f, 1, ColorUtil.mulAlpha(chevron, ex));

        float areaH = ex * (childH + 2f * CHILD_PAD);
        if (areaH > 0.5f) {
            g.pushScissor(x, y + m.rowH, w, areaH);
            ctx.pushClip(x, y + m.rowH, w, areaH);
            g.roundRect(x + 1f, y + m.rowH, w - 2f, childH + 2f * CHILD_PAD, rs, ColorUtil.mulAlpha(p.field(), ex));
            g.pushAlpha(ex);
            for (Control c : controls) c.render(ctx);
            g.popAlpha();
            ctx.popClip();
            g.popScissor();
        }

        if (focusAnim.get() > 0.02f) {
            g.roundBorder(x, y, w, m.rowH, rs, 1, ColorUtil.mulAlpha(p.primary(), focusAnim.get()));
        }
        if (hoveredNow && ctx.settings.showTooltips) {
            ctx.tooltip.request(this, "Interface appearance  |  RMB: settings", ctx.mx, ctx.my);
        }
    }

    @Override
    public boolean mouseClicked(UiContext ctx, double mx, double my, int button) {
        if (expand.get() > 0.5f) {
            for (Control c : controls) if (c.mouseClicked(ctx, mx, my, button)) return true;
        }
        if (!hit(mx, my)) return false;
        ctx.focus.set(this);
        if (button == 1) {
            expanded = !expanded;
            return true;
        }
        return true;
    }

    @Override
    public void collectFocus(FocusManager fm, CategoryPanel panel) {
        fm.add(this, panel);
        if (expanded) for (Control c : controls) fm.add(c, panel);
    }

    @Override public float fx() { return x; }
    @Override public float fy() { return y; }
    @Override public float fw() { return w; }
    @Override public float fh() { return h; }
    @Override public void setFocused(boolean f) { focused = f; }
    @Override public boolean isFocused() { return focused; }

    @Override
    public boolean onEnter(UiContext ctx) {
        expanded = !expanded;
        return true;
    }

    @Override
    public boolean onHorizontal(UiContext ctx, int dir) {
        if (dir > 0 && !expanded) {
            expanded = true;
            return true;
        }
        if (dir < 0 && expanded) {
            expanded = false;
            return true;
        }
        return false;
    }
}
