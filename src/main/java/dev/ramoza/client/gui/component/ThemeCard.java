package dev.ramoza.client.gui.component;

import dev.ramoza.client.gui.Focusable;
import dev.ramoza.client.gui.UiContext;
import dev.ramoza.client.gui.anim.SmoothFloat;
import dev.ramoza.client.gui.render.Gfx;
import dev.ramoza.client.gui.render.Icons;
import dev.ramoza.client.gui.theme.Palette;
import dev.ramoza.client.gui.theme.ThemeDef;
import dev.ramoza.client.gui.theme.ThemeManager;
import dev.ramoza.client.util.ColorUtil;

/** Compact theme preview card: accent gradient, surface chip, name, selected highlight. */
public final class ThemeCard extends Component implements Focusable {
    private final ThemeDef def;
    private final int index;
    private final ThemeManager themes;
    private final SmoothFloat selected;
    private final SmoothFloat focusAnim = new SmoothFloat(0f, 18f);
    private boolean focused;

    public ThemeCard(ThemeDef def, int index, ThemeManager themes) {
        this.def = def;
        this.index = index;
        this.themes = themes;
        this.selected = new SmoothFloat(themes.themeIndex() == index ? 1f : 0f, 14f);
    }

    public void render(UiContext ctx) {
        begin(ctx);
        Gfx g = ctx.g;
        Palette p = ctx.p;
        float rs = Math.max(2f, ctx.m.radiusSmall);
        selected.setTarget(themes.themeIndex() == index ? 1f : 0f);
        selected.update(ctx.dt);
        focusAnim.setTarget(focused ? 1f : 0f);
        focusAnim.update(ctx.dt);
        if (hoveredNow && ctx.settings.showTooltips) ctx.tooltip.request(this, def.name + " theme", ctx.mx, ctx.my);

        float sel = selected.get();
        float cy = y - hover.get() * 1.5f + press.get();

        g.roundRect(x, cy, w, h, rs, ColorUtil.mix(p.field(), p.hover(), hover.get()));

        float px = x + 3f;
        float py = cy + 3f;
        float pw = w - 6f;
        float ph = h - 14f;
        g.roundRectH(px, py, pw, ph, Math.max(1f, rs - 1f), def.primary, def.secondary);
        g.roundRect(px + 3f, py + ph - 7f, 11f, 5f, 2f, def.tint);
        g.roundBorder(px + 3f, py + ph - 7f, 11f, 5f, 2f, 1, ColorUtil.withAlpha(ColorUtil.WHITE, 0x70));

        if (sel > 0.02f) {
            g.circle(px + pw - 6f, py + 6f, 4.5f, ColorUtil.mulAlpha(ColorUtil.WHITE, sel));
            g.bitmap(Icons.CHECK, px + pw - 8.5f, py + 4f, 1, ColorUtil.mulAlpha(def.primary, sel));
        }

        g.textCenter(g.fit(def.name, w - 4f), x + w / 2f, cy + h - 9.5f, ColorUtil.mix(p.muted(), p.text(), Math.max(sel, hover.get())));

        if (sel > 0.02f) g.roundBorder(x, cy, w, h, rs, 1, ColorUtil.mulAlpha(p.primary(), sel));
        if (focusAnim.get() > 0.02f) g.roundBorder(x - 1f, cy - 1f, w + 2f, h + 2f, rs + 1f, 1, ColorUtil.mulAlpha(p.secondary(), focusAnim.get()));
    }

    @Override
    public boolean mouseClicked(UiContext ctx, double mx, double my, int button) {
        if (button != 0 || !hit(mx, my)) return false;
        themes.setTheme(index);
        ctx.focus.set(this);
        setPressed(true);
        ctx.pressTarget = this;
        return true;
    }

    @Override public float fx() { return x; }
    @Override public float fy() { return y; }
    @Override public float fw() { return w; }
    @Override public float fh() { return h; }
    @Override public void setFocused(boolean f) { focused = f; }
    @Override public boolean isFocused() { return focused; }

    @Override
    public boolean onEnter(UiContext ctx) {
        themes.setTheme(index);
        return true;
    }

    @Override
    public boolean onHorizontal(UiContext ctx, int dir) {
        ctx.focus.move(dir);
        return true;
    }
}
