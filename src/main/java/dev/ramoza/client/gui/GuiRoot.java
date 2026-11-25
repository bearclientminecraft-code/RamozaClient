package dev.ramoza.client.gui;

import dev.ramoza.client.config.GuiSettings;
import dev.ramoza.client.core.RamozaCore;
import dev.ramoza.client.gui.anim.Easing;
import dev.ramoza.client.gui.anim.SmoothFloat;
import dev.ramoza.client.gui.component.AppearancePopup;
import dev.ramoza.client.gui.component.BottomBar;
import dev.ramoza.client.gui.component.CategoryPanel;
import dev.ramoza.client.gui.component.GuiSettingsSection;
import dev.ramoza.client.gui.component.KeybindButton;
import dev.ramoza.client.gui.component.ModuleRow;
import dev.ramoza.client.gui.component.SearchBar;
import dev.ramoza.client.gui.component.SettingsPanel;
import dev.ramoza.client.gui.component.ThemesSection;
import dev.ramoza.client.gui.render.Gfx;
import dev.ramoza.client.gui.render.Icons;
import dev.ramoza.client.gui.render.UiFont;
import dev.ramoza.client.gui.theme.Palette;
import dev.ramoza.client.module.Category;
import dev.ramoza.client.module.ClientModule;
import dev.ramoza.client.util.ColorUtil;
import dev.ramoza.client.util.Keys;
import dev.ramoza.client.util.MathUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Root of the ClickGUI: layout, open/close timeline, input routing and keyboard navigation.
 * Contains no Minecraft types - everything draws through {@link Gfx}.
 */
public final class GuiRoot {
    private static final float OPEN_FADE = 0.28f;
    private static final float PANEL_DURATION = 0.30f;
    private static final float STAGGER = 0.05f;
    private static final float SEARCH_W = 190f;
    private static final float SEARCH_H = 18f;
    private static final float TOP_MARGIN = 8f;
    private static final float LEFT_MARGIN = 12f;

    private final RamozaCore core;
    private final UiContext ctx;
    private final List<CategoryPanel> panels = new ArrayList<>();
    private final float[] panelX;
    private final SearchBar search = new SearchBar();
    private final SettingsPanel settingsPanel;
    private final AppearancePopup popup;
    private final BottomBar bar;
    private final SmoothFloat density = new SmoothFloat(0f, 10f);
    private final SmoothFloat emptyAnim = new SmoothFloat(0f, 12f);

    private Runnable onClosed = () -> {};
    private float timer;
    private boolean closing;
    private boolean closedNotified;
    private boolean swallowNextChar;
    private String lastQuery = "";
    private String emptyQuery = "";
    private float eff = 1f;
    private float openScale = 0.95f;
    private float fade;
    private float vw = 1f;
    private float vh = 1f;

    public GuiRoot(RamozaCore core, Gfx gfx) {
        this.core = core;
        this.ctx = new UiContext(gfx, core.settings, core.themes);

        for (Category c : Category.values()) {
            CategoryPanel panel = new CategoryPanel(c, core.settings);
            if (c == Category.MISC) {
                panel.add(new ThemesSection(core.themes));
                panel.add(new GuiSettingsSection(core.settings));
            }
            for (ClientModule m : core.modules.in(c)) panel.add(new ModuleRow(m));
            panels.add(panel); // all categories are shown; empty ones get a placeholder
        }
        panelX = new float[panels.size()];

        popup = new AppearancePopup(core.themes);
        settingsPanel = new SettingsPanel(core.settings);
        bar = new BottomBar(this::toggleAppearance,
                dir -> core.themes.setAppearance(core.themes.appearance().next(dir)),
                this::toggleSettings,
                this::selfDestruct);
        density.snap(core.settings.compactMode ? 1f : 0f);
    }

    // ------------------------------------------------------------ public state

    public void setOnClosed(Runnable r) { this.onClosed = r; }
    public boolean isClosing() { return closing; }
    public float fade() { return fade; }
    public boolean isCapturing() { return ctx.focus.capturing() != null; }

    public void requestClose() {
        closing = true;
        popup.close();
        settingsPanel.close();
        ctx.focus.endCapture();
    }

    private void toggleAppearance() {
        settingsPanel.close();
        popup.toggle();
    }

    private void toggleSettings() {
        popup.close();
        settingsPanel.toggle();
    }

    /** GUI prototype of "Self Destruct": just hides the GUI until the restore key is used. */
    private void selfDestruct() {
        core.setGuiHidden(true);
        requestClose();
    }

    private float totalDuration() {
        return Math.max(OPEN_FADE, (panels.size() - 1) * STAGGER + PANEL_DURATION);
    }

    // ------------------------------------------------------------ rendering

