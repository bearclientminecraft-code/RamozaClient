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
    private final GuiRoot root;
    private long lastNanos;

    /** @param openKey supplies the GLFW key code currently bound to "Open GUI" (closes the GUI too) */
    public RamozaScreen(RamozaCore core, IntSupplier openKey) {
        super(Text.literal("Ramoza"));
        this.core = core;
        this.openKey = openKey;
        this.root = new GuiRoot(core, new Gfx(renderer));
        this.root.setOnClosed(() -> {
            if (this.client != null) this.client.setScreen(null);
        });
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
        long now = System.nanoTime();
        float dt = lastNanos == 0L ? 1f / 60f : (now - lastNanos) / 1.0e9f;
        lastNanos = now;

        // Real menu blur (vanilla). Cannot be animated, so it is optional (GUI Settings -> Blur).
        if (core.settings.blur && root.fade() > 0.02f) {
            super.renderBackground(context, mouseX, mouseY, deltaTicks);
        }

        renderer.begin(context, this.textRenderer);
        root.render(this.width, this.height, mouseX, mouseY, dt);
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        return root.mouseClicked(click.x(), click.y(), click.button());
    }

    @Override
    public boolean mouseReleased(Click click) {
        return root.mouseReleased(click.x(), click.y(), click.button());
    }

    @Override
    public boolean mouseDragged(Click click, double offsetX, double offsetY) {
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
