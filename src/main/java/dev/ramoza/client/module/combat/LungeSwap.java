package dev.ramoza.client.module.combat;

import dev.ramoza.client.mixin.MinecraftClientAccessor;
import dev.ramoza.client.module.Category;
import dev.ramoza.client.module.MacroModule;
import dev.ramoza.client.setting.BoolSetting;
import dev.ramoza.client.setting.ModeSetting;
import dev.ramoza.client.setting.NumberSetting;
import dev.ramoza.client.util.MacroState;
import dev.ramoza.client.util.PlayerActions;
import net.minecraft.client.MinecraftClient;

/**
 * Activate Key: switch to the spear slot, fire the spear's left-click attack (the jab that triggers Lunge),
 * then after Delay +/- Random Delay switch back to the hand slot. The attack is vanilla's own click
 * ({@code MinecraftClient.doAttack}), so whatever the spear and its enchantments do in 1.21.11 happens normally.
 */
public final class LungeSwap extends MacroModule {
    private final NumberSetting handSlot = add(new NumberSetting("Hand Slot",
            "Hotbar slot (1-9) you want to end up on", 9, 1, 9, 1));
    private final NumberSetting spearSlot = add(new NumberSetting("Spear Slot",
            "Hotbar slot (1-9) with the spear", 3, 1, 9, 1));

    private final ModeSetting mode = add(new ModeSetting("Mode",
            "Press: one lunge per key press. Hold: keeps lunging every tick while the key is held", "Press", "Press", "Hold"));
    private final BoolSetting noDelay = add(new BoolSetting("No Lunge Delay",
            "Ignore Delay / Random Delay / Humanize: swap, lunge and swap back in the same tick, and every key press fires at once", false));

    private final Runnable[] sequence = {this::lunge, this::restore};
    private boolean queuedPress;

    public LungeSwap() {
        super("Lunge Swap", "Key: switch to spear, lunge, switch back to the hand slot", Category.COMBAT);
    }

    @Override
    protected void onEnable() {
        super.onEnable();
        queuedPress = false;
    }

    @Override
    protected void tick(MinecraftClient mc) {
        boolean hold = mode.get().equals("Hold");
        if (noDelay.get()) {
            queuedPress = false;
            boolean trigger = hold ? (activateKey.isBound() && gate()) : pressed();
            if (!trigger) return;
            runner.flush(); // finish a sequence left over from before the option was turned on
            lunge();
            restore();
            return;
        }

        // Delayed mode: the spear is held for a few ticks, and every slot change resets the attack cooldown.
        // A jab with a recharging cooldown does not lunge, so a new cycle starts only once it is full again
        // (Press mode remembers a press made meanwhile and fires it as soon as that is the case).
        if (hold) queuedPress = false;
        else if (pressed()) queuedPress = true;
        boolean want = hold ? (activateKey.isBound() && gate()) : queuedPress;
        if (!want || runner.running()) return;
        if (mc.player.getAttackCooldownProgress(0.0f) < 1.0f) return;
        queuedPress = false;
        runner.start(sequence);
    }

    private void lunge() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) return;
        PlayerActions.selectSlot(mc.player, spearSlot.get().intValue() - 1);
        MacroState.syntheticAttack = true;
        try {
            ((MinecraftClientAccessor) mc).ramoza$doAttack();
        } finally {
            MacroState.syntheticAttack = false;
        }
    }

    private void restore() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player != null) PlayerActions.selectSlot(mc.player, handSlot.get().intValue() - 1);
    }
}
