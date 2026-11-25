package dev.ramoza.client.module.combat;

import dev.ramoza.client.RamozaClientMod;
import dev.ramoza.client.core.RamozaCore;
import dev.ramoza.client.module.Category;
import dev.ramoza.client.module.MacroModule;
import dev.ramoza.client.setting.NumberSetting;
import dev.ramoza.client.util.PlayerActions;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.AxeItem;
import net.minecraft.item.Items;

/**
 * Your own left click on a shield-blocking target while falling becomes a combo:
 * axe hit (breaks the shield) -> pause -> mace hit -> pause -> back to the original slot.
 * Activate Key: hold to arm (unbound = always armed while the module is on).
 * Needs an axe and a mace in the hotbar, otherwise the click stays a normal attack.
 */
public final class StunSlam extends MacroModule {
    private final NumberSetting minFall = add(new NumberSetting("Min Fall Distance",
            "Blocks you must have fallen before the combo", 1.5, 0, 20, 0.1));

    private final Runnable[] sequence = {this::axeHit, this::maceHit, this::restore};
    private int previousSlot;
    private int axeSlot;
    private int maceSlot;
    private Entity target;

    public StunSlam() {
        super("Stun Slam", "Axe breaks the shield, then the mace hits, while falling", Category.COMBAT);
    }

    /** Enabled instance or null. */
    public static StunSlam active() {
        RamozaCore core = RamozaClientMod.core();
        if (core == null) return null;
        StunSlam m = core.modules.get(StunSlam.class);
        return m != null && m.isEnabled() ? m : null;
    }

    /** Called by the attack hook. Returns true if the click was taken over (vanilla attack is skipped). */
    public static boolean handleAttack() {
        StunSlam m = active();
        return m != null && m.begin();
    }

    @Override
    protected void tick(MinecraftClient mc) {}

    private boolean begin() {
        MinecraftClient mc = MinecraftClient.getInstance();
        ClientPlayerEntity player = mc.player;
        if (player == null || mc.interactionManager == null || runner.running() || !gate()) return false;
        if (player.isOnGround() || PlayerActions.fallDistance(player) <= minFall.get()) return false;

        Entity under = PlayerActions.targetedEntity(mc);
        if (!(under instanceof LivingEntity living) || !living.isBlocking()) return false;

        int axe = -1;
        for (int i = 0; i < 9; i++) {
            if (player.getInventory().getStack(i).getItem() instanceof AxeItem) {
                axe = i;
                break;
            }
        }
        int mace = PlayerActions.findInHotbar(player, Items.MACE);
        if (axe < 0 || mace < 0) return false;

        target = under;
        axeSlot = axe;
        maceSlot = mace;
        previousSlot = PlayerActions.selectedSlot(player);
        runner.start(sequence);
        return true;
    }

    private void axeHit() { swapAndHit(axeSlot); }

    private void maceHit() { swapAndHit(maceSlot); }

    private void swapAndHit(int slot) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || target == null || !target.isAlive()) return;
        PlayerActions.selectSlot(mc.player, slot);
        PlayerActions.attack(mc, target);
    }

    private void restore() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player != null) PlayerActions.selectSlot(mc.player, previousSlot);
        target = null;
    }
}
