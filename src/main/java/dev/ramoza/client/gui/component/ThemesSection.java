package dev.ramoza.client.gui.component;

import dev.ramoza.client.gui.FocusManager;
import dev.ramoza.client.gui.Focusable;
import dev.ramoza.client.gui.Metrics;
import dev.ramoza.client.gui.UiContext;
import dev.ramoza.client.gui.anim.SmoothFloat;
import dev.ramoza.client.gui.render.Gfx;
import dev.ramoza.client.gui.render.Icons;
import dev.ramoza.client.gui.theme.Palette;
import dev.ramoza.client.gui.theme.ThemeDef;
import dev.ramoza.client.gui.theme.ThemeManager;
import dev.ramoza.client.util.ColorUtil;
import dev.ramoza.client.util.MathUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** The "Themes" section inside the Misc panel: a collapsible grid of {@link ThemeCard}s. */
public final class ThemesSection extends PanelEntry implements Focusable {
    private static final int COLUMNS = 2;
    private static final float GAP = 4f;
    private static final float INSET = 3f;

    private final ThemeManager themes;
    private final List<ThemeCard> cards = new ArrayList<>();
    private final SmoothFloat expand = new SmoothFloat(1f, 14f);
    private final SmoothFloat focusAnim = new SmoothFloat(0f, 18f);
    private boolean expanded = true;
    private boolean focused;

    public ThemesSection(ThemeManager themes) {
        this.themes = themes;
        for (int i = 0; i < ThemeDef.ALL.length; i++) cards.add(new ThemeCard(ThemeDef.ALL[i], i, themes));
    }

    private static float cardHeight(Metrics m) { return MathUtil.lerpInt(30, 26, m.density); }

    private float bodyHeight(Metrics m) {
        int rows = (cards.size() + COLUMNS - 1) / COLUMNS;
        return rows * cardHeight(m) + (rows - 1) * GAP + 2f * INSET + 2f;
    }

    @Override
    public boolean matches(String query) {
        if (query.isEmpty() || "themes".contains(query)) return true;
        for (ThemeDef d : ThemeDef.ALL) if (d.name.toLowerCase(Locale.ROOT).contains(query)) return true;
        return false;
    }

    @Override
    public float contentHeight(UiContext ctx) {
        return ctx.m.rowH + expand.get() * bodyHeight(ctx.m);
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

        float ex = expand.get();
        float cw = (w - 2f * INSET - GAP * (COLUMNS - 1)) / COLUMNS;
        float ch = cardHeight(m);
        float gy = y + m.rowH + INSET;
        for (int i = 0; i < cards.size(); i++) {
            int col = i % COLUMNS;
            int row = i / COLUMNS;
            cards.get(i).setBounds(x + INSET + col * (cw + GAP), gy + row * (ch + GAP), cw, ch);
        }
        if (!draw) return;

        begin(ctx);
        float hv = hover.get();
        float rs = m.radiusSmall;
        if (hv > 0.01f) g.roundRect(x, y, w, m.rowH, rs, ColorUtil.mulAlpha(p.hover(), hv));
        g.bitmap(Icons.PALETTE, x + m.pad, y + (m.rowH - 9f) / 2f, 1, ColorUtil.mix(p.muted(), p.primary(), Math.max(ex, hv)));
        g.textMid("Themes", x + m.pad + 14f, y, m.rowH, ColorUtil.mix(p.muted(), p.text(), Math.max(hv, 0.6f)));
        ThemeDef active = themes.theme();
        g.roundRectH(x + w - m.pad - 31f, y + (m.rowH - 5f) / 2f, 15f, 5f, 2.5f, active.primary, active.secondary);
        int chevron = ColorUtil.mix(p.dim(), p.primary(), ex);
        float cx = x + w - m.pad - 5f;
        g.bitmap(Icons.CHEVRON_RIGHT, cx + 1f, y + (m.rowH - 5f) / 2f, 1, ColorUtil.mulAlpha(chevron, 1f - ex));
        g.bitmap(Icons.CHEVRON_DOWN, cx - 1f, y + (m.rowH - 3f) / 2f, 1, ColorUtil.mulAlpha(chevron, ex));

        float areaH = ex * bodyHeight(m);
        if (areaH > 0.5f) {
            g.pushScissor(x, y + m.rowH, w, areaH);
            ctx.pushClip(x, y + m.rowH, w, areaH);
            g.pushAlpha(ex);
            for (ThemeCard c : cards) c.render(ctx);
            g.popAlpha();
            ctx.popClip();
            g.popScissor();
        }
        if (focusAnim.get() > 0.02f) {
            g.roundBorder(x, y, w, m.rowH, rs, 1, ColorUtil.mulAlpha(p.primary(), focusAnim.get()));
        }
    }

    @Override
    public boolean mouseClicked(UiContext ctx, double mx, double my, int button) {
        if (expand.get() > 0.5f) {
            for (ThemeCard c : cards) if (c.mouseClicked(ctx, mx, my, button)) return true;
        }
        if (!hit(mx, my)) return false;
        ctx.focus.set(this);
        if (button == 0) expanded = !expanded;
        return true;
    }

    @Override
    public void collectFocus(FocusManager fm, CategoryPanel panel) {
        fm.add(this, panel);
        if (expanded) for (ThemeCard c : cards) fm.add(c, panel);
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
