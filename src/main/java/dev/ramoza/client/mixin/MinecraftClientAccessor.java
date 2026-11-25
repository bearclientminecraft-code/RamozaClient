package dev.ramoza.client.mixin;

import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(MinecraftClient.class)
public interface MinecraftClientAccessor {
    @Accessor("itemUseCooldown")
    void ramoza$setItemUseCooldown(int value);

    /** Vanilla left click (attack / jab / start breaking), exactly as if the attack key was pressed. */
    @Invoker("doAttack")
    boolean ramoza$doAttack();
}
