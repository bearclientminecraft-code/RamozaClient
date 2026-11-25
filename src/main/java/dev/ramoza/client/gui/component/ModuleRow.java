package dev.ramoza.client.gui.component;

import dev.ramoza.client.gui.FocusManager;
import dev.ramoza.client.gui.Focusable;
import dev.ramoza.client.gui.Metrics;
import dev.ramoza.client.gui.UiContext;
import dev.ramoza.client.gui.anim.SmoothFloat;
import dev.ramoza.client.gui.render.Gfx;
import dev.ramoza.client.gui.render.Icons;
import dev.ramoza.client.gui.theme.Palette;
import dev.ramoza.client.module.ClientModule;
import dev.ramoza.client.setting.Setting;
import dev.ramoza.client.util.ColorUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** One module: name, enabled pill, and (right-click / Right arrow) its expandable settings. */
public final class ModuleRow extends PanelEntry implements Focusable {
    private static final float CHILD_PAD = 2f;

    private final ClientModule module;
    private final List<Control> controls = new ArrayList<>();
    private final SmoothFloat expand = new SmoothFloat(0f, 14f);
    private final SmoothFloat enabled;
    private final SmoothFloat focusAnim = new SmoothFloat(0f, 18f);
    private boolean expanded;
    private boolean focused;

    public ModuleRow(ClientModule module) {
        this.module = module;
        this.enabled = new SmoothFloat(module.isEnabled() ? 1f : 0f, 16f);
        for (Setting<?> s : module.settings()) controls.add(ControlFactory.create(s));
    }

    public ClientModule module() { return module; }

    @Override
    public boolean matches(String query) {
        return query.isEmpty() || module.name().toLowerCase(Locale.ROOT).contains(query);
    }

    private float childrenHeight(Metrics m) {
        float t = 0f;
        for (Control c : controls) t += c.preferredHeight(m);
        return t;
    }

    @Override
    public float contentHeight(UiContext ctx) {
        Metrics m = ctx.m;
        if (controls.isEmpty()) return m.rowH;
        return m.rowH + expand.get() * (childrenHeight(m) + 2f * CHILD_PAD);
    }

    @Override
    public void renderEntry(UiContext ctx, float x, float y, float w, boolean draw) {
        Metrics m = ctx.m;
        Gfx g = ctx.g;
        Palette p = ctx.p;

        enabled.setTarget(module.isEnabled() ? 1f : 0f);
        enabled.update(ctx.dt);
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
        float en = enabled.get();
        float pr = press.get();
        float ex = expand.get();
        float rs = m.radiusSmall; // soft row corners

        if (en > 0.01f) g.roundRect(x, y, w, m.rowH, rs, ColorUtil.mulAlpha(p.active(), en));
        if (hv > 0.01f) g.roundRect(x, y, w, m.rowH, rs, ColorUtil.mulAlpha(p.hover(), hv));
        if (pr > 0.01f) g.roundRect(x, y, w, m.rowH, rs, ColorUtil.withAlpha(ColorUtil.BLACK, Math.round(0x34 * pr)));

        int nameColor = ColorUtil.mix(p.muted(), p.text(), Math.max(en, hv * 0.75f));
        float textMax = w - m.pad * 2f - (controls.isEmpty() ? 0f : 8f);
        g.textMid(g.fit(module.name(), textMax), x + m.pad, y, m.rowH, nameColor);

        if (!controls.isEmpty()) {
            float cx = x + w - m.pad - 4f;
            int chevron = ColorUtil.mix(p.dim(), p.text(), Math.max(en, hv));
            if (ex >= 0.5f) g.bitmap(Icons.CHEVRON_DOWN, cx - 1f, y + (m.rowH - 3f) / 2f, 1, chevron);
            else g.bitmap(Icons.CHEVRON_RIGHT, cx + 1f, y + (m.rowH - 5f) / 2f, 1, chevron);
        }

        float areaH = ex * (childH + 2f * CHILD_PAD);
        if (!controls.isEmpty() && areaH > 0.5f) {
            g.pushScissor(x, y + m.rowH, w, areaH);
            ctx.pushClip(x, y + m.rowH, w, areaH);
            g.roundRect(x + 1f, y + m.rowH, w - 2f, childH + 2f * CHILD_PAD, rs, ColorUtil.mulAlpha(p.field(), ex));
            if (ex < 0.999f) g.pushAlpha(ex);
            for (Control c : controls) c.render(ctx);
            if (ex < 0.999f) g.popAlpha();
            ctx.popClip();
            g.popScissor();
        }

        if (focusAnim.get() > 0.02f) {
            g.roundBorder(x, y, w, m.rowH, rs, 1, ColorUtil.mulAlpha(p.primary(), focusAnim.get()));
        }

        if (hoveredNow && ctx.settings.showTooltips) {
            String tip = module.description() + (controls.isEmpty() ? "" : "  |  RMB: settings");
            ctx.tooltip.request(this, tip, ctx.mx, ctx.my);
        }
    }

    @Override
    public boolean mouseClicked(UiContext ctx, double mx, double my, int button) {
        if (expand.get() > 0.5f) {
            for (Control c : controls) if (c.mouseClicked(ctx, mx, my, button)) return true;
        }
        if (!hit(mx, my)) return false;
        ctx.focus.set(this);
        if (button == 0) {
            module.toggle();
            setPressed(true);
            ctx.pressTarget = this;
            return true;
        }
        if (button == 1 && !controls.isEmpty()) {
            expanded = !expanded;
            return true;
        }
        return false;
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
        module.toggle();
        return true;
    }

    @Override
    public boolean onHorizontal(UiContext ctx, int dir) {
        if (dir > 0 && !controls.isEmpty() && !expanded) {
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
