package dev.ramoza.client.gui.theme;

import dev.ramoza.client.util.ColorUtil;

/** Centralised color variables. Every component reads colors from here, never hard-coded. */
public final class Palette {
    public enum Slot {
        PRIMARY, SECONDARY,
        BACKGROUND, PANEL, PANEL_HEADER,
        HOVER, ACTIVE, BORDER,
        TEXT, TEXT_MUTED, TEXT_DIM,
        TOGGLE_ON, TOGGLE_OFF, TOGGLE_KNOB,
        SLIDER_TRACK, SLIDER_FILL, SLIDER_KNOB,
        SHADOW, GLOSS, TOOLTIP, FIELD
    }

    private static final Slot[] SLOTS = Slot.values();
    private final int[] v = new int[SLOTS.length];

    public int get(Slot s) { return v[s.ordinal()]; }
    public void set(Slot s, int color) { v[s.ordinal()] = color; }

    public Palette copy() {
        Palette p = new Palette();
        System.arraycopy(v, 0, p.v, 0, v.length);
        return p;
    }

    public void copyFrom(Palette o) { System.arraycopy(o.v, 0, v, 0, v.length); }

    /** Moves every slot towards {@code target} by fraction t. */
    public void approach(Palette target, float t) {
        for (int i = 0; i < v.length; i++) v[i] = ColorUtil.approach(v[i], target.v[i], t);
    }

    public int primary() { return get(Slot.PRIMARY); }
    public int secondary() { return get(Slot.SECONDARY); }
    public int background() { return get(Slot.BACKGROUND); }
    public int panel() { return get(Slot.PANEL); }
    public int header() { return get(Slot.PANEL_HEADER); }
    public int hover() { return get(Slot.HOVER); }
    public int active() { return get(Slot.ACTIVE); }
    public int border() { return get(Slot.BORDER); }
    public int text() { return get(Slot.TEXT); }
    public int muted() { return get(Slot.TEXT_MUTED); }
    public int dim() { return get(Slot.TEXT_DIM); }
    public int toggleOn() { return get(Slot.TOGGLE_ON); }
    public int toggleOff() { return get(Slot.TOGGLE_OFF); }
    public int toggleKnob() { return get(Slot.TOGGLE_KNOB); }
    public int sliderTrack() { return get(Slot.SLIDER_TRACK); }
    public int sliderFill() { return get(Slot.SLIDER_FILL); }
    public int sliderKnob() { return get(Slot.SLIDER_KNOB); }
    public int shadow() { return get(Slot.SHADOW); }
    public int gloss() { return get(Slot.GLOSS); }
    public int tooltip() { return get(Slot.TOOLTIP); }
    public int field() { return get(Slot.FIELD); }
}
