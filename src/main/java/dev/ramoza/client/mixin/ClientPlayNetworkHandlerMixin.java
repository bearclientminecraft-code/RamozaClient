package dev.ramoza.client.mixin;

import dev.ramoza.client.hud.HudStats;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPlayNetworkHandler.class)
public abstract class ClientPlayNetworkHandlerMixin {
    /**
     * Feeds the TPS estimate of the watermark. The handler is first entered on the network thread and re-run on the
     * main thread, so only the main-thread pass is counted. require = 0: if the method name differs in this version
     * the game still starts and TPS simply stays "--".
     */
    @Inject(method = "onWorldTimeUpdate", at = @At("HEAD"), require = 0)
    private void ramoza$timeUpdate(CallbackInfo ci) {
        if (MinecraftClient.getInstance().isOnThread()) HudStats.onWorldTimeUpdate();
    }
}
