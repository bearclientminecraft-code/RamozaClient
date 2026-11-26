package dev.ramoza.client.config;

import dev.ramoza.client.module.Category;

import java.util.EnumMap;
import java.util.Map;

/** Visual interface preferences. Persisted by {@link ConfigManager}. */
public final class GuiSettings {
    public static final float SCALE_MIN = 0.75f, SCALE_MAX = 1.5f;
    public static final float PERCENT_MIN = 0f, PERCENT_MAX = 100f;
    /** Corner radius in pixels at "Corner Rounding" = 100. */
    public static final float MAX_RADIUS_PX = 12f;
    public static final float SPEED_MIN = 0.5f, SPEED_MAX = 2.0f;

    public float scale = 1.0f;
    /** Percent values, 0-100. */
    public float menuOpacity = 100f;
    public float textOpacity = 100f;
    public float cornerRounding = 40f;
    public float animationSpeed = 1.0f;
    public boolean blur = false; // vanilla blur is the single most expensive thing in the GUI
    public boolean showTooltips = true;
    public boolean compactMode = false;

    /** Watermark position: centre X as a fraction of the screen width, top Y as a fraction of the height. */
    public float hudX = 0.5f;
    public float hudY = 0.012f;

    public float menuAlpha() { return menuOpacity / 100f; }
    public float textAlpha() { return textOpacity / 100f; }
    public float radiusPx() { return cornerRounding / 100f * MAX_RADIUS_PX; }

    /** Panel collapsed state, remembered between sessions. */
    public final Map<Category, Boolean> collapsed = new EnumMap<>(Category.class);

    public void reset() {
        scale = 1.0f;
        menuOpacity = 100f;
        textOpacity = 100f;
        cornerRounding = 40f;
        animationSpeed = 1.0f;
        blur = false;
        showTooltips = true;
        compactMode = false;
    }
}
