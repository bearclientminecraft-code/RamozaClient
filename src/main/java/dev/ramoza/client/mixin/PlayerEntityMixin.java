package dev.ramoza.client.mixin;

import dev.ramoza.client.module.player.NoPush;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerEntity.class)
public abstract class PlayerEntityMixin {
    /** PlayerEntity overrides this, so it has to be hooked here and not in Entity. */
    @Inject(method = "isPushedByFluids", at = @At("HEAD"), cancellable = true)
    private void ramoza$noLiquidPush(CallbackInfoReturnable<Boolean> cir) {
        NoPush m = NoPush.active();
        if (m == null || !m.blocksLiquidPush()) return;
        if ((Object) this == MinecraftClient.getInstance().player) cir.setReturnValue(false);
    }
}
