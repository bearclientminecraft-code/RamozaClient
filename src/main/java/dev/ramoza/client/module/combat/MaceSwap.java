package dev.ramoza.client.module.combat;

import dev.ramoza.client.RamozaClientMod;
import dev.ramoza.client.core.RamozaCore;
import dev.ramoza.client.module.Category;
import dev.ramoza.client.module.MacroModule;
import dev.ramoza.client.setting.NumberSetting;
import dev.ramoza.client.util.PlayerActions;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.item.Items;

/**
 * Your own left click on an entity while falling: the hotbar switches to the mace first (the slot packet goes
 * out before the attack packet), and switches back after Delay +/- Random Delay.
 * Activate Key: hold to arm (unbound = always armed while the module is on).
 */
public final class MaceSwap extends MacroModule {
    private final NumberSetting minFall = add(new NumberSetting("Min Fall Distance",
            "Blocks you must have fallen before the swap", 1.5, 0, 20, 0.1));

    private final Runnable[] sequence = {this::restore};
    private int previousSlot;

    public MaceSwap() {
        super("Mace Swap", "Swaps to the mace when you attack while falling", Category.COMBAT);
    }

    /** Enabled instance or null. */
    public static MaceSwap active() {
        RamozaCore core = RamozaClientMod.core();
        if (core == null) return null;
        MaceSwap m = core.modules.get(MaceSwap.class);
        return m != null && m.isEnabled() ? m : null;
    }

    /** Called by the attack hook before vanilla handles the click. */
    public static void handleAttack() {
        MaceSwap m = active();
        if (m != null) m.beforeAttack();
    }

    @Override
    protected void tick(MinecraftClient mc) {}

    private void beforeAttack() {
        MinecraftClient mc = MinecraftClient.getInstance();
        ClientPlayerEntity player = mc.player;
        if (player == null || mc.interactionManager == null || runner.running() || !gate()) return;
        if (player.isOnGround() || PlayerActions.fallDistance(player) <= minFall.get()) return;
        if (PlayerActions.targetedEntity(mc) == null) return;

        int mace = PlayerActions.findInHotbar(player, Items.MACE);
        int current = PlayerActions.selectedSlot(player);
        if (mace < 0 || mace == current) return;

        previousSlot = current;
        PlayerActions.selectSlot(player, mace);
        runner.startDelayed(sequence); // the attack itself is vanilla's, we only switch back later
    }

    private void restore() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player != null) PlayerActions.selectSlot(mc.player, previousSlot);
    }
}
