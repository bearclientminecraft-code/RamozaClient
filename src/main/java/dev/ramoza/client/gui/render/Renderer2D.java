package dev.ramoza.client.gui.render;

/**
 * Minimal drawing backend. The whole GUI is written against this interface, so the only
 * Minecraft-specific rendering code lives in {@code McRenderer}.
 */
public interface Renderer2D {
    void fillRect(int x, int y, int w, int h, int argb);

    void fillGradientV(int x, int y, int w, int h, int topArgb, int bottomArgb);

    void drawText(String text, int x, int y, int argb, boolean shadow);

    int textWidth(String text);

    int fontHeight();

    void pushScissor(int x, int y, int w, int h);

    void popScissor();

    /** Translate by (tx, ty), then scale by (sx, sy). */
    void pushTransform(float tx, float ty, float sx, float sy);

    void popTransform();
}
