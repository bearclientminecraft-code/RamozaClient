package dev.ramoza.client.gui.theme;

/** An accent theme. {@code tint} is the hue the dark/glass surfaces are built from. */
public final class ThemeDef {
    public final String name;
    public final int primary;
    public final int secondary;
    public final int tint;

    public ThemeDef(String name, int primary, int secondary, int tint) {
        this.name = name;
        this.primary = primary;
        this.secondary = secondary;
        this.tint = tint;
    }

    /** The six built-in themes. */
    public static final ThemeDef[] ALL = {
            new ThemeDef("Amethyst", 0xFF9D6BFF, 0xFFCDB8FF, 0xFF1B1130),
            new ThemeDef("Crimson", 0xFFE8384F, 0xFFFF8A78, 0xFF1D1517),
            new ThemeDef("Ocean", 0xFF3B8CFF, 0xFF2DD4C4, 0xFF0D1B29),
            new ThemeDef("Emerald", 0xFF2ECC85, 0xFFA6E8A8, 0xFF0F1D17),
            new ThemeDef("Sunset", 0xFFFF8A3D, 0xFFFFC857, 0xFF231712),
            new ThemeDef("Rose", 0xFFFF5C9A, 0xFFD946EF, 0xFF26131D)
    };
}
