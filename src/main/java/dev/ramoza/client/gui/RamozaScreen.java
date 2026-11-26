package dev.ramoza.client.gui;

import dev.ramoza.client.core.RamozaCore;
import dev.ramoza.client.gui.render.Gfx;
import dev.ramoza.client.gui.render.McRenderer;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.input.CharInput;
import net.minecraft.client.input.KeyInput;
import net.minecraft.text.Text;

import java.util.function.IntSupplier;

/** Thin Minecraft adapter: forwards rendering and input to {@link GuiRoot}. */
public final class RamozaScreen extends Screen {
    private final RamozaCore core;
    private final IntSupplier openKey;
    private final McRenderer renderer = new McRenderer();
    private final Gfx gfx = new Gfx(renderer);
    private final GuiRoot root;
    private long lastNanos;

    /** @param openKey supplies the GLFW key code currently bound to "Open GUI" (closes the GUI too) */
    public RamozaScreen(RamozaCore core, IntSupplier openKey) {
        super(Text.literal("Ramoza"));
        this.core = core;
        this.openKey = openKey;
        this.root = new GuiRoot(core, gfx);
        this.root.setOnClosed(() -> {
            if (this.client != null) this.client.setScreen(null);
        });
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
        long now = System.nanoTime();
        float dt = lastNanos == 0L ? 1f / 60f : (now - lastNanos) / 1.0e9f;
        lastNanos = now;

        renderer.begin(context, this.textRenderer);
        root.render(this.width, this.height, mouseX, mouseY, dt);

        // the watermark is drawn on top of the GUI backdrop so it can be seen and dragged
        if (!core.isGuiHidden()) core.watermark.render(gfx, this.width, this.height, root.fade(), mouseX, mouseY);
    }

    /**
     * Vanilla (1.21.6+) calls renderBackground() itself before render(); calling it again from render()
     * crashed with "Can only blur once per frame". We override it so blur follows GUI Settings -> Blur.
     */
    @Override
    public void renderBackground(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
        if (core.settings.blur && root.fade() > 0.02f) {
            super.renderBackground(context, mouseX, mouseY, deltaTicks);
        }
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        if (click.button() == 0 && !root.isClosing() && core.watermark.beginDrag(click.x(), click.y())) return true;
        return root.mouseClicked(click.x(), click.y(), click.button());
    }

    @Override
    public boolean mouseReleased(Click click) {
        if (core.watermark.endDrag()) {
            core.save();
            return true;
        }
        return root.mouseReleased(click.x(), click.y(), click.button());
    }

    @Override
    public boolean mouseDragged(Click click, double offsetX, double offsetY) {
        if (core.watermark.dragging()) {
            core.watermark.drag(click.x(), click.y(), this.width, this.height);
            return true;
        }
        return root.mouseDragged(click.x(), click.y());
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        return root.mouseScrolled(mouseX, mouseY, verticalAmount);
    }

    @Override
    public boolean keyPressed(KeyInput input) {
        int key = input.key();
        if (!root.isCapturing() && key == openKey.getAsInt()) {
            root.requestClose();
            return true;
        }
        if (root.keyPressed(key, input.hasCtrl(), input.hasShift())) return true;
        return super.keyPressed(input);
    }

    @Override
    public boolean charTyped(CharInput input) {
        if (root.charTyped(input.codepoint())) return true;
        return super.charTyped(input);
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false; // Esc is handled by GuiRoot so the close animation can play
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    @Override
    public void removed() {
        core.save();
        super.removed();
    }
}
