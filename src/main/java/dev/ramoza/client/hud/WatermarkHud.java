package dev.ramoza.client.hud;

import dev.ramoza.client.config.GuiSettings;
import dev.ramoza.client.core.RamozaCore;
import dev.ramoza.client.gui.render.Gfx;
import dev.ramoza.client.gui.render.Icons;
import dev.ramoza.client.gui.render.UiFont;
import dev.ramoza.client.gui.theme.Palette;
import dev.ramoza.client.module.render.Watermark;
import dev.ramoza.client.util.ColorUtil;
import dev.ramoza.client.util.MathUtil;
import net.minecraft.client.MinecraftClient;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Draws the watermark (two rows of rounded pills, like the reference) and handles dragging it.
 * Written against {@link Gfx}; the game state comes from {@link HudStats}.
 * Coordinates are scaled GUI pixels, the same space as mouse events of a Screen.
 */
public final class WatermarkHud {
    private static final float ROW_H = 17f;
    private static final float ROW_GAP = 3f;
    private static final float PAD_X = 8f;
    private static final float SEG_GAP = 6f;
    private static final float ICON_GAP = 4f;
    private static final float LOGO_SCALE = 1.35f;
    private static final float MAX_SERVER_W = 110f;
    private static final DateTimeFormatter CLOCK = DateTimeFormatter.ofPattern("HH:mm:ss");

    private static final class Seg {
        final String[] icon; // null for the logo
        final String text;
        float w;

        Seg(String[] icon, String text) {
            this.icon = icon;
            this.text = text;
        }
    }

    private final RamozaCore core;
    private float bx, by, bw, bh; // bounds of the whole block from the last frame; bw = 0 when nothing is drawn
    private boolean dragging;
    private float grabX, grabY;   // mouse offset from the block's top-left corner while dragging

    public WatermarkHud(RamozaCore core) { this.core = core; }

    public boolean dragging() { return dragging; }

    // ------------------------------------------------------------ drawing

    /**
     * @param editAlpha 0 in the normal HUD; while the ClickGUI is open the opacity (0..1) of the outline that shows
     *                  the watermark can be dragged
     */
    public void render(Gfx g, int sw, int sh, float editAlpha, double mouseX, double mouseY) {
        Watermark m = core.modules.get(Watermark.class);
        MinecraftClient mc = MinecraftClient.getInstance();
        if (m == null || !m.isEnabled() || mc.player == null) {
            bw = 0f;
            return;
        }
        List<Seg> row1 = new ArrayList<>(5);
        List<Seg> row2 = new ArrayList<>(4);
        if (m.showUsername()) row1.add(new Seg(Icons.HUD_USER, mc.player.getName().getString()));
        if (m.showFps()) row1.add(new Seg(Icons.HUD_MONITOR, HudStats.fps(mc) + " FPS"));
        if (m.showLogo()) row1.add(new Seg(null, "R"));
        if (m.showPing()) row1.add(new Seg(Icons.HUD_WIFI, HudStats.ping(mc) + " ms"));
        if (m.showTime()) row1.add(new Seg(Icons.HUD_CLOCK, LocalTime.now().format(CLOCK)));
        if (m.showCoords()) {
            row2.add(new Seg(Icons.HUD_PIN, "x " + (int) Math.floor(mc.player.getX())
                    + " y " + (int) Math.floor(mc.player.getY())
                    + " z " + (int) Math.floor(mc.player.getZ())));
        }
        if (m.showTps()) {
            double tps = HudStats.tps();
            row2.add(new Seg(Icons.HUD_SIGNAL, (Double.isNaN(tps) ? "--" : String.format(Locale.ROOT, "%.1f", tps)) + " TPS"));
        }
        if (m.showBps()) row2.add(new Seg(Icons.HUD_SPEED, String.format(Locale.ROOT, "%.1f", HudStats.bps()) + " BPS"));
        if (m.showServer()) row2.add(new Seg(Icons.HUD_SERVER, g.fit(HudStats.server(mc), MAX_SERVER_W)));

        float w1 = measure(g, row1);
        float w2 = measure(g, row2);
        int rows = (row1.isEmpty() ? 0 : 1) + (row2.isEmpty() ? 0 : 1);
        if (rows == 0) {
            bw = 0f;
            return;
        }
        bw = Math.max(w1, w2);
        bh = rows * ROW_H + (rows - 1) * ROW_GAP;

        GuiSettings s = core.settings;
        bx = MathUtil.clamp(s.hudX * sw - bw / 2f, 0f, Math.max(0f, sw - bw));
        by = MathUtil.clamp(s.hudY * sh, 0f, Math.max(0f, sh - bh));

        Palette p = core.themes.palette();
        float radius = m.roundness() * ROW_H / 2f;
        float y = by;
        if (!row1.isEmpty()) {
            drawRow(g, p, m, row1, w1, bx + (bw - w1) / 2f, y, radius);
            y += ROW_H + ROW_GAP;
        }
        if (!row2.isEmpty()) drawRow(g, p, m, row2, w2, bx + (bw - w2) / 2f, y, radius);

        if (editAlpha > 0.01f) {
            boolean hot = dragging || hit(mouseX, mouseY);
            g.pushAlpha(editAlpha * (hot ? 1f : 0.55f));
            g.roundBorder(bx - 3, by - 3, bw + 6, bh + 6, radius + 3, 1, p.primary());
            g.popAlpha();
        }
    }

