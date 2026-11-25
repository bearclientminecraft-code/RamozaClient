package dev.ramoza.client.mixin;

import dev.ramoza.client.module.combat.MaceSwap;
import dev.ramoza.client.module.combat.StunSlam;
import dev.ramoza.client.module.combat.Trigger;
import dev.ramoza.client.util.MacroState;
import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MinecraftClient.class)
public abstract class MinecraftClientMixin {
    /** Your own left click: Stun Slam may take it over, Mace Swap may swap the slot before the attack goes out. */
    @Inject(method = "doAttack", at = @At("HEAD"), cancellable = true)
    private void ramoza$onAttack(CallbackInfoReturnable<Boolean> cir) {
        if (MacroState.syntheticAttack) return;
        if (StunSlam.handleAttack()) {
            cir.setReturnValue(true);
            return;
        }
        MaceSwap.handleAttack();
    }

    /** Once per rendered frame, after the crosshair target was refreshed: Trigger runs on frames, not ticks. */
    @Inject(method = "render", at = @At("RETURN"))
    private void ramoza$onFrame(boolean tick, CallbackInfo ci) {
        Trigger.frame((MinecraftClient) (Object) this);
    }
}
