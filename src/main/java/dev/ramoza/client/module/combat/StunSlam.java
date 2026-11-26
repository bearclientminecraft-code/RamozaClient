package dev.ramoza.client.module.combat;

import dev.ramoza.client.RamozaClientMod;
import dev.ramoza.client.core.RamozaCore;
import dev.ramoza.client.module.Category;
import dev.ramoza.client.module.MacroModule;
import dev.ramoza.client.setting.BoolSetting;
import dev.ramoza.client.setting.NumberSetting;
import dev.ramoza.client.util.PlayerActions;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.Items;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.text.Text;

/**
 * Your own left click on a shield-blocking target while falling becomes a combo:
 * axe hit (breaks the shield) -> pause -> mace hit -> pause -> back to the original slot.
 * Activate Key: hold to arm (unbound = always armed while the module is on).
 * Needs an axe and a mace in the hotbar, otherwise the click stays a normal attack.
 */
public final class StunSlam extends MacroModule {
    private final NumberSetting minFall = add(new NumberSetting("Min Fall Distance",
            "Blocks you must have fallen before the combo", 1.5, 0, 20, 0.1));

    private final BoolSetting requireShield = add(new BoolSetting("Require Shield",
            "ON: the combo only starts on a target that is blocking with a shield. OFF: axe + mace on any living target", true));
    private final BoolSetting debug = add(new BoolSetting("Debug",
            "Shows in the action bar why a click did NOT start the combo", false));

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
        if (player == null || mc.interactionManager == null) return false;
        if (runner.running()) return refuse(player, "previous combo still running");
        if (!gate()) return refuse(player, "Activate Key not held");
        if (player.isOnGround()) return refuse(player, "you are on the ground");
        if (PlayerActions.fallDistance(player) <= minFall.get()) {
            return refuse(player, "fall distance " + String.format(java.util.Locale.ROOT, "%.1f", PlayerActions.fallDistance(player))
                    + " <= " + minFall.get());
        }

        Entity under = PlayerActions.targetedEntity(mc);
        if (!(under instanceof LivingEntity living) || !living.isAlive()) return refuse(player, "no living target under the crosshair");
        if (requireShield.get() && !isBlocking(living)) return refuse(player, "target is not blocking");

        int axe = -1;
        for (int i = 0; i < 9; i++) {
            if (player.getInventory().getStack(i).isIn(ItemTags.AXES)) {
                axe = i;
                break;
            }
        }
        int mace = PlayerActions.findInHotbar(player, Items.MACE);
        if (axe < 0) return refuse(player, "no axe in the hotbar");
        if (mace < 0) return refuse(player, "no mace in the hotbar");

        target = under;
        axeSlot = axe;
        maceSlot = mace;
        previousSlot = PlayerActions.selectedSlot(player);
        runner.start(sequence);
        return true;
    }

    /** Vanilla blocking check, plus "is holding up a shield" for cases where the blocking delay is not met yet on the client. */
    private static boolean isBlocking(LivingEntity e) {
        return e.isBlocking() || (e.isUsingItem() && e.getActiveItem().isOf(Items.SHIELD));
    }

    private boolean refuse(ClientPlayerEntity player, String why) {
        if (debug.get()) player.sendMessage(Text.literal("Stun Slam: " + why), true);
        return false;
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
