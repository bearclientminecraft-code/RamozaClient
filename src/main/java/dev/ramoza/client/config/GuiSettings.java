package dev.ramoza.client.config;

import dev.ramoza.client.module.Category;

import java.util.EnumMap;
import java.util.Map;

/** Visual interface preferences. Persisted by {@link ConfigManager}. */
public final class GuiSettings {
    public static final float SCALE_MIN = 0.75f, SCALE_MAX = 1.5f;
    public static final float OPACITY_MIN = 0.40f, OPACITY_MAX = 1.0f;
    public static final float SPEED_MIN = 0.5f, SPEED_MAX = 2.0f;
    public static final int CORNERS_MIN = 0, CORNERS_MAX = 10;

    public float scale = 1.0f;
    public float opacity = 1.0f;
    public float animationSpeed = 1.0f;
    public boolean blur = true;
    public float roundedCorners = 6f;
    public boolean showTooltips = true;
    public boolean compactMode = false;

    /** Panel collapsed state, remembered between sessions. */
    public final Map<Category, Boolean> collapsed = new EnumMap<>(Category.class);

    public void reset() {
        scale = 1.0f;
        opacity = 1.0f;
        animationSpeed = 1.0f;
        blur = true;
        roundedCorners = 6f;
        showTooltips = true;
        compactMode = false;
    }
}
