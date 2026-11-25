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

    public boolean isEmpty() { return entries.isEmpty(); }

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
        float r = m.radius;
        boolean collapsedLook = ex < 0.5f;

        g.pushAlpha(a);

        // body surface starts under the header, so translucent colors are never blended twice
        if (bodyH > 1f) g.roundRectEx(px, py + headerH, pw, bodyH, r, p.panel(), false, true);

        // header: only the top corners are rounded while the panel is open
        g.roundRectEx(px, py, pw, headerH, r, p.header(), true, collapsedLook);
        if (hh > 0.01f) g.roundRectEx(px, py, pw, headerH, r, ColorUtil.mulAlpha(p.hover(), hh), true, collapsedLook);

        float ix = px + m.pad + 1f;
        int iconSize = 10;
        int iconCol = ColorUtil.mix(p.muted(), p.text(), Math.max(act, 0.55f + hh * 0.45f));
        g.categoryIcon(category, ix, py + (headerH - iconSize) / 2f, iconSize, iconCol);
        ix += iconSize + 4f;
        g.textMid(category.title(), ix, py, headerH, ColorUtil.mix(p.muted(), p.text(), Math.max(act, 0.55f + hh * 0.45f)));

        float tx = px + pw - m.pad - 5f;
        if (ex >= 0.5f) {
            g.bitmap(Icons.TRIANGLE_DOWN, tx, py + (headerH - 3f) / 2f, 1, p.muted());
        } else {
            g.bitmap(Icons.TRIANGLE_RIGHT, tx + 1f, py + (headerH - 5f) / 2f, 1, p.muted());
        }

        // accent underline (pulled in from the corners while collapsed so it does not poke out of them)
        float li = (1f - ex) * Math.max(0f, r - 1f);
        g.rect(px + li, py + headerH - 1f, pw - 2f * li, 1f, p.primary());

        bodyTop = py + headerH;
        bodyBottom = bodyTop + bodyH;
        if (bodyH > 1f) {
            // ONE scissor for the whole body (scissor changes split the GPU batch; per-row ones were the main cost)
            g.pushScissor(px, bodyTop, pw, bodyH);
            ctx.pushClip(px, bodyTop, pw, bodyH);
            float cy = bodyTop + BODY_PAD - scroll.get();
            float ex0 = px + BODY_PAD;
            float ew = pw - 2f * BODY_PAD;
            for (PanelEntry e : entries) {
                float vh = e.visibleHeight(ctx);
                if (vh < 0.5f) continue;
                boolean draw = cy + vh > bodyTop - 1f && cy < bodyBottom + 1f;
                float mv = e.matchValue();
                if (draw && mv < 0.999f) g.pushAlpha(mv);
                e.renderEntry(ctx, ex0, cy, ew, draw);
                if (draw && mv < 0.999f) g.popAlpha();
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
                g.roundRect(px + pw - 3f, ty, 2f, thumbH, 1f,
                        ColorUtil.mulAlpha(ColorUtil.withAlpha(p.text(), 0x80), scrollbar.get()));
            }
        }

        if (focusAnim.get() > 0.02f) {
            g.roundBorder(px + 1f, py + 1f, pw - 2f, headerH - 2f, Math.max(0f, r - 1f), 1,
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
