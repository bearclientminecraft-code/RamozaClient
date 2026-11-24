package dev.ramoza.client.gui.component;

import dev.ramoza.client.config.GuiSettings;
import dev.ramoza.client.gui.FocusManager;
import dev.ramoza.client.gui.Metrics;
import dev.ramoza.client.gui.UiContext;
import dev.ramoza.client.gui.anim.SmoothFloat;
import dev.ramoza.client.gui.render.Gfx;
import dev.ramoza.client.gui.render.Icons;
import dev.ramoza.client.gui.theme.Palette;
import dev.ramoza.client.util.ColorUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** The floating "GUI Settings" window. */
public final class SettingsPanel extends Component {
    private static final float W = 168f;
    private static final float HEADER_H = 20f;
    private static final float FOOTER_H = 24f;
    private static final float CLOSE_ZONE = 18f;

    private final List<Control> controls = new ArrayList<>();
    private final PillButton reset;
    private final SmoothFloat open = new SmoothFloat(0f, 16f);
    private final SmoothFloat closeHover = new SmoothFloat(0f, 18f);
    private boolean isOpen;

    public SettingsPanel(GuiSettings s) {
        controls.add(new Slider("Scale", "Overall size of the interface",
                () -> s.scale, v -> s.scale = (float) v,
                GuiSettings.SCALE_MIN, GuiSettings.SCALE_MAX, 0.05,
                v -> String.format(Locale.ROOT, "%.2fx", v)));
        controls.add(new Slider("Opacity", "Opacity of the panel surfaces",
                () -> s.opacity, v -> s.opacity = (float) v,
                GuiSettings.OPACITY_MIN, GuiSettings.OPACITY_MAX, 0.05,
                v -> Math.round(v * 100) + "%"));
        controls.add(new Slider("Animation Speed", "Speed of all interface animations",
                () -> s.animationSpeed, v -> s.animationSpeed = (float) v,
                GuiSettings.SPEED_MIN, GuiSettings.SPEED_MAX, 0.1,
                v -> String.format(Locale.ROOT, "%.1fx", v)));
        controls.add(new Toggle("Blur", "Blur the game behind the GUI (uses the vanilla menu blur)",
                () -> s.blur, v -> s.blur = v));
        controls.add(new Slider("Rounded Corners", "Corner radius in pixels",
                () -> s.roundedCorners, v -> s.roundedCorners = (float) v,
                GuiSettings.CORNERS_MIN, GuiSettings.CORNERS_MAX, 1,
                v -> Math.round(v) + " px"));
        controls.add(new Toggle("Show Tooltips", "Show hints when hovering elements",
                () -> s.showTooltips, v -> s.showTooltips = v));
        controls.add(new Toggle("Compact Mode", "Tighter rows and spacing",
                () -> s.compactMode, v -> s.compactMode = v));
        reset = new PillButton(Icons.CLOSE, "Reset", "Restore default GUI settings", s::reset);
    }

    public boolean isOpen() { return isOpen; }
    public boolean isVisible() { return open.get() > 0.01f; }
    public void toggle() { isOpen = !isOpen; }
    public void close() { isOpen = false; }

    public boolean contains(double mx, double my) {
        return isVisible() && mx >= x && mx < x + w && my >= y && my < y + h;
    }

    public void collectFocus(FocusManager fm) {
        if (!isOpen) return;
        for (Control c : controls) fm.add(c, null);
        fm.add(reset, null);
    }

    public void render(UiContext ctx, float anchorRight, float anchorBottom) {
        open.setTarget(isOpen ? 1f : 0f);
        open.update(ctx.dt);
        float o = open.get();
        if (o < 0.01f) {
            setBounds(0f, 0f, 0f, 0f);
            return;
        }
        Gfx g = ctx.g;
        Palette p = ctx.p;
        Metrics m = ctx.m;

        float bodyH = 6f;
        for (Control c : controls) bodyH += c.preferredHeight(m);
        float ph = HEADER_H + bodyH + FOOTER_H;
        float px = anchorRight - W;
        float py = anchorBottom - ph - 6f + (1f - o) * 6f;
        setBounds(px, py, W, ph);
        float rl = m.radiusLarge;

        g.pushAlpha(o);
        g.shadow(px, py, W, ph, rl, 6, p.shadow());
        g.roundRect(px, py, W, ph, rl, p.panel());
        g.pushScissor(px, py, W, HEADER_H);
        g.roundRect(px, py, W, HEADER_H + 2f * rl, rl, p.header());
        g.popScissor();
        g.roundRectV(px, py, W, Math.min(ph, 34f), rl, p.gloss(), ColorUtil.withAlpha(p.gloss(), 0));

        g.bitmap(Icons.GEAR, px + m.pad, py + (HEADER_H - 9f) / 2f, 1, p.primary());
        g.textMid("GUI Settings", px + m.pad + 14f, py, HEADER_H, p.text());

        boolean overClose = !ctx.hoverBlocked && ctx.mx >= px + W - CLOSE_ZONE && ctx.mx < px + W
                && ctx.my >= py && ctx.my < py + HEADER_H;
        closeHover.setTarget(overClose ? 1f : 0f);
        closeHover.update(ctx.dt);
        g.bitmap(Icons.CLOSE, px + W - m.pad - 7f, py + (HEADER_H - 5f) / 2f, 1,
                ColorUtil.mix(p.dim(), p.text(), closeHover.get()));
        g.roundRectH(px + m.pad, py + HEADER_H - 1f, W - 2f * m.pad, 1f, 0f,
                ColorUtil.withAlpha(p.primary(), 0xB0), ColorUtil.withAlpha(p.secondary(), 0x30));

        ctx.pushClip(px, py, W, ph);
        float cy = py + HEADER_H + 3f;
        for (Control c : controls) {
            float ch = c.preferredHeight(m);
            c.setBounds(px + 3f, cy, W - 6f, ch);
            c.render(ctx);
            cy += ch;
        }
        float bw = reset.preferredWidth(g, m);
        reset.setBounds(px + W - m.pad - bw, py + ph - FOOTER_H + 3f, bw, 18f);
        reset.render(ctx);
        ctx.popClip();

        g.roundBorder(px, py, W, ph, rl, 1, p.border());
        g.popAlpha();
    }

    @Override
    public boolean mouseClicked(UiContext ctx, double mx, double my, int button) {
        if (!isOpen || !contains(mx, my)) return false;
        if (button == 0 && my < y + HEADER_H && mx >= x + W - CLOSE_ZONE) {
            close();
            return true;
        }
        for (Control c : controls) if (c.mouseClicked(ctx, mx, my, button)) return true;
        reset.mouseClicked(ctx, mx, my, button);
        return true;
    }
}
