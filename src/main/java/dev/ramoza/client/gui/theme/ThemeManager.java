package dev.ramoza.client.gui.theme;

import dev.ramoza.client.config.GuiSettings;
import dev.ramoza.client.util.MathUtil;

/**
 * Owns the active theme + appearance. The {@link #palette()} that components read follows the
 * target palette smoothly, so switching never produces an instant color jump.
 */
public final class ThemeManager {
    private static final float FOLLOW_SPEED = 9f;

    private final GuiSettings settings;
    private int themeIndex = 0;
    private Appearance appearance = Appearance.DARK;
    private final Palette current;
    private Palette target;

    public ThemeManager(GuiSettings settings) {
        this.settings = settings;
        this.target = ThemeFactory.build(theme(), appearance, settings.opacity);
        this.current = target.copy();
    }

    public ThemeDef theme() { return ThemeDef.ALL[themeIndex]; }
    public int themeIndex() { return themeIndex; }
    public Appearance appearance() { return appearance; }
    public Palette palette() { return current; }

    public void setTheme(int index) {
        themeIndex = MathUtil.clamp(index, 0, ThemeDef.ALL.length - 1);
    }

    public void setAppearance(Appearance a) { appearance = a; }

    /** Jumps straight to the target palette (used after loading the config). */
    public void snap() {
        target = ThemeFactory.build(theme(), appearance, settings.opacity);
        current.copyFrom(target);
    }

    public void update(float dt) {
        target = ThemeFactory.build(theme(), appearance, settings.opacity);
        float k = 1f - (float) Math.exp(-FOLLOW_SPEED * dt);
        current.approach(target, k);
    }
}
