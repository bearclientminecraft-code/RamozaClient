package dev.ramoza.client.gui.component;

import dev.ramoza.client.gui.Metrics;
import dev.ramoza.client.gui.UiContext;
import dev.ramoza.client.gui.anim.SmoothFloat;
import dev.ramoza.client.gui.render.Gfx;
import dev.ramoza.client.gui.render.Icons;
import dev.ramoza.client.gui.theme.Palette;
import dev.ramoza.client.util.ColorUtil;

import java.util.List;
import java.util.function.IntConsumer;
import java.util.function.IntSupplier;

public final class Dropdown extends Control {
    private final List<String> options;
    private final IntSupplier getter;
    private final IntConsumer setter;
    private final SmoothFloat open = new SmoothFloat(0f, 16f);
    private boolean isOpen;
    private float headerH = 14f;
    private float optionH = 13f;

    public Dropdown(String label, String tip, List<String> options, IntSupplier getter, IntConsumer setter) {
        super(label, tip);
        this.options = options;
        this.getter = getter;
        this.setter = setter;
    }

    @Override
    public float preferredHeight(Metrics m) {
        return m.controlH + open.get() * options.size() * m.optionH;
    }

    @Override public float fh() { return headerH; }

    @Override
    public void render(UiContext ctx) {
        frame(ctx);
        Gfx g = ctx.g;
        Palette p = ctx.p;
        Metrics m = ctx.m;
        headerH = m.controlH;
        optionH = m.optionH;
        open.setTarget(isOpen ? 1f : 0f);
        open.update(ctx.dt);

        boolean headerHover = hoveredNow && ctx.my < y + headerH;
        float hv = headerHover ? 1f : 0f;
        if (headerHover) g.roundRect(x, y, w, headerH, m.radiusSmall, p.hover());

        g.textMid(label, x + m.pad, y, headerH, ColorUtil.mix(p.muted(), p.text(), Math.max(open.get(), hv)));

        int sel = Math.max(0, Math.min(options.size() - 1, getter.getAsInt()));
        String value = options.get(sel);
        float vw = Math.max(38f, g.textWidth(value) + 18f);
        float bx = x + w - m.pad - vw;
        float by = y + 2f;
        float bh = headerH - 4f;
        g.roundRect(bx, by, vw, bh, m.radiusSmall, p.field());
        g.roundBorder(bx, by, vw, bh, m.radiusSmall, 1,
                ColorUtil.mix(ColorUtil.mulAlpha(p.border(), 0.8f), p.primary(), open.get()));
        g.text(value, bx + 5f, by + (bh - 8f) / 2f, p.text());
        g.bitmap(Icons.CHEVRON_DOWN, bx + vw - 9f, by + (bh - 3f) / 2f, 1, ColorUtil.mix(p.muted(), p.primary(), open.get()));

        float o = open.get();
        if (o > 0.01f) {
            float oy0 = y + headerH;
            float regionH = o * options.size() * optionH;
            g.pushScissor(x, oy0, w, regionH);
            ctx.pushClip(x, oy0, w, regionH);
            for (int i = 0; i < options.size(); i++) {
                float oy = oy0 + i * optionH;
                boolean over = !ctx.hoverBlocked && ctx.mx >= x && ctx.mx < x + w
                        && ctx.my >= oy && ctx.my < oy + optionH
                        && ctx.mx >= ctx.clipL() && ctx.mx < ctx.clipR() && ctx.my >= ctx.clipT() && ctx.my < ctx.clipB();
                boolean selected = i == sel;
                if (over) g.roundRect(x + 2f, oy, w - 4f, optionH, m.radiusSmall, p.hover());
                int col = selected ? p.primary() : (over ? p.text() : p.muted());
                g.textMid(options.get(i), x + m.pad + 4f, oy, optionH, col);
                if (selected) g.circle(x + w - m.pad - 3f, oy + optionH / 2f, 2f, p.primary());
            }
            ctx.popClip();
            g.popScissor();
        }
        drawFocus(ctx);
    }

    @Override
    public boolean mouseClicked(UiContext ctx, double mx, double my, int button) {
        if (button != 0 || !hit(mx, my)) return false;
        ctx.focus.set(this);
        if (my < y + headerH) {
            isOpen = !isOpen;
        } else if (open.get() > 0.5f) {
            int idx = (int) ((my - (y + headerH)) / optionH);
            if (idx >= 0 && idx < options.size()) setter.accept(idx);
            isOpen = false;
        }
        return true;
    }

    @Override
    public boolean onEnter(UiContext ctx) {
        isOpen = !isOpen;
        return true;
    }

    @Override
    public boolean onHorizontal(UiContext ctx, int dir) {
        int n = options.size();
        setter.accept(Math.floorMod(getter.getAsInt() + dir, n));
        return true;
    }
}
