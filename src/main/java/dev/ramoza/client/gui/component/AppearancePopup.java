package dev.ramoza.client.gui.component;

import dev.ramoza.client.gui.UiContext;
import dev.ramoza.client.gui.anim.SmoothFloat;
import dev.ramoza.client.gui.render.Gfx;
import dev.ramoza.client.gui.render.Icons;
import dev.ramoza.client.gui.theme.Appearance;
import dev.ramoza.client.gui.theme.Palette;
import dev.ramoza.client.gui.theme.ThemeManager;
import dev.ramoza.client.util.ColorUtil;

/** Small popover with the Dark / Light / Liquid Glass quick switch. */
public final class AppearancePopup extends Component {
    private static final float W = 118f;
    private static final float ROW_H = 18f;

    private final ThemeManager themes;
    private final SmoothFloat open = new SmoothFloat(0f, 16f);
    private final SmoothFloat[] rowHover;
    private boolean isOpen;

    public AppearancePopup(ThemeManager themes) {
        this.themes = themes;
        this.rowHover = new SmoothFloat[Appearance.values().length];
        for (int i = 0; i < rowHover.length; i++) rowHover[i] = new SmoothFloat(0f, 18f);
    }

    public boolean isOpen() { return isOpen; }
    public boolean isVisible() { return open.get() > 0.01f; }
    public void toggle() { isOpen = !isOpen; }
    public void close() { isOpen = false; }

    public void render(UiContext ctx, float anchorRight, float anchorTop) {
        open.setTarget(isOpen ? 1f : 0f);
        open.update(ctx.dt);
        float o = open.get();
        if (o < 0.01f) {
            setBounds(0f, 0f, 0f, 0f);
            return;
        }
        Gfx g = ctx.g;
        Palette p = ctx.p;
        float rs = ctx.m.radiusSmall;
        Appearance[] all = Appearance.values();
        float ph = all.length * ROW_H + 6f;
        float px = anchorRight - W;
        float py = anchorTop - ph - 6f + (1f - o) * 5f;
        setBounds(px, py, W, ph);

        g.pushAlpha(o);
        g.roundRect(px, py, W, ph, ctx.m.radius + 2f, p.panel());
        g.roundRect(px, py, W, ph, ctx.m.radius + 2f, ColorUtil.withAlpha(p.header(), 0x60));
        g.roundBorder(px, py, W, ph, ctx.m.radius + 2f, 1, p.border());

        for (int i = 0; i < all.length; i++) {
            float ry = py + 3f + i * ROW_H;
            boolean over = !ctx.hoverBlocked && ctx.mx >= px && ctx.mx < px + W && ctx.my >= ry && ctx.my < ry + ROW_H;
            rowHover[i].setTarget(over ? 1f : 0f);
            rowHover[i].update(ctx.dt);
            boolean selected = themes.appearance() == all[i];
            float hv = rowHover[i].get();
            if (selected) g.roundRect(px + 3f, ry, W - 6f, ROW_H, rs, p.active());
            if (hv > 0.01f) g.roundRect(px + 3f, ry, W - 6f, ROW_H, rs, ColorUtil.mulAlpha(p.hover(), hv));
            swatch(g, all[i], px + 8f, ry + (ROW_H - 8f) / 2f, p);
            g.textMid(all[i].label(), px + 24f, ry, ROW_H, ColorUtil.mix(p.muted(), p.text(), selected ? 1f : hv));
            if (selected) g.bitmap(Icons.CHECK, px + W - 14f, ry + (ROW_H - 4f) / 2f, 1, p.primary());
        }
        g.popAlpha();
    }

    private static void swatch(Gfx g, Appearance a, float x, float y, Palette p) {
        switch (a) {
            case LIGHT:
                g.roundRect(x, y, 12f, 8f, 2f, 0xFFF1F1F7);
                break;
            case GLASS:
                g.roundRectV(x, y, 12f, 8f, 2f, 0xB0FFFFFF, 0x50A0A8D8);
                break;
            default:
                g.roundRect(x, y, 12f, 8f, 2f, 0xFF16161F);
                break;
        }
        g.roundBorder(x, y, 12f, 8f, 2f, 1, ColorUtil.withAlpha(p.text(), 0x50));
    }

    @Override
    public boolean mouseClicked(UiContext ctx, double mx, double my, int button) {
        if (!isOpen || button != 0 || !hit(mx, my)) return false;
        int idx = (int) ((my - (y + 3f)) / ROW_H);
        Appearance[] all = Appearance.values();
        if (idx >= 0 && idx < all.length) themes.setAppearance(all[idx]);
        return true;
    }
}