    public void render(int sw, int sh, double rawMx, double rawMy, float realDt) {
        GuiSettings s = core.settings;
        float rdt = MathUtil.clamp(realDt, 0f, 0.05f);
        float dt = rdt * s.animationSpeed;
        ctx.dt = dt;
        ctx.time += rdt;
        swallowNextChar = false;

        core.themes.update(dt);
        ctx.p = core.themes.palette();
        density.setTarget(s.compactMode ? 1f : 0f);
        density.update(dt);
        ctx.m = new Metrics(s, density.get());
        Metrics m = ctx.m;
        Palette p = ctx.p;
        Gfx g = ctx.g;
        g.setTextAlpha(s.textAlpha());

        // open / close timeline
        if (closing) {
            timer = Math.max(0f, timer - dt * 1.3f);
            if (timer <= 0f && !closedNotified) {
                closedNotified = true;
                onClosed.run();
            }
        } else {
            timer = Math.min(totalDuration(), timer + dt);
        }
        fade = Easing.EASE_OUT_CUBIC.applyClamped(timer / OPEN_FADE);
        openScale = MathUtil.lerp(0.95f, 1f, fade);

        // fit the GUI into the window
        int n = panels.size();
        float contentW = n * m.panelW + (n - 1) * m.panelGap + 24f;
        eff = s.scale;
        if (contentW * eff > sw) eff = sw / contentW;
        vw = sw / eff;
        vh = sh / eff;
        float cx = vw / 2f;
        float cy = vh / 2f;
        mapMouse(rawMx, rawMy);

        // backdrop (screen space)
        g.rect(0, 0, sw, sh, ColorUtil.mulAlpha(p.background(), fade * (s.blur ? 0.6f : 1f)));

        g.pushTransform(0f, 0f, eff, eff);
        g.pushTransform(cx * (1f - openScale), cy * (1f - openScale), openScale, openScale);
        g.pushAlpha(fade);

        ctx.resetClip();
        ctx.tooltip.beginFrame();
        buildFocusOrder();

        ctx.hoverBlocked = settingsPanel.contains(ctx.mx, ctx.my) || (popup.isVisible() && popup.hit(ctx.mx, ctx.my));

        float top = TOP_MARGIN;
        float maxPanelH = MathUtil.clamp(vh - (top + m.topBlockH) - 34f, 80f, 420f);

        search.setBounds(cx - SEARCH_W / 2f, top, SEARCH_W, SEARCH_H);
        search.render(ctx);

        String q = search.query().trim().toLowerCase(Locale.ROOT);
        if (!q.equals(lastQuery)) {
            lastQuery = q;
            for (CategoryPanel panel : panels) panel.applyQuery(q);
        }

        int visible = 0;
        for (CategoryPanel panel : panels) if (panel.hasMatches()) visible++;
        float totalW = visible * m.panelW + Math.max(0, visible - 1) * m.panelGap;
        float left = LEFT_MARGIN; // panels start at the left edge, like the reference
        float panelsTop = top + m.topBlockH;
        int slot = 0;
        for (int i = 0; i < n; i++) {
            CategoryPanel panel = panels.get(i);
            if (panel.hasMatches()) {
                panelX[i] = left + slot * (m.panelW + m.panelGap);
                slot++;
            } else if (panelX[i] == 0f && !panel.isShown()) {
                panelX[i] = left;
            }
            float appear = Easing.EASE_OUT_CUBIC.applyClamped((timer - i * STAGGER) / PANEL_DURATION);
            panel.renderPanel(ctx, panelX[i], panelsTop, maxPanelH, appear);
        }

        boolean empty = !q.isEmpty() && visible == 0;
        if (empty) emptyQuery = search.query().trim();
        emptyAnim.setTarget(empty ? 1f : 0f);
        emptyAnim.update(dt);
        if (emptyAnim.get() > 0.01f) drawEmptyState(cx, panelsTop + 24f);

        ctx.hoverBlocked = false;
        bar.render(ctx, vw, vh);
        bar.setAppearanceOpen(popup.isOpen());
        bar.setSettingsOpen(settingsPanel.isOpen());
        popup.render(ctx, bar.appearanceRight(), bar.top());
        settingsPanel.render(ctx, bar.right(), bar.top());

        ctx.tooltip.update(dt, s.showTooltips);
        ctx.tooltip.render(ctx, vw, vh);

        g.popAlpha();
        g.popTransform();
        g.popTransform();
    }

    private void buildFocusOrder() {
        FocusManager fm = ctx.focus;
        fm.begin();
        fm.add(search, null);
        for (CategoryPanel panel : panels) panel.collectFocus(fm);
        bar.collectFocus(fm);
        settingsPanel.collectFocus(fm);
        fm.end();
    }

    private void drawEmptyState(float cx, float y) {
        Gfx g = ctx.g;
        Palette p = ctx.p;
        float a = emptyAnim.get();
        float w = 190f;
        float h = 70f;
        float x = cx - w / 2f;
        float yy = y + (1f - a) * 6f;
        g.pushAlpha(a);
        g.roundRect(x, yy, w, h, ctx.m.radiusLarge, p.panel());
        g.bitmap(Icons.SEARCH, cx - 9f, yy + 10f, 2, p.dim());
        g.textCenter("No results", cx, yy + 34f, p.text());
        g.textCenter(g.fit("for \"" + emptyQuery + "\"", w - 20f), cx, yy + 47f, p.muted());
        g.popAlpha();
    }

