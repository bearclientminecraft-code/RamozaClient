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
import dev.ramoza.client.module.Category;
import dev.ramoza.client.util.ColorUtil;
import dev.ramoza.client.util.MathUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * A category dashboard panel: header (icon, title, active count, chevron), collapsible and
 * scrollable body, smooth search filtering and keyboard focus on the header.
 */
public final class CategoryPanel extends Component implements Focusable {
    private static final float BODY_PAD = 3f;

    private final Category category;
    private final String[] icon;
    private final GuiSettings settings;
    private final List<PanelEntry> entries = new ArrayList<>();

    private final SmoothFloat expand;
    private final SmoothFloat scroll = new SmoothFloat(0f, 16f);
    private final SmoothFloat shown = new SmoothFloat(1f, 14f);
    private final SmoothFloat slideX = new SmoothFloat(0f, 10f);
    private final SmoothFloat activeAnim = new SmoothFloat(0f, 12f);
    private final SmoothFloat scrollbar = new SmoothFloat(0f, 10f);
    private final SmoothFloat focusAnim = new SmoothFloat(0f, 18f);
    private final SmoothFloat headerHover = new SmoothFloat(0f, 18f);

    private float scrollTarget;
    private float scrollIdle = 10f;
    private boolean placed;
    private boolean visibleTarget = true;
    private boolean forceOpen;
    private boolean focused;
    private float bodyTop;
    private float bodyBottom;
    private float contentH;
    private float viewH;
    private float headerH = 22f;

    public CategoryPanel(Category category, GuiSettings settings) {
        this.category = category;
        this.settings = settings;
        this.icon = Icons.forCategory(category);
        this.expand = new SmoothFloat(isCollapsed() ? 0f : 1f, 12f);
    }

    public Category category() { return category; }

    public void add(PanelEntry e) { entries.add(e); }

    private boolean isCollapsed() { return settings.collapsed.getOrDefault(category, false); }

    private void setCollapsed(boolean v) { settings.collapsed.put(category, v); }

    /** @param query already lower-cased and trimmed */
    public void applyQuery(String query) {
        boolean any = false;
        for (PanelEntry e : entries) {
            boolean m = e.matches(query);
            e.setMatch(m);
            any |= m;
        }
        visibleTarget = query.isEmpty() || any;
        forceOpen = !query.isEmpty();
        scrollTarget = 0f;
    }

    public boolean hasMatches() { return visibleTarget; }

    public boolean isShown() { return shown.get() > 0.01f; }

    public int enabledCount() {
        int n = 0;
        for (PanelEntry e : entries) if (e instanceof ModuleRow && ((ModuleRow) e).module().isEnabled()) n++;
        return n;
    }

