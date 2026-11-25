package dev.ramoza.client.mixin;

import dev.ramoza.client.module.player.FreeCam;
import net.minecraft.client.render.Camera;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Camera.class)
public abstract class CameraMixin {
    @Shadow protected abstract void setPos(double x, double y, double z);

    @Shadow protected abstract void setRotation(float yaw, float pitch);

    /** Runs once per frame after vanilla placed the camera: Free Cam moves it away from the player. */
    @Inject(method = "update", at = @At("TAIL"))
    private void ramoza$freeCam(CallbackInfo ci) {
        FreeCam m = FreeCam.active();
        if (m == null || !m.updateCamera()) return;
        setPos(m.x(), m.y(), m.z());
        setRotation(m.yaw(), m.pitch());
    }

    /** Makes the game draw the player model while the camera is detached. */
    @Inject(method = "isThirdPerson", at = @At("HEAD"), cancellable = true)
    private void ramoza$freeCamThirdPerson(CallbackInfoReturnable<Boolean> cir) {
        if (FreeCam.active() != null) cir.setReturnValue(true);
    }
}
