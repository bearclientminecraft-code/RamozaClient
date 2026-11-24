package dev.ramoza.client.gui.theme;

public enum Appearance {
    DARK("Dark"),
    LIGHT("Light"),
    GLASS("Liquid Glass");

    private final String label;

    Appearance(String label) { this.label = label; }

    public String label() { return label; }

    public Appearance next(int dir) {
        Appearance[] v = values();
        return v[Math.floorMod(ordinal() + dir, v.length)];
    }
}
