package dev.ramoza.client.module.player;

import dev.ramoza.client.module.Category;
import dev.ramoza.client.module.ClientModule;
import dev.ramoza.client.module.DelayTimer;
import dev.ramoza.client.setting.NumberSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.DeathScreen;

/** Presses "Respawn" on the death screen for you. Does nothing in hardcore worlds. */
public final class AutoRespawn extends ClientModule {
    private final NumberSetting delay = add(new NumberSetting("Delay",
            "Ticks to wait on the death screen before respawning (20 ticks = 1 second)", 0, 0, 40, 1));
    private final NumberSetting random = DelayTimer.randomSetting(this);
    // Delay is in ticks (50 ms each); Random Delay is +/- milliseconds around that
    private final DelayTimer timer = new DelayTimer(() -> delay.get() * 50.0, random::get);

    private boolean waiting;
    private boolean requested;

    public AutoRespawn() {
        super("Auto Respawn", "Respawns automatically after you die", Category.PLAYER);
    }

    @Override
    protected void onEnable() { reset(); }

    @Override
    public void onTick() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || !(mc.currentScreen instanceof DeathScreen)) {
            reset();
            return;
        }
        if (requested) return; // already asked the server, wait for the respawn packet
        if (mc.world != null && mc.world.getLevelProperties().isHardcore()) return;
        if (!waiting) {
            waiting = true;
            timer.restart(); // new death: roll a fresh wait
        }
        if (!timer.elapsed()) return;
        mc.player.requestRespawn();
        requested = true;
    }

    private void reset() {
        waiting = false;
        requested = false;
    }
}
