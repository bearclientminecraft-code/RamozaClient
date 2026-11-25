package dev.ramoza.client.gui.theme;

/** An accent theme. {@code tint} is the hue the dark/glass surfaces are built from. */
public final class ThemeDef {
    public final String name;
    public final int primary;
    public final int secondary;
    public final int tint;
    /** Explicit surface colors (0 = derive from {@code tint}). Used by the dark appearance. */
    public final int background;
    public final int panel;

    public ThemeDef(String name, int primary, int secondary, int tint) {
        this(name, primary, secondary, tint, 0, 0);
    }

    /** Theme with explicit Accent / Secondary / Background / Panel colors. */
    public static ThemeDef of(String name, int accent, int secondary, int background, int panel) {
        return new ThemeDef(name, accent, secondary, panel, background, panel);
    }

    private ThemeDef(String name, int primary, int secondary, int tint, int background, int panel) {
        this.name = name;
        this.primary = primary;
        this.secondary = secondary;
        this.tint = tint;
        this.background = background;
        this.panel = panel;
    }

    public boolean hasSurfaces() { return panel != 0; }

    /** The built-in themes. */
    public static final ThemeDef[] ALL = {
            new ThemeDef("Amethyst", 0xFF9D6BFF, 0xFFCDB8FF, 0xFF1B1130),
            new ThemeDef("Crimson", 0xFFE8384F, 0xFFFF8A78, 0xFF1D1517),
            new ThemeDef("Ocean", 0xFF3B8CFF, 0xFF2DD4C4, 0xFF0D1B29),
            new ThemeDef("Emerald", 0xFF2ECC85, 0xFFA6E8A8, 0xFF0F1D17),
            new ThemeDef("Sunset", 0xFFFF8A3D, 0xFFFFC857, 0xFF231712),
            new ThemeDef("Rose", 0xFFFF5C9A, 0xFFD946EF, 0xFF26131D),
            ThemeDef.of("Midnight", 0xFF5865F2, 0xFF8994FF, 0xFF0B0D18, 0xFF15182A),
            ThemeDef.of("Blood Moon", 0xFFD72638, 0xFFFF5364, 0xFF12090C, 0xFF211116),
            ThemeDef.of("Frozen", 0xFF70CFFF, 0xFFB4E9FF, 0xFF09131B, 0xFF132431),
            ThemeDef.of("Gold", 0xFFD6AD55, 0xFFF3D58A, 0xFF110F0A, 0xFF211C12)
    };
}
