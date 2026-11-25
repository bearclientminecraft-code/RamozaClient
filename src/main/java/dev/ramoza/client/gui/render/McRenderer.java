package dev.ramoza.client.gui.render;

import net.minecraft.client.font.TextRenderer;
import dev.ramoza.client.RamozaClientMod;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.Identifier;

import java.util.HashMap;
import java.util.Map;

/**
 * The only place where the GUI touches Minecraft's rendering API (Yarn, 1.21.11).
 * In 1.21.6+ DrawContext#getMatrices() is a 2D JOML Matrix3x2fStack.
 */
public final class McRenderer implements Renderer2D {
    private static final int CACHE_LIMIT = 2048;

    private DrawContext context;
    private TextRenderer font;
    private final Map<String, Integer> widthInter = new HashMap<>();
    private final Map<String, Integer> widthWatermark = new HashMap<>();
    private final Map<String, Identifier> textures = new HashMap<>();

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
    public void drawText(String text, int x, int y, int argb, boolean shadow, UiFont f) {
        context.drawText(font, Fonts.text(text, f), x, y, argb, shadow);
    }

    @Override
    public int textWidth(String text, UiFont f) {
        Map<String, Integer> cache = f == UiFont.WATERMARK ? widthWatermark : widthInter;
        Integer w = cache.get(text);
        if (w == null) {
            if (cache.size() >= CACHE_LIMIT) cache.clear();
            w = font.getWidth(Fonts.text(text, f));
            cache.put(text, w);
        }
        return w;
    }

    @Override
    public void drawTexture(String name, int x, int y, int size, int argb) {
        Identifier id = textures.computeIfAbsent(name,
                n -> Identifier.of(RamozaClientMod.MOD_ID, "textures/gui/" + n + ".png"));
        // textures are 64x64; the region is scaled down to size x size
        context.drawTexture(RenderPipelines.GUI_TEXTURED, id, x, y, 0f, 0f, size, size, 64, 64, 64, 64, argb);
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
