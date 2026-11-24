package dev.ramoza.client.gui.theme;

import dev.ramoza.client.gui.theme.Palette.Slot;
import dev.ramoza.client.util.ColorUtil;

import static dev.ramoza.client.util.ColorUtil.BLACK;
import static dev.ramoza.client.util.ColorUtil.WHITE;
import static dev.ramoza.client.util.ColorUtil.mix;
import static dev.ramoza.client.util.ColorUtil.withAlpha;

/** Builds the full color set for a (theme, appearance) pair. */
public final class ThemeFactory {
    private ThemeFactory() {}

    /** @param opacity multiplier for the panel surfaces (GUI Settings -> Opacity) */
    public static Palette build(ThemeDef t, Appearance a, float opacity) {
        Palette p = new Palette();
        switch (a) {
            case LIGHT: light(p, t); break;
            case GLASS: glass(p, t); break;
            default: dark(p, t); break;
        }
        p.set(Slot.PANEL, ColorUtil.mulAlpha(p.get(Slot.PANEL), opacity));
        p.set(Slot.PANEL_HEADER, ColorUtil.mulAlpha(p.get(Slot.PANEL_HEADER), opacity));
        return p;
    }

    private static void common(Palette p, int primary, int secondary) {
        p.set(Slot.PRIMARY, primary);
        p.set(Slot.SECONDARY, secondary);
        p.set(Slot.TOGGLE_ON, primary);
        p.set(Slot.SLIDER_FILL, primary);
        p.set(Slot.TOGGLE_KNOB, WHITE);
        p.set(Slot.SLIDER_KNOB, WHITE);
        p.set(Slot.ACTIVE, withAlpha(primary, 0x2E));
    }

    private static void dark(Palette p, ThemeDef t) {
        common(p, t.primary, t.secondary);
        int tint = t.tint;
        p.set(Slot.BACKGROUND, withAlpha(mix(tint, BLACK, 0.55f), 0xA0));
        p.set(Slot.PANEL, withAlpha(mix(tint, BLACK, 0.10f), 0xEE));
        p.set(Slot.PANEL_HEADER, withAlpha(mix(tint, WHITE, 0.06f), 0xF6));
        p.set(Slot.HOVER, withAlpha(WHITE, 0x14));
        p.set(Slot.BORDER, withAlpha(mix(tint, WHITE, 0.24f), 0x9A));
        p.set(Slot.TEXT, 0xFFF2F2F7);
        p.set(Slot.TEXT_MUTED, 0xFFA9A9BB);
        p.set(Slot.TEXT_DIM, 0xFF6F6F83);
        p.set(Slot.TOGGLE_OFF, mix(tint, WHITE, 0.20f));
        p.set(Slot.SLIDER_TRACK, withAlpha(WHITE, 0x22));
        p.set(Slot.SHADOW, withAlpha(BLACK, 0x80));
        p.set(Slot.GLOSS, withAlpha(WHITE, 0x0E));
        p.set(Slot.TOOLTIP, withAlpha(mix(tint, BLACK, 0.30f), 0xF4));
        p.set(Slot.FIELD, withAlpha(BLACK, 0x55));
    }

    private static void light(Palette p, ThemeDef t) {
        int primary = mix(t.primary, BLACK, 0.14f);
        int secondary = mix(t.secondary, BLACK, 0.12f);
        common(p, primary, secondary);
        p.set(Slot.BACKGROUND, withAlpha(0xFF0B0B14, 0x40));
        p.set(Slot.PANEL, withAlpha(mix(WHITE, primary, 0.03f), 0xF5));
        p.set(Slot.PANEL_HEADER, withAlpha(mix(WHITE, primary, 0.10f), 0xFA));
        p.set(Slot.HOVER, withAlpha(0xFF14142A, 0x0E));
        p.set(Slot.ACTIVE, withAlpha(primary, 0x26));
        p.set(Slot.BORDER, withAlpha(0xFFB9B9CB, 0xB0));
        p.set(Slot.TEXT, 0xFF1B1B26);
        p.set(Slot.TEXT_MUTED, 0xFF5A5A6E);
        p.set(Slot.TEXT_DIM, 0xFF9A9AAC);
        p.set(Slot.TOGGLE_OFF, 0xFFCDCDDA);
        p.set(Slot.SLIDER_TRACK, 0xFFDBDBE6);
        p.set(Slot.SHADOW, withAlpha(0xFF20202C, 0x48));
        p.set(Slot.GLOSS, withAlpha(WHITE, 0x60));
        p.set(Slot.TOOLTIP, withAlpha(0xFF23232F, 0xF4));
        p.set(Slot.FIELD, withAlpha(0xFF14142A, 0x0C));
    }

    private static void glass(Palette p, ThemeDef t) {
        common(p, t.primary, t.secondary);
        int tint = t.tint;
        p.set(Slot.ACTIVE, withAlpha(t.primary, 0x44));
        p.set(Slot.BACKGROUND, withAlpha(mix(tint, BLACK, 0.40f), 0x70));
        p.set(Slot.PANEL, withAlpha(mix(tint, WHITE, 0.18f), 0x66));
        p.set(Slot.PANEL_HEADER, withAlpha(mix(tint, WHITE, 0.28f), 0x7C));
        p.set(Slot.HOVER, withAlpha(WHITE, 0x1E));
        p.set(Slot.BORDER, withAlpha(WHITE, 0x48));
        p.set(Slot.TEXT, 0xFFFFFFFF);
        p.set(Slot.TEXT_MUTED, 0xFFD2D2E2);
        p.set(Slot.TEXT_DIM, 0xFF9E9EB6);
        p.set(Slot.TOGGLE_OFF, withAlpha(WHITE, 0x40));
        p.set(Slot.SLIDER_TRACK, withAlpha(WHITE, 0x30));
        p.set(Slot.SHADOW, withAlpha(BLACK, 0x50));
        p.set(Slot.GLOSS, withAlpha(WHITE, 0x2A));
        p.set(Slot.TOOLTIP, withAlpha(mix(tint, BLACK, 0.30f), 0xE6));
        p.set(Slot.FIELD, withAlpha(WHITE, 0x16));
    }
}
