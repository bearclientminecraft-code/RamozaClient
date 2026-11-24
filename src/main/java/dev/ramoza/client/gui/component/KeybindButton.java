package dev.ramoza.client.gui.component;

import dev.ramoza.client.gui.Metrics;
import dev.ramoza.client.gui.UiContext;
import dev.ramoza.client.gui.render.Gfx;
import dev.ramoza.client.gui.theme.Palette;
import dev.ramoza.client.util.ColorUtil;
import dev.ramoza.client.util.Keys;

import java.util.function.IntConsumer;
import java.util.function.IntSupplier;

public final class KeybindButton extends Control {
    private final IntSupplier getter;
    private final IntConsumer setter;
    private boolean capturing;

    public KeybindButton(String label, String tip, IntSupplier getter, IntConsumer setter) {
        super(label, tip);
        this.getter = getter;
        this.setter = setter;
    }

    @Override public float preferredHeight(Metrics m) { return m.controlH; }

    public boolean isCapturing() { return capturing; }

    @Override
    public void render(UiContext ctx) {
        frame(ctx);
        Gfx g = ctx.g;
        Palette p = ctx.p;
        Metrics m = ctx.m;
        float hv = hover.get();
        if (hv > 0.01f) g.roundRect(x, y, w, h, m.radiusSmall, ColorUtil.mulAlpha(p.hover(), hv));
        g.textMid(label, x + m.pad, y, h, ColorUtil.mix(p.muted(), p.text(), Math.max(hv, capturing ? 1f : 0f)));

        String text = capturing ? "Press a key..." : "[ " + Keys.nameOf(getter.getAsInt()) + " ]";
        float bw = Math.max(40f, g.textWidth(text) + 10f);
        float bx = x + w - m.pad - bw;
        float by = y + 2f;
        float bh = h - 4f;
        int bg = capturing ? ColorUtil.withAlpha(p.primary(), 0x38) : p.field();
        g.roundRect(bx, by, bw, bh, m.radiusSmall, bg);
        g.roundBorder(bx, by, bw, bh, m.radiusSmall, 1,
                capturing ? p.primary() : ColorUtil.mulAlpha(p.border(), 0.8f));
        int tc = capturing ? p.primary() : (getter.getAsInt() == Keys.NONE ? p.dim() : p.text());
        g.text(text, bx + (bw - g.textWidth(text)) / 2f, by + (bh - 8f) / 2f, tc);
        drawFocus(ctx);
    }

    private void begin(UiContext ctx, boolean start) {
        capturing = start;
        if (start) ctx.focus.beginCapture(this);
        else ctx.focus.endCapture();
    }

    /** Called by the GUI root with the next pressed key while this button is capturing. */
    public void accept(UiContext ctx, int key) {
        if (key == Keys.ESCAPE) {
            // cancel
        } else if (key == Keys.BACKSPACE || key == Keys.DELETE) {
            setter.accept(Keys.NONE);
        } else {
            setter.accept(key);
        }
        begin(ctx, false);
    }

    @Override
    public boolean mouseClicked(UiContext ctx, double mx, double my, int button) {
        if (button != 0 || !hit(mx, my)) return false;
        ctx.focus.set(this);
        begin(ctx, !capturing);
        return true;
    }

    @Override
    public boolean onEnter(UiContext ctx) {
        begin(ctx, true);
        return true;
    }

    @Override
    public void setFocused(boolean f) {
        super.setFocused(f);
        if (!f) capturing = false;
    }
}
