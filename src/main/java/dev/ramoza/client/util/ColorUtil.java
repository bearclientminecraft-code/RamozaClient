package dev.ramoza.client.util;

/** ARGB helpers. All colors are 0xAARRGGBB. */
public final class ColorUtil {
    public static final int WHITE = 0xFFFFFFFF;
    public static final int BLACK = 0xFF000000;
    public static final int CLEAR = 0x00000000;

    private ColorUtil() {}

    public static int alpha(int c) { return c >>> 24; }
    public static int red(int c) { return (c >> 16) & 0xFF; }
    public static int green(int c) { return (c >> 8) & 0xFF; }
    public static int blue(int c) { return c & 0xFF; }

    public static int argb(int a, int r, int g, int b) {
        return ((a & 0xFF) << 24) | ((r & 0xFF) << 16) | ((g & 0xFF) << 8) | (b & 0xFF);
    }

    public static int withAlpha(int c, int a) {
        return ((a & 0xFF) << 24) | (c & 0x00FFFFFF);
    }

    /** Multiplies the alpha channel by f (0..1). */
    public static int mulAlpha(int c, float f) {
        int a = Math.round(alpha(c) * MathUtil.saturate(f));
        return withAlpha(c, a);
    }

    /** Per-channel linear interpolation including alpha. */
    public static int mix(int a, int b, float t) {
        t = MathUtil.saturate(t);
        if (t <= 0f) return a;
        if (t >= 1f) return b;
        return argb(
                Math.round(alpha(a) + (alpha(b) - alpha(a)) * t),
                Math.round(red(a) + (red(b) - red(a)) * t),
                Math.round(green(a) + (green(b) - green(a)) * t),
                Math.round(blue(a) + (blue(b) - blue(a)) * t));
    }

    /** Mixes RGB only, keeps the alpha of {@code a}. */
    public static int mixRgb(int a, int b, float t) {
        return withAlpha(mix(a, b, t), alpha(a));
    }

    /**
     * Moves each channel of {@code cur} towards {@code target} by fraction t, always making
     * progress of at least one step so the value is guaranteed to converge.
     */
    public static int approach(int cur, int target, float t) {
        if (cur == target) return cur;
        return argb(
                step(alpha(cur), alpha(target), t),
                step(red(cur), red(target), t),
                step(green(cur), green(target), t),
                step(blue(cur), blue(target), t));
    }

    private static int step(int c, int tgt, float t) {
        int d = tgt - c;
        if (d == 0) return c;
        int s = Math.round(d * t);
        if (s == 0) s = d > 0 ? 1 : -1;
        if (Math.abs(s) > Math.abs(d)) s = d;
        return c + s;
    }
}
