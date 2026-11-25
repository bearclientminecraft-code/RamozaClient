package dev.ramoza.client.util;

import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import org.lwjgl.glfw.GLFW;

/** Polls the physical keyboard (GLFW) for module hotkeys, independent of vanilla KeyBinding state. */
public final class KeyInput {
    private KeyInput() {}

    /** True while the GLFW key code is held. Also works for mouse buttons (Keys.MOUSE_BASE + button). NONE is never "down". */
    public static boolean isDown(int key) {
        boolean mouse = Keys.isMouse(key);
        if (!mouse && (key < 32 || key > GLFW.GLFW_KEY_LAST)) return false;
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null || mc.getWindow() == null) return false;
        if (mouse) return GLFW.glfwGetMouseButton(mc.getWindow().getHandle(), key - Keys.MOUSE_BASE) == GLFW.GLFW_PRESS;
        return GLFW.glfwGetKey(mc.getWindow().getHandle(), key) == GLFW.GLFW_PRESS;
    }

    /** True while the physical key currently bound to a vanilla binding is held. */
    public static boolean isDown(KeyBinding binding) {
        return isDown(KeyBindingHelper.getBoundKeyOf(binding).getCode());
    }
}
