package dev.ramoza.client.util;

import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import org.lwjgl.glfw.GLFW;

/** Polls the physical keyboard (GLFW) for module hotkeys, independent of vanilla KeyBinding state. */
public final class KeyInput {
    private KeyInput() {}

    /** True while the GLFW key code is held. NONE and mouse buttons (codes below 32) are never "down". */
    public static boolean isDown(int key) {
        if (key < 32 || key > GLFW.GLFW_KEY_LAST) return false;
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null || mc.getWindow() == null) return false;
        return GLFW.glfwGetKey(mc.getWindow().getHandle(), key) == GLFW.GLFW_PRESS;
    }

    /** True while the physical key currently bound to a vanilla binding is held. */
    public static boolean isDown(KeyBinding binding) {
        return isDown(KeyBindingHelper.getBoundKeyOf(binding).getCode());
    }
}