    // ------------------------------------------------------------ input

    private void mapMouse(double rx, double ry) {
        float cx = vw / 2f;
        float cy = vh / 2f;
        ctx.mx = ((rx / eff) - cx * (1f - openScale)) / openScale;
        ctx.my = ((ry / eff) - cy * (1f - openScale)) / openScale;
    }

    public boolean mouseClicked(double rx, double ry, int button) {
        if (closing) return true;
        KeybindButton capturingKey = ctx.focus.capturing();
        if (capturingKey != null && button >= 1) { // RMB / MMB / side buttons bind; LMB still cancels via the button
            capturingKey.accept(ctx, Keys.mouse(button));
            return true;
        }
        mapMouse(rx, ry);
        double mx = ctx.mx;
        double my = ctx.my;

        if (popup.isOpen()) {
            if (!popup.mouseClicked(ctx, mx, my, button) && !popup.hit(mx, my)) popup.close();
            return true;
        }
        if (settingsPanel.isOpen() && settingsPanel.mouseClicked(ctx, mx, my, button)) return true;
        if (bar.mouseClicked(ctx, mx, my, button)) return true;
        if (search.mouseClicked(ctx, mx, my, button)) return true;
        if (ctx.focus.current() == search) ctx.focus.set(null);
        for (CategoryPanel panel : panels) if (panel.mouseClicked(ctx, mx, my, button)) return true;
        ctx.focus.set(null);
        return true;
    }

    public boolean mouseReleased(double rx, double ry, int button) {
        mapMouse(rx, ry);
        if (ctx.dragTarget != null) {
            ctx.dragTarget.mouseReleased(ctx, ctx.mx, ctx.my, button);
            ctx.dragTarget = null;
        }
        if (ctx.pressTarget != null) {
            ctx.pressTarget.setPressed(false);
            ctx.pressTarget = null;
        }
        return true;
    }

    public boolean mouseDragged(double rx, double ry) {
        mapMouse(rx, ry);
        if (ctx.dragTarget == null) return false;
        ctx.dragTarget.mouseDragged(ctx, ctx.mx, ctx.my);
        return true;
    }

    public boolean mouseScrolled(double rx, double ry, double amount) {
        if (closing) return true;
        mapMouse(rx, ry);
        for (CategoryPanel panel : panels) if (panel.scrolled(ctx.mx, ctx.my, amount)) return true;
        return false;
    }

    public boolean keyPressed(int key, boolean ctrl, boolean shift) {
        if (closing) return true;

        KeybindButton capturing = ctx.focus.capturing();
        if (capturing != null) {
            capturing.accept(ctx, key);
            swallowNextChar = true;
            return true;
        }
        if (key == Keys.ESCAPE) {
            if (popup.isOpen()) popup.close();
            else if (settingsPanel.isOpen()) settingsPanel.close();
            else requestClose();
            return true;
        }
        if (ctrl && key == Keys.F) {
            ctx.focus.set(search);
            return true;
        }

        Focusable cur = ctx.focus.current();
        if (cur == search) {
            switch (key) {
                case Keys.BACKSPACE:
                    search.backspace(ctrl);
                    return true;
                case Keys.DELETE:
                    search.clear();
                    return true;
                case Keys.DOWN:
                case Keys.ENTER:
                case Keys.KP_ENTER:
                    ctx.focus.move(1);
                    afterFocusMove();
                    return true;
                case Keys.UP:
                    return true;
                default:
                    return false;
            }
        }

        switch (key) {
            case Keys.UP:
                ctx.focus.move(-1);
                afterFocusMove();
                return true;
            case Keys.DOWN:
                ctx.focus.move(1);
                afterFocusMove();
                return true;
            case Keys.LEFT:
            case Keys.RIGHT: {
                int dir = key == Keys.RIGHT ? 1 : -1;
                if (cur == null) ctx.focus.move(1);
                else if (!cur.onHorizontal(ctx, dir)) ctx.focus.jumpPanel(dir);
                afterFocusMove();
                return true;
            }
            case Keys.ENTER:
            case Keys.KP_ENTER:
            case Keys.SPACE:
                if (cur != null) cur.onEnter(ctx);
                return true;
            default:
                return false;
        }
    }

    public boolean charTyped(int codepoint) {
        if (closing) return true;
        if (swallowNextChar) {
            swallowNextChar = false;
            return true;
        }
        if (ctx.focus.capturing() != null) return true;
        if (ctx.focus.current() == search) {
            search.type(codepoint);
            return true;
        }
        if (Character.isLetterOrDigit(codepoint)) {
            ctx.focus.set(search);
            search.type(codepoint);
            return true;
        }
        return false;
    }

    private void afterFocusMove() {
        Focusable f = ctx.focus.current();
        if (f == null) return;
        CategoryPanel owner = ctx.focus.ownerOf(f);
        if (owner != null) owner.ensureVisible(f);
    }
}