    public void renderPanel(UiContext ctx, float targetX, float top, float maxH, float appear) {
        Metrics m = ctx.m;
        Gfx g = ctx.g;
        Palette p = ctx.p;

        if (!placed) {
            slideX.snap(targetX);
            placed = true;
        }
        slideX.setTarget(targetX);
        slideX.update(ctx.dt);
        shown.setTarget(visibleTarget ? 1f : 0f);
        shown.update(ctx.dt);
        expand.setTarget(isCollapsed() && !forceOpen ? 0f : 1f);
        expand.update(ctx.dt);
        focusAnim.setTarget(focused ? 1f : 0f);
        focusAnim.update(ctx.dt);
        activeAnim.setTarget(enabledCount() > 0 ? 1f : 0f);
        activeAnim.update(ctx.dt);
        scrollIdle += ctx.dt;
        for (PanelEntry e : entries) e.tickMatch(ctx.dt);

        headerH = m.headerH;
        contentH = BODY_PAD * 2f;
        for (PanelEntry e : entries) {
            float vh = e.visibleHeight(ctx);
            if (vh > 0.5f) contentH += vh + m.entryGap;
        }
        float maxBody = Math.max(40f, maxH - headerH);
        viewH = Math.min(contentH, maxBody);
        float maxScroll = Math.max(0f, contentH - viewH);
        scrollTarget = MathUtil.clamp(scrollTarget, 0f, maxScroll);
        scroll.setTarget(scrollTarget);
        scroll.update(ctx.dt);

        float ex = expand.get();
        float bodyH = viewH * ex;
        float panelH = headerH + bodyH;
        float appearC = MathUtil.saturate(appear);
        float a = appearC * shown.get();
        float px = slideX.get();
        float py = top + (1f - appearC) * 10f;
        float pw = m.panelW;

        if (a < 0.01f) {
            setBounds(px, py, 0f, 0f);
            return;
        }
        setBounds(px, py, pw, panelH);

        boolean overHeader = !ctx.hoverBlocked && a > 0.5f && ctx.mx >= px && ctx.mx < px + pw
                && ctx.my >= py && ctx.my < py + headerH;
        headerHover.setTarget(overHeader ? 1f : 0f);
        headerHover.update(ctx.dt);
        float hh = headerHover.get();
        float act = activeAnim.get();
        float rl = m.radiusLarge;

        g.pushAlpha(a);

        g.shadow(px, py, pw, panelH, rl, 5, p.shadow());
        g.roundRect(px, py, pw, panelH, rl, p.panel());

        // header: rounded rect taller than the visible area so only the top corners show
        g.pushScissor(px, py, pw, headerH);
        float hh2 = headerH + 2f * rl * ex;
        g.roundRect(px, py, pw, hh2, rl, p.header());
        if (hh > 0.01f) g.roundRect(px, py, pw, hh2, rl, ColorUtil.mulAlpha(p.hover(), hh));
        g.popScissor();

        g.roundRectV(px, py, pw, Math.min(panelH, 34f), rl, p.gloss(), ColorUtil.withAlpha(p.gloss(), 0));

        float ix = px + m.pad;
        g.bitmap(icon, ix, py + (headerH - 9f) / 2f, 1, ColorUtil.mix(p.muted(), p.primary(), act));
        g.textMid(category.title(), ix + 14f, py, headerH, ColorUtil.mix(p.muted(), p.text(), Math.max(act, 0.35f + hh * 0.65f)));

        float chevX = px + pw - m.pad - 5f;
        int chevColor = ColorUtil.mix(p.dim(), p.primary(), ex);
        g.bitmap(Icons.CHEVRON_RIGHT, chevX + 1f, py + (headerH - 5f) / 2f, 1, ColorUtil.mulAlpha(chevColor, 1f - ex));
        g.bitmap(Icons.CHEVRON_DOWN, chevX - 1f, py + (headerH - 3f) / 2f, 1, ColorUtil.mulAlpha(chevColor, ex));

        int count = enabledCount();
        if (count > 0) {
            String s = String.valueOf(count);
            float bw = g.textWidth(s) + 7f;
            float bx = chevX - 6f - bw;
            g.roundRect(bx, py + (headerH - 10f) / 2f, bw, 10f, 5f, ColorUtil.withAlpha(p.primary(), 0x38));
            g.text(s, bx + 3.5f, py + (headerH - 8f) / 2f, p.primary());
        }

        g.roundRectH(px + m.pad, py + headerH - 1f, pw - 2f * m.pad, 1f, 0f,
                ColorUtil.mulAlpha(ColorUtil.withAlpha(p.primary(), 0xC0), 0.3f + 0.7f * ex),
                ColorUtil.mulAlpha(ColorUtil.withAlpha(p.secondary(), 0x30), 0.3f + 0.7f * ex));

        bodyTop = py + headerH;
        bodyBottom = bodyTop + bodyH;
        if (bodyH > 1f) {
            g.pushScissor(px, bodyTop, pw, bodyH);
            ctx.pushClip(px, bodyTop, pw, bodyH);
            float cy = bodyTop + BODY_PAD - scroll.get();
            float ex0 = px + BODY_PAD;
            float ew = pw - 2f * BODY_PAD;
            for (PanelEntry e : entries) {
                float vh = e.visibleHeight(ctx);
                if (vh < 0.5f) continue;
                boolean draw = cy + vh > bodyTop - 1f && cy < bodyBottom + 1f;
                if (draw) {
                    g.pushScissor(px, cy, pw, vh);
                    ctx.pushClip(px, cy, pw, vh);
                    g.pushAlpha(e.matchValue());
                    e.renderEntry(ctx, ex0, cy, ew, true);
                    g.popAlpha();
                    ctx.popClip();
                    g.popScissor();
                } else {
                    e.renderEntry(ctx, ex0, cy, ew, false);
                }
                cy += vh + m.entryGap;
            }
            ctx.popClip();
            g.popScissor();

            boolean overBody = !ctx.hoverBlocked && ctx.mx >= px && ctx.mx < px + pw && ctx.my >= bodyTop && ctx.my < bodyBottom;
            scrollbar.setTarget(maxScroll > 1f && (scrollIdle < 0.9f || overBody) ? 1f : 0f);
            scrollbar.update(ctx.dt);
            if (maxScroll > 1f && scrollbar.get() > 0.01f) {
                float thumbH = Math.max(14f, bodyH * viewH / contentH);
                float ty = bodyTop + (scroll.get() / maxScroll) * (bodyH - thumbH);
                g.roundRect(px + pw - 4f, ty, 2f, thumbH, 1f,
                        ColorUtil.mulAlpha(ColorUtil.withAlpha(p.text(), 0x80), scrollbar.get()));
            }
        }

        g.roundBorder(px, py, pw, panelH, rl, 1, p.border());
        if (focusAnim.get() > 0.02f) {
            g.roundBorder(px + 1f, py + 1f, pw - 2f, headerH - 2f, Math.max(0f, rl - 1f), 1,
                    ColorUtil.mulAlpha(p.primary(), focusAnim.get()));
        }
        g.popAlpha();
    }

