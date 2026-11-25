package dev.ramoza.client.module.player;

import dev.ramoza.client.mixin.LivingEntityAccessor;
import dev.ramoza.client.module.Category;
import dev.ramoza.client.module.ClientModule;
import net.minecraft.client.MinecraftClient;

/** Resets the vanilla jump cooldown every tick so holding jump repeats jumps without the 10 tick pause. */
public final class NoJumpDelay extends ClientModule {
    public NoJumpDelay() {
        super("No Jump Delay", "Removes the delay between jumps while holding the jump key", Category.PLAYER);
    }

    @Override
    public void onTick() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) return;
        ((LivingEntityAccessor) mc.player).ramoza$setJumpingCooldown(0);
    }
}