    private float measure(Gfx g, List<Seg> row) {
        if (row.isEmpty()) return 0f;
        float total = 2f * PAD_X;
        for (int i = 0; i < row.size(); i++) {
            Seg seg = row.get(i);
            if (seg.icon == null) {
                seg.w = g.textWidth(seg.text, UiFont.WATERMARK) * LOGO_SCALE;
            } else {
                seg.w = Icons.width(seg.icon) + ICON_GAP + g.textWidth(seg.text);
            }
            total += seg.w;
            if (i > 0) total += 2f * SEG_GAP + 1f;
        }
        return total;
    }

    private void drawRow(Gfx g, Palette p, Watermark m, List<Seg> row, float w, float x, float y, float radius) {
        int bg = ColorUtil.withAlpha(p.panel(), Math.round(255f * m.backgroundAlpha()));
        g.roundRect(x, y, w, ROW_H, radius, bg);
        g.roundBorder(x, y, w, ROW_H, radius, 1, ColorUtil.mulAlpha(p.border(), 0.7f));

        float cx = x + PAD_X;
        for (int i = 0; i < row.size(); i++) {
            Seg seg = row.get(i);
            if (i > 0) {
                cx += SEG_GAP;
                g.rect(cx, y + (ROW_H - 9f) / 2f, 1f, 9f, ColorUtil.mulAlpha(p.border(), 0.9f));
                cx += 1f + SEG_GAP;
            }
            if (seg.icon == null) {
                float ty = y + (ROW_H - g.fontHeight() * LOGO_SCALE) / 2f;
                g.textScaled(seg.text, cx, ty, LOGO_SCALE, p.primary(), false, UiFont.WATERMARK);
            } else {
                float iy = y + (ROW_H - Icons.height(seg.icon)) / 2f;
                g.bitmap(seg.icon, cx, iy, 1, p.primary());
                g.textMid(seg.text, cx + Icons.width(seg.icon) + ICON_GAP, y, ROW_H, p.text());
            }
            cx += seg.w;
        }
    }

    // ------------------------------------------------------------ dragging

    public boolean hit(double mx, double my) {
        return bw > 0f && mx >= bx && mx <= bx + bw && my >= by && my <= by + bh;
    }

    /** Starts dragging if the mouse is on the watermark. */
    public boolean beginDrag(double mx, double my) {
        if (!hit(mx, my)) return false;
        dragging = true;
        grabX = (float) mx - bx;
        grabY = (float) my - by;
        return true;
    }

    public void drag(double mx, double my, int sw, int sh) {
        if (!dragging || sw <= 0 || sh <= 0) return;
        float left = MathUtil.clamp((float) mx - grabX, 0f, Math.max(0f, sw - bw));
        float top = MathUtil.clamp((float) my - grabY, 0f, Math.max(0f, sh - bh));
        core.settings.hudX = (left + bw / 2f) / sw;
        core.settings.hudY = top / sh;
    }

    /** Returns true if a drag was in progress (the caller should save the config). */
    public boolean endDrag() {
        boolean was = dragging;
        dragging = false;
        return was;
    }
}
