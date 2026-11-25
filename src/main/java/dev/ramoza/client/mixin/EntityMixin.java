package dev.ramoza.client.mixin;

import dev.ramoza.client.module.player.FreeCam;
import dev.ramoza.client.module.player.NoPush;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Entity.class)
public abstract class EntityMixin {
    /** Entity-vs-entity push: skipped when the local player is either side of it. */
    @Inject(method = "pushAwayFrom", at = @At("HEAD"), cancellable = true)
    private void ramoza$noEntityPush(Entity other, CallbackInfo ci) {
        NoPush m = NoPush.active();
        if (m == null || !m.blocksEntityPush()) return;
        ClientPlayerEntity player = MinecraftClient.getInstance().player;
        if (player != null && ((Object) this == player || other == player)) ci.cancel();
    }

    /** Mouse look: while Free Cam is on it turns the camera, the real player keeps its rotation. */
    @Inject(method = "changeLookDirection", at = @At("HEAD"), cancellable = true)
    private void ramoza$freeCamLook(double cursorDeltaX, double cursorDeltaY, CallbackInfo ci) {
        FreeCam m = FreeCam.active();
        if (m == null) return;
        if ((Object) this == MinecraftClient.getInstance().player) {
            m.look(cursorDeltaX, cursorDeltaY);
            ci.cancel();
        }
    }
}
