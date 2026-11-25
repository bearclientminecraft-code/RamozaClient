package dev.ramoza.client.gui;

import dev.ramoza.client.config.GuiSettings;
import dev.ramoza.client.util.MathUtil;

/** Single source of truth for sizes and spacing. Compact mode blends smoothly via {@code density}. */
public final class Metrics {
    public final float density;
    public final int panelW = 120;
    public final int panelGap = 6;
    public final int headerH;
    public final int rowH;
    public final int controlH;
    public final int sliderH;
    public final int optionH;
    public final int pad;
    public final int entryGap = 1;
    public final int topBlockH;
    public final float radius;
    public final float radiusSmall;
    public final float radiusLarge;

    /** @param density 0 = comfortable, 1 = compact */
    public Metrics(GuiSettings s, float density) {
        this.density = density;
        headerH = MathUtil.lerpInt(17, 15, density);
        rowH = MathUtil.lerpInt(14, 12, density);
        controlH = MathUtil.lerpInt(14, 12, density);
        sliderH = MathUtil.lerpInt(22, 19, density);
        optionH = MathUtil.lerpInt(13, 11, density);
        pad = MathUtil.lerpInt(6, 4, density);
        topBlockH = MathUtil.lerpInt(34, 30, density);
        radius = s.radiusPx();
        radiusSmall = Math.max(0f, radius - 2f);
        radiusLarge = Math.min(14f, radius + 2f);
    }
}