    public void collectFocus(FocusManager fm) {
        if (!visibleTarget) return;
        fm.add(this, this);
        if (isCollapsed() && !forceOpen) return;
        for (PanelEntry e : entries) if (e.matchTarget()) e.collectFocus(fm, this);
    }

    /** Scrolls so the focused element is inside the viewport. */
    public void ensureVisible(Focusable f) {
        if (f == this) return;
        float fy = f.fy() + (scroll.get() - scrollTarget);
        float fh = f.fh();
        float margin = 4f;
        float vTop = bodyTop + margin;
        float vBottom = bodyTop + viewH - margin;
        if (fy < vTop) scrollTarget -= vTop - fy;
        else if (fy + fh > vBottom) scrollTarget += fy + fh - vBottom;
        scrollTarget = MathUtil.clamp(scrollTarget, 0f, Math.max(0f, contentH - viewH));
        scrollIdle = 0f;
    }

    public boolean scrolled(double mx, double my, double amount) {
        if (w <= 0f || expand.get() < 0.5f) return false;
        if (mx < x || mx >= x + w || my < bodyTop || my >= bodyBottom) return false;
        float maxScroll = Math.max(0f, contentH - viewH);
        scrollTarget = MathUtil.clamp(scrollTarget - (float) amount * 18f, 0f, maxScroll);
        scrollIdle = 0f;
        return true;
    }

    @Override
    public boolean mouseClicked(UiContext ctx, double mx, double my, int button) {
        if (w <= 0f || shown.get() < 0.5f) return false;
        if (mx < x || mx >= x + w || my < y || my >= y + h) return false;
        if (my < y + headerH) {
            if (button == 0) {
                setCollapsed(!isCollapsed());
                ctx.focus.set(this);
            }
            return true;
        }
        if (expand.get() < 0.5f) return true;
        if (my >= bodyTop && my < bodyBottom) {
            for (PanelEntry e : entries) {
                if (e.matchTarget() && e.mouseClicked(ctx, mx, my, button)) return true;
            }
        }
        return true;
    }

    // ---- Focusable (header) ----
    @Override public float fx() { return x; }
    @Override public float fy() { return y; }
    @Override public float fw() { return w; }
    @Override public float fh() { return headerH; }
    @Override public void setFocused(boolean f) { focused = f; }
    @Override public boolean isFocused() { return focused; }

    @Override
    public boolean onEnter(UiContext ctx) {
        setCollapsed(!isCollapsed());
        return true;
    }

    @Override
    public boolean onHorizontal(UiContext ctx, int dir) {
        if (dir < 0 && !isCollapsed()) {
            setCollapsed(true);
            return true;
        }
        if (dir > 0 && isCollapsed()) {
            setCollapsed(false);
            return true;
        }
        return false;
    }
}
