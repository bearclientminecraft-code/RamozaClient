package dev.ramoza.client.module.combat;

import dev.ramoza.client.module.Category;
import dev.ramoza.client.module.ClientModule;
import dev.ramoza.client.module.DelayTimer;
import dev.ramoza.client.setting.BoolSetting;
import dev.ramoza.client.setting.NumberSetting;
import dev.ramoza.client.util.PlayerActions;
import dev.ramoza.client.util.TotemUtil;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;

/**
 * You get hit, lose health or are launched upwards (explosion, respawn anchor, wind charge): after
 * Delay +/- Random Delay the Totem Slot is selected, so a totem is in both hands. Only if that slot holds a totem.
 */
public final class AutoDoubleHand extends ClientModule {
    /** Upward speed (blocks/tick) above a normal jump (0.42), so jumping alone never triggers. */
    private static final double LAUNCH_SPEED = 0.6;

    private final NumberSetting totemSlot = add(new NumberSetting("Totem Slot",
            "Hotbar slot (1-9) that is selected when you are hit or launched", 9, 1, 9, 1));
    private final BoolSetting returnSlot = add(new BoolSetting("Return Slot",
            "Go back to the slot you were on once the hits stop", true));
    private final NumberSetting returnDelay = add(new NumberSetting("Return Delay (ms)",
            "How long after the last hit or launch the previous slot is selected again", 500, 0, 3000, 50));
    private final DelayTimer delay = DelayTimer.attach(this, 0);

    private boolean pending;
    private float lastHealth;
    private int lastHurtTime;
    private boolean wasLaunched;
    private int previousSlot = -1; // slot to go back to, -1 = nothing to return
    private long lastTriggerNanos;

    public AutoDoubleHand() {
        super("Auto Double Hand", "When you are hit or launched, switches to the Totem Slot", Category.COMBAT);
    }

    @Override
    protected void onEnable() { pending = false; lastHealth = -1; lastHurtTime = 0; wasLaunched = false; previousSlot = -1; }

    @Override
    public void onTick() {
        MinecraftClient mc = MinecraftClient.getInstance();
        ClientPlayerEntity player = mc.player;
        if (player == null || mc.interactionManager == null) {
            onEnable();
            return;
        }
        float health = player.getHealth();
        boolean hit = player.hurtTime > lastHurtTime || (lastHealth >= 0 && health < lastHealth);
        boolean launched = player.getVelocity().y > LAUNCH_SPEED && !player.isGliding();
        boolean launchEdge = launched && !wasLaunched;
        lastHealth = health;
        lastHurtTime = player.hurtTime;
        wasLaunched = launched;

        if (hit || launchEdge) lastTriggerNanos = System.nanoTime(); // every new hit postpones the return
        if ((hit || launchEdge) && !pending) {
            pending = true;
            delay.restart();
        }
        int target = TotemUtil.hotbarIndex(totemSlot);
        if (!pending) {
            maybeReturn(mc, player, target);
            return;
        }
        if (!delay.elapsed()) return;
        pending = false;
        if (mc.currentScreen != null) return;
        if (PlayerActions.selectedSlot(player) != target && TotemUtil.isTotem(player.getInventory().getStack(target))) {
            previousSlot = PlayerActions.selectedSlot(player);
            PlayerActions.selectSlot(player, target);
        }
    }

    /** Back to the old slot after Return Delay, unless you already moved to another slot yourself. */
    private void maybeReturn(MinecraftClient mc, ClientPlayerEntity player, int target) {
        if (previousSlot < 0) return;
        if (!returnSlot.get() || PlayerActions.selectedSlot(player) != target) {
            previousSlot = -1; // feature off, or the player picked a slot manually: leave it alone
            return;
        }
        if (mc.currentScreen != null) return;
        if ((System.nanoTime() - lastTriggerNanos) / 1_000_000.0 < returnDelay.get()) return;
        PlayerActions.selectSlot(player, previousSlot);
        previousSlot = -1;
    }
}
