package dev.ramoza.client.module.combat;

import dev.ramoza.client.module.Category;
import dev.ramoza.client.module.ClientModule;
import dev.ramoza.client.setting.BoolSetting;
import dev.ramoza.client.setting.ModeSetting;
import dev.ramoza.client.setting.NumberSetting;
import dev.ramoza.client.util.PlayerActions;
import dev.ramoza.client.util.Timing;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Crystal clicker. Left click = hit the end crystal under the crosshair, right click = place a crystal on the
 * obsidian / bedrock block under the crosshair (needs a crystal in a hand).
 * <ul>
 *   <li><b>Hold</b>: active while you hold the right mouse button.</li>
 *   <li><b>Auto</b>: becomes active when you aim at a crystal standing on obsidian / bedrock and stays active while
 *       the crosshair is on that crystal or its block, so it keeps hitting and re-placing by itself.</li>
 * </ul>
 * Blast Delay (+ Random) is the wait between aiming at a crystal and hitting it, Delay (+ Random) the wait between
 * two placements. Min/Max CPS limit both actions (one action of each kind per tick, so at most 20 per second).
 * Self Damage Check pauses everything while you eat, drink or draw a bow / trident.
 */
public final class AutoHitCrystal extends ClientModule {
    private final ModeSetting mode = add(new ModeSetting("Mode",
            "Hold: while the right mouse button is held. Auto: starts by itself when you aim at a crystal on obsidian",
            "Hold", "Hold", "Auto"));
    private final NumberSetting minCps = add(new NumberSetting("Min CPS",
            "Lowest clicks per second", 8, 1, 30, 1));
    private final NumberSetting maxCps = add(new NumberSetting("Max CPS",
            "Highest clicks per second. Every click picks a rate between Min and Max", 12, 1, 30, 1));
    private final NumberSetting delay = add(new NumberSetting("Delay",
            "Milliseconds between two crystal placements", 0, 0, 500, 5));
    private final NumberSetting randomDelay = add(new NumberSetting("Random Delay",
            "Delay is shifted by a random value in +/- this many ms", 0, 0, 200, 5));
    private final NumberSetting screenJitter = add(new NumberSetting("Screen Jitter",
            "Random view shake (degrees) after every click. 0 = off", 0, 0, 3, 0.1));
    private final NumberSetting blastDelay = add(new NumberSetting("Blast Delay",
            "Milliseconds between aiming at a crystal and hitting it", 0, 0, 500, 5));
    private final NumberSetting blastRandom = add(new NumberSetting("Blast Random Delay",
            "Blast Delay is shifted by a random value in +/- this many ms", 0, 0, 200, 5));
    private final BoolSetting selfDamageCheck = add(new BoolSetting("Self Damage Check",
            "Do not hit or place while you eat, drink or draw a bow / trident", true));

    private boolean armed;
    private long blastDueMs = -1;
    private long nextAttackMs;
    private long nextPlaceMs;

    public AutoHitCrystal() {
        super("Auto Hit Crystal", "Hits crystals you aim at and places new ones on obsidian", Category.COMBAT);
    }

    @Override
    protected void onEnable() {
        armed = false;
        blastDueMs = -1;
    }

    @Override
    public void onTick() {
        MinecraftClient mc = MinecraftClient.getInstance();
        ClientPlayerEntity player = mc.player;
        if (player == null || mc.world == null || mc.interactionManager == null || mc.currentScreen != null) {
            armed = false;
            blastDueMs = -1;
            return;
        }
        if (selfDamageCheck.get() && busy(player)) {
            blastDueMs = -1;
            return;
        }

        Entity aimed = PlayerActions.targetedEntity(mc);
        EndCrystalEntity crystal = aimed instanceof EndCrystalEntity c && c.isAlive() ? c : null;
        BlockHitResult block = PlayerActions.targetedBlock(mc);

        boolean active;
        if ("Hold".equals(mode.get())) {
            active = mc.options.useKey.isPressed();
        } else {
            if (crystal != null && isBase(mc, crystal.getBlockPos().down())) armed = true;
            else if (!(armed && block != null && isBase(mc, block.getBlockPos()))) armed = false;
            active = armed;
        }
        if (!active) {
            blastDueMs = -1;
            return;
        }

        long now = Timing.nowMs();
        if (crystal != null) {
            if (blastDueMs < 0) blastDueMs = now + Math.round(Timing.jitter(blastDelay.get(), blastRandom.get()));
            if (now >= blastDueMs && now >= nextAttackMs) {
                PlayerActions.attack(mc, crystal);
                nextAttackMs = now + cpsInterval();
                blastDueMs = -1;
                jitter(player);
            }
            return;
        }
        blastDueMs = -1;

        if (block == null || now < nextPlaceMs || !isBase(mc, block.getBlockPos())) return;
        Hand hand = player.getMainHandStack().isOf(Items.END_CRYSTAL) ? Hand.MAIN_HAND
                : player.getOffHandStack().isOf(Items.END_CRYSTAL) ? Hand.OFF_HAND : null;
        if (hand == null) return;
        BlockPos above = block.getBlockPos().up();
        if (!mc.world.getBlockState(above).isAir()) return;
        if (!mc.world.getEntitiesByClass(EndCrystalEntity.class, new Box(above), e -> true).isEmpty()) return;
        if (PlayerActions.useOnBlock(mc, hand, block)) {
            nextPlaceMs = now + Math.max(cpsInterval(), Math.round(Timing.jitter(delay.get(), randomDelay.get())));
            jitter(player);
        }
    }

    private static boolean isBase(MinecraftClient mc, BlockPos pos) {
        return mc.world.getBlockState(pos).isOf(Blocks.OBSIDIAN) || mc.world.getBlockState(pos).isOf(Blocks.BEDROCK);
    }

    /** Eating, drinking, drawing a bow / trident ... (holding up a shield does not pause). */
    private static boolean busy(ClientPlayerEntity player) {
        return player.isUsingItem() && !player.getActiveItem().isOf(Items.SHIELD);
    }

    private long cpsInterval() {
        double lo = Math.min(minCps.get(), maxCps.get());
        double hi = Math.max(minCps.get(), maxCps.get());
        double cps = hi > lo ? ThreadLocalRandom.current().nextDouble(lo, hi) : lo;
        return Math.round(1000.0 / Math.max(cps, 1.0));
    }

    private void jitter(ClientPlayerEntity player) {
        double j = screenJitter.get();
        if (j <= 0.0) return;
        ThreadLocalRandom rng = ThreadLocalRandom.current();
        player.setYaw((float) (player.getYaw() + rng.nextDouble(-j, j)));
        player.setPitch((float) Math.max(-90.0, Math.min(90.0, player.getPitch() + rng.nextDouble(-j, j))));
    }
}
