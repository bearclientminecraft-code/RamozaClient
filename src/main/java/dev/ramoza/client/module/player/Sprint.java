package dev.ramoza.client.module.player;

import dev.ramoza.client.module.Category;
import dev.ramoza.client.module.ClientModule;
import net.minecraft.client.MinecraftClient;

/**
 * Holds the vanilla sprint key while the forward key is held. All vanilla sprint conditions
 * (hunger, sneaking, using an item, blindness...) are still checked by the game itself.
 */
public final class Sprint extends ClientModule {
    public Sprint() {
        super("Sprint", "Sprints automatically while you move forward", Category.PLAYER);
    }

    @Override
    public void onTick() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.currentScreen != null) return;
        if (mc.options.forwardKey.isPressed()) mc.options.sprintKey.setPressed(true);
    }

    @Override
    protected void onDisable() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc != null && mc.options != null) mc.options.sprintKey.setPressed(false);
    }
}
