package dev.ramoza.client.module.combat;

import dev.ramoza.client.module.Category;
import dev.ramoza.client.module.MacroModule;
import dev.ramoza.client.setting.NumberSetting;
import dev.ramoza.client.util.PlayerActions;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.Items;

/**
 * While you fall onto a target under the crosshair: swap to the mace, hit, swap back.
 * Activate Key: hold to arm (unbound = always armed while the module is on). One hit per fall.
 */
public final class AutoMace extends MacroModule {
    private final NumberSetting minFall = add(new NumberSetting("Min Fall Distance",
            "Blocks you must have fallen before the hit (mace smash needs more than 1.5)", 1.5, 0, 20, 0.1));

    private final Runnable[] sequence = {this::selectMace, this::hit, this::restore};
    private int previousSlot;
    private int maceSlot;
    private Entity target;
    private boolean hitThisFall;

    public AutoMace() {
        super("Auto Mace", "Hits the target under the crosshair with a mace while you fall", Category.COMBAT);
    }

    @Override
    protected void onEnable() {
        super.onEnable();
        hitThisFall = false;
    }

    @Override
    protected void tick(MinecraftClient mc) {
        ClientPlayerEntity player = mc.player;
        if (player.isOnGround()) {
            hitThisFall = false;
            return;
        }
        if (hitThisFall || runner.running() || !gate()) return;
        if (player.getVelocity().y >= 0 || PlayerActions.fallDistance(player) <= minFall.get()) return;

        Entity under = PlayerActions.targetedEntity(mc);
        if (!(under instanceof LivingEntity living) || !living.isAlive()) return;
        int mace = PlayerActions.findInHotbar(player, Items.MACE);
        if (mace < 0) return;

        target = under;
        maceSlot = mace;
        previousSlot = PlayerActions.selectedSlot(player);
        hitThisFall = true;
        runner.start(sequence);
    }

    private void selectMace() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player != null) PlayerActions.selectSlot(mc.player, maceSlot);
    }

    private void hit() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player != null && target != null && target.isAlive()) PlayerActions.attack(mc, target);
    }

    private void restore() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player != null) PlayerActions.selectSlot(mc.player, previousSlot);
        target = null;
    }
}
