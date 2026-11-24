package dev.ramoza.client.gui.render;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;

/**
 * The only place where the GUI touches Minecraft's rendering API (Yarn, 1.21.11).
 * In 1.21.6+ DrawContext#getMatrices() is a 2D JOML Matrix3x2fStack.
 */
public final class McRenderer implements Renderer2D {
    private DrawContext context;
    private TextRenderer font;

    public void begin(DrawContext context, TextRenderer font) {
        this.context = context;
        this.font = font;
    }

    @Override
    public void fillRect(int x, int y, int w, int h, int argb) {
        context.fill(x, y, x + w, y + h, argb);
    }

    @Override
    public void fillGradientV(int x, int y, int w, int h, int topArgb, int bottomArgb) {
        context.fillGradient(x, y, x + w, y + h, topArgb, bottomArgb);
    }

    @Override
    public void drawText(String text, int x, int y, int argb, boolean shadow) {
        context.drawText(font, text, x, y, argb, shadow);
    }

    @Override
    public int textWidth(String text) {
        return font.getWidth(text);
    }

    @Override
    public int fontHeight() {
        return 9;
    }

    @Override
    public void pushScissor(int x, int y, int w, int h) {
        context.enableScissor(x, y, x + w, y + h);
    }

    @Override
    public void popScissor() {
        context.disableScissor();
    }

    @Override
    public void pushTransform(float tx, float ty, float sx, float sy) {
        context.getMatrices().pushMatrix();
        context.getMatrices().translate(tx, ty);
        context.getMatrices().scale(sx, sy);
    }

    @Override
    public void popTransform() {
        context.getMatrices().popMatrix();
    }
}
