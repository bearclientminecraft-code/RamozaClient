package dev.ramoza.client.module.combat;

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
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/**
 * While you are FALLING (never on the ground): equip the mace and hit the target under the crosshair in the
 * same tick the conditions are met, then put the previous slot back.
 * <p>
 * The old version started a delayed swap -> hit sequence; the hit landed a few ticks later, usually after you had
 * touched the ground (fall distance already reset = 0 smash damage). Now the attack is never delayed and is
 * re-checked against {@code isOnGround()} in the very tick it is sent.
 * <p>
 * Hit Chance: rolled ONCE per target per fall (not every tick, which would make any chance below 100 behave like
 * 100 while the target stays under the crosshair). A failed roll skips that target until you land.
 */
public final class AutoMace extends MacroModule {
    private final NumberSetting hitChance = add(new NumberSetting("Hit Chance",
            "Chance (%) that a target gets hit. Rolled once per target per fall", 100, 0, 100, 1));
    private final NumberSetting minFall = add(new NumberSetting("Min Fall Distance",
            "Blocks you must have fallen before the hit (mace smash needs more than 1.5)", 1.5, 0, 20, 0.1));
    private final BoolSetting preEquip = add(new BoolSetting("Pre-Equip",
            "Take the mace out as soon as a living target is below you, so the attack cooldown can recharge before the hit", true));

    private final NumberSetting aimTolerance = add(new NumberSetting("Aim Tolerance",
            "If the crosshair is not exactly on a target, a living entity whose hitbox (grown by this many blocks) is on your "
            + "look line within reach is still hit. 0 = crosshair only", 0.3, 0, 1, 0.05));

    private static final double REACH = 3.0;

    private final Runnable[] restoreSequence = {this::restore};
    private final Map<Integer, Boolean> rolls = new HashMap<>();
    private int swappedFrom = -1;
    private boolean hitThisFall;

    public AutoMace() {
        super("Auto Mace", "Hits the target under the crosshair with a mace while you fall", Category.COMBAT);
    }

    @Override
    protected void onEnable() {
        super.onEnable();
        hitThisFall = false;
        rolls.clear();
        swappedFrom = -1;
    }

    @Override
    protected void onDisable() {
        super.onDisable();
        restore();
    }

    @Override
    protected void tick(MinecraftClient mc) {
        ClientPlayerEntity player = mc.player;
        if (player.isOnGround()) {
            hitThisFall = false;
            rolls.clear();
            if (!runner.running()) restore();
            return;
        }
        if (player.getVelocity().y > 0) {
            // rising again (wind charge, bounce, jump): a new fall starts, so the previous fall's lock must not stay
            hitThisFall = false;
            rolls.clear();
        }
        if (hitThisFall || runner.running() || !gate()) return;
        if (player.getVelocity().y >= 0 || PlayerActions.fallDistance(player) <= minFall.get()) return;

        int mace = PlayerActions.findInHotbar(player, Items.MACE);
        if (mace < 0) return;

        Entity under = findTarget(mc, player);
        boolean hasTarget = under != null;

        if (preEquip.get() && PlayerActions.selectedSlot(player) != mace && (hasTarget || targetBelow(mc, player))) {
            equip(player, mace);
        }
        if (!hasTarget) return;

        boolean allowed = rolls.computeIfAbsent(under.getId(),
                id -> ThreadLocalRandom.current().nextDouble(100.0) < hitChance.get());
        if (!allowed) return;

        if (PlayerActions.selectedSlot(player) != mace) equip(player, mace);
        if (player.isOnGround()) return; // last check right before the packet: never swing from the ground
        PlayerActions.attack(mc, under);
        hitThisFall = true;
        runner.startDelayed(restoreSequence); // slot goes back on a later tick, never in the attack tick
    }

    /** Crosshair entity first; otherwise the nearest living entity on the look line (hitbox grown by Aim Tolerance). */
    private Entity findTarget(MinecraftClient mc, ClientPlayerEntity player) {
        Entity c = PlayerActions.targetedEntity(mc);
        if (c instanceof LivingEntity l && l.isAlive() && !l.isSpectator()) return c;
        double tol = aimTolerance.get();
        if (tol <= 0.0 || mc.world == null) return null;

        Vec3d eye = player.getEyePos();
        Vec3d end = eye.add(player.getRotationVec(1.0f).multiply(REACH));
        Box search = new Box(eye, end).expand(tol + 0.5);
        Entity best = null;
        double bestDist = Double.MAX_VALUE;
        for (LivingEntity e : mc.world.getEntitiesByClass(LivingEntity.class, search,
                x -> x != player && x.isAlive() && !x.isSpectator())) {
            Box box = e.getBoundingBox().expand(tol);
            Vec3d hit = box.contains(eye) ? eye : box.raycast(eye, end).orElse(null);
            if (hit == null) continue;
            double d = hit.squaredDistanceTo(eye);
            if (d < bestDist) {
                bestDist = d;
                best = e;
            }
        }
        return best;
    }

    private void equip(ClientPlayerEntity player, int mace) {
        if (swappedFrom < 0) swappedFrom = PlayerActions.selectedSlot(player);
        PlayerActions.selectSlot(player, mace);
    }

    private boolean targetBelow(MinecraftClient mc, ClientPlayerEntity player) {
        if (mc.world == null) return false;
        Box zone = player.getBoundingBox().expand(2.0, 0.0, 2.0).stretch(0.0, -6.0, 0.0);
        return !mc.world.getEntitiesByClass(LivingEntity.class, zone,
                e -> e != player && e.isAlive() && !e.isSpectator()).isEmpty();
    }

    private void restore() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player != null && swappedFrom >= 0) PlayerActions.selectSlot(mc.player, swappedFrom);
        swappedFrom = -1;
    }
}
