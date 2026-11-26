package dev.ramoza.client.module.combat;

import dev.ramoza.client.RamozaClientMod;
import dev.ramoza.client.core.RamozaCore;
import dev.ramoza.client.hud.HudStats;
import dev.ramoza.client.module.Category;
import dev.ramoza.client.module.ClientModule;
import dev.ramoza.client.setting.BoolSetting;
import dev.ramoza.client.setting.KeybindSetting;
import dev.ramoza.client.setting.ModeSetting;
import dev.ramoza.client.setting.NumberSetting;
import dev.ramoza.client.util.HumanTiming;
import dev.ramoza.client.util.KeyInput;
import dev.ramoza.client.util.Keys;
import dev.ramoza.client.util.PlayerActions;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Trigger Bot: attacks the living entity under the crosshair once a (randomised) delay has passed.
 * <p>
 * Decisions are made once per rendered FRAME (see {@code MinecraftClientMixin}); all timing is
 * delta-time based ({@link System#nanoTime()}), so behaviour does not depend on the frame rate.
 * <p>
 * <b>Humanize ON (default)</b> changes how the decision is shaped and when the click leaves the client:
 * <ul>
 *   <li>Reaction to a new target is log-normal (right-skewed, correlated, rare slow outliers), not uniform.
 *       Switching directly from one entity to another counts as a new target and waits again.</li>
 *   <li>After the weapon cooldown is full there is a short, varying "click lag" before the hit, so the
 *       interval between hits is no longer cooldown-exact. Weapon Min/Max add a right-skewed extra.</li>
 *   <li>Hit Chance is rolled with streaks (focus state), not as independent coin flips.</li>
 *   <li>A minimum gap between clicks (about 10 CPS ceiling) applies even with Cooldown Check off.</li>
 *   <li>The attack is not sent from the render hook. It is queued and sent at the start of the next client
 *       tick, which is where vanilla processes a real left click, so attack packets are tick-aligned.
 *       It is re-validated first: if the crosshair left the target or it left range, the hit is dropped.</li>
 * </ul>
 * <b>Humanize OFF</b> keeps the original behaviour: uniform delays, independent rolls, immediate attack.
 * <p>
 * <p>
 * <b>Mode</b>: Classic is everything above. Crystal Timing hits PLAYERS under the crosshair on a fixed rhythm
 * (Timing (ms) +/- Timing Jitter) that ignores the weapon cooldown, Humanize, Hit Chance and reaction time, so
 * crystals can be placed in step with the hits. Range, Weapon Check, Hold Mode and the tick-aligned send still apply.
 * <p>
 * No per-frame allocations: every value lives in a field.
 */
public final class Trigger extends ClientModule {
    /** A single frame never advances the timer by more than this (lag spike / alt-tab protection), ms. */
    private static final double MAX_FRAME_MS = 100.0;
    private static final double RANDOM_CHANCE_MIN = 50.0;
    private static final double RANDOM_CHANCE_MAX = 70.0;

    // Humanize tuning (log-space sigma, drift weight, chance of a slow outlier)
    private static final double REACTION_SIGMA = 0.35;
    private static final double REACTION_DRIFT = 0.25;
    private static final double REACTION_HESITATION = 0.04;
    private static final double CLICK_LAG_SIGMA = 0.55;
    private static final double CLICK_LAG_DRIFT = 0.30;
    private static final double CLICK_LAG_HESITATION = 0.03;
    private static final double MIN_GAP_MEDIAN_MS = 95.0;
    private static final double MIN_GAP_SIGMA = 0.20;

    private static final int WEAPON_NONE = 0;
    private static final int WEAPON_SWORD = 1;
    private static final int WEAPON_AXE = 2;
    private static final int WEAPON_MACE = 3;

    /** Mace smash needs more than this many blocks of fall (vanilla MaceItem rule). */
    private static final double MACE_MIN_FALL = 1.5;
    /** TPS sync is skipped at or above this value (server is on time) and never assumes less than TPS_FLOOR. */
    private static final double TPS_OK = 19.5;
    private static final double TPS_FLOOR = 5.0;

    /** Enabled instance, cached so the per-frame hook needs no module lookup. Render thread only. */
    private static Trigger active;

    // ---- Mode
    private final ModeSetting mode = add(new ModeSetting("Mode",
            "Classic: normal trigger. Crystal Timing: hits players on a fixed rhythm (Timing) so you can place crystals in step",
            "Classic", "Classic", "Crystal Timing"));
    private final NumberSetting timingMs = add(new NumberSetting("Timing (ms)",
            "Crystal Timing: time between two hits on a player. Ignores the weapon cooldown", 500, 50, 2000, 5));
    private final NumberSetting timingJitter = add(new NumberSetting("Timing Jitter (ms)",
            "Crystal Timing: every interval is shifted by a random value in +/- this many ms (0 = exact rhythm)", 0, 0, 100, 1));

    // ---- Weapon Settings
    private final BoolSetting weaponCheck = add(new BoolSetting("Weapon Check",
            "Only trigger while a sword, an axe or a mace is in your main hand", true));
    private final NumberSetting swordMin = add(new NumberSetting("Sword Delay Min",
            "Shortest extra wait (ms) before a sword hit. If Min is above Max they are swapped", 0, 0, 1000, 5));
    private final NumberSetting swordMax = add(new NumberSetting("Sword Delay Max",
            "Longest extra wait (ms) before a sword hit. Each hit rolls a value between Min and Max", 60, 0, 1000, 5));
    private final NumberSetting axeMin = add(new NumberSetting("Axe Delay Min",
            "Shortest extra wait (ms) before an axe hit. If Min is above Max they are swapped", 0, 0, 1000, 5));
    private final NumberSetting axeMax = add(new NumberSetting("Axe Delay Max",
            "Longest extra wait (ms) before an axe hit. Each hit rolls a value between Min and Max", 80, 0, 1000, 5));

    private final NumberSetting maceMin = add(new NumberSetting("Mace Delay Min",
            "Shortest extra wait (ms) before a mace hit. If Min is above Max they are swapped", 0, 0, 1000, 5));
    private final NumberSetting maceMax = add(new NumberSetting("Mace Delay Max",
            "Longest extra wait (ms) before a mace hit. Each hit rolls a value between Min and Max", 60, 0, 1000, 5));

    // ---- Criticals / server sync
    private final BoolSetting onlyCriticals = add(new BoolSetting("Only Criticals",
            "Hit only while the hit would be a critical: in the air, falling, not sprinting, not climbing or swimming, "
            + "not blind, not riding. Mace: needs a smash fall (more than 1.5 blocks), not gliding. Waits instead of hitting on the ground", false));
    private final BoolSetting tpsSync = add(new BoolSetting("TPS Sync",
            "Cooldown Check follows the SERVER tick rate: when TPS drops below 20 the server recharges the weapon slower than "
            + "your client shows, so the hit waits for that extra real time. Needs Cooldown Check, uses the watermark TPS estimate", true));

    // ---- Timing
    private final BoolSetting humanize = add(new BoolSetting("Humanize",
            "Human-like timing: skewed reaction, click lag after the cooldown, streaky hit chance, tick-aligned clicks", true));
    private final NumberSetting reactionMs = add(new NumberSetting("Reaction (ms)",
            "Humanize: typical wait before the first hit after a target enters the crosshair. Most reactions land near it, a few are much slower",
            170, 60, 400, 5));
    private final NumberSetting delay = add(new NumberSetting("Delay (ms)",
            "Base wait (ms) added to the weapon delay. Humanize ON: typical click lag after the cooldown is full. OFF: also the reaction time",
            20, 0, 500, 1));
    private final NumberSetting randomDelay = add(new NumberSetting("Random Delay",
            "Humanize OFF only: every wait is shifted by a random value in +/- this many milliseconds (0 = off)", 10, 0, 200, 1));
    private final BoolSetting holdMode = add(new BoolSetting("Hold Mode",
            "ON: only active while the Activate Key is held. OFF: always active while the module is on", false));
    private final KeybindSetting activateKey = add(new KeybindSetting("Activate Key",
            "Key or mouse button used by Hold Mode. Unbound = Hold Mode is always armed", Keys.NONE));
    private final BoolSetting cooldownCheck = add(new BoolSetting("Cooldown Check",
            "Wait until the weapon's attack cooldown has recharged, so no hit is wasted on a weak swing", true));

    // ---- Target Settings
    private final NumberSetting rangeMin = add(new NumberSetting("Range Min",
            "Closest distance (blocks, eyes to hit point) at which the trigger may fire", 0, 0, 6, 0.1));
    private final NumberSetting rangeMax = add(new NumberSetting("Range Max",
            "Farthest distance (blocks) at which the trigger may fire. Vanilla reach is about 3", 3, 0, 6, 0.1));
    private final NumberSetting edgeMargin = add(new NumberSetting("Edge Margin",
            "The crosshair must be this many blocks inside the target's hitbox. Other players are drawn slightly behind "
            + "their real server position, so hits on the very edge swing at nothing. 0 = off", 0.1, 0, 0.25, 0.01));
    private final NumberSetting hitChance = add(new NumberSetting("Hit Chance",
            "Chance (%) that an attack window ends in a hit. 100 = every window, 0 = never. Humanize ON: misses come in streaks", 100, 0, 100, 1));

    // ---- Random Hit Chance
    private final BoolSetting randomHit = add(new BoolSetting("Random Hit Chance",
            "While Random Hit Key is held, the hit chance is re-randomised between 50% and 70% on every roll",
            false));
    private final KeybindSetting randomHitKey = add(new KeybindSetting("Random Hit Key",
            "Hold this key or mouse button to use the random 50-70% hit chance. Release restores Hit Chance", Keys.NONE));

    // ---- runtime state (no allocation after construction)
    private final HumanTiming human = new HumanTiming();
    private long lastNanos;
    private double waitMs;
    private boolean hadTarget;
    private boolean armed;
    private Entity lastTarget;
    /** Humanize: hit decided on a frame, sent at the start of the next client tick. */
    private Entity pending;
    private ClientPlayerEntity lastPlayer;
    private boolean lastCrystal;
    /** TPS sync: when our last attack left, and how long the client then needed to show a full cooldown. */
    private long lastAttackNanos;
    private double measuredCooldownMs;
    private boolean measuring;

    public Trigger() {
        super("Trigger Bot", "Automatically hits the entity under your crosshair (sword, axe, mace) with randomised timing", Category.COMBAT);
    }

    /** Called once per rendered frame by the client mixin. */
    public static void frame(MinecraftClient mc) {
        Trigger t = active;
        if (t != null) t.update(mc);
    }

    @Override
    protected void onEnable() {
        reset();
        human.reset();
        active = this;
    }

    @Override
    protected void onDisable() {
        if (active == this) active = null;
        reset();
    }

    private boolean crystalMode() { return "Crystal Timing".equals(mode.get()); }

    /** Core instance lookup kept for GUI / other modules; not used on the frame path. */
    public static Trigger instance() {
        RamozaCore core = RamozaClientMod.core();
        return core == null ? null : core.modules.get(Trigger.class);
    }

    private void reset() {
        lastNanos = 0L;
        waitMs = 0.0;
        drop();
        armed = false;
        lastPlayer = null;
        lastAttackNanos = 0L;
        measuredCooldownMs = 0.0;
        measuring = false;
    }

    /** Forget the current target and any queued hit. */
    private void drop() {
        hadTarget = false;
        lastTarget = null;
        pending = null;
    }

    /**
     * Start of a client tick (START_CLIENT_TICK, before vanilla input handling). Sends the hit that a frame
     * queued in Humanize mode, after checking that it is still valid.
     */
    @Override
    public void onTick() {
        Entity target = pending;
        if (target == null) return;
        pending = null;

        MinecraftClient mc = MinecraftClient.getInstance();
        ClientPlayerEntity player = mc.player;
        if (player == null || mc.world == null || mc.interactionManager == null || mc.currentScreen != null) return;
        if (player.isUsingItem()) return;
        if (!crystalMode() && !cooldownReady(player, System.nanoTime())) return;
        // the player may have landed / started sprinting since the frame that queued this hit
        if (onlyCriticals.get() && !critReady(player, weaponOf(player))) return;

        EntityHitResult hit = validTarget(mc, player);
        if (hit == null || hit.getEntity() != target) return;

        strike(mc, target);
    }

    /** Sends the attack and starts the TPS-sync measurement (attack -> client shows a full cooldown). */
    private void strike(MinecraftClient mc, Entity target) {
        PlayerActions.attack(mc, target);
        lastAttackNanos = System.nanoTime();
        measuredCooldownMs = 0.0;
        measuring = true;
    }

    /**
     * Weapon cooldown is full. With TPS Sync the hit also waits until the SERVER has had enough ticks: the client
     * needed {@code measuredCooldownMs} real time to recharge, a server running at {@code tps} needs that times 20/tps.
     */
    private boolean cooldownReady(ClientPlayerEntity player, long nowNanos) {
        if (!cooldownCheck.get()) return true;
        if (player.getAttackCooldownProgress(0.0f) < 1.0f) return false;
        if (!tpsSync.get() || measuring || measuredCooldownMs <= 0.0 || lastAttackNanos == 0L) return true;
        double tps = HudStats.tps();
        if (Double.isNaN(tps) || tps >= TPS_OK) return true;
        double neededMs = measuredCooldownMs * (20.0 / Math.max(tps, TPS_FLOOR));
        return (nowNanos - lastAttackNanos) / 1_000_000.0 >= neededMs;
    }

    /** True if a hit right now would be a critical (mace: a smash). Mirrors the vanilla conditions of PlayerEntity#attack. */
    private boolean critReady(ClientPlayerEntity p, int weapon) {
        if (p.isOnGround() || p.isClimbing() || p.isTouchingWater() || p.hasVehicle()) return false;
        if (p.hasStatusEffect(StatusEffects.BLINDNESS) || p.isGliding()) return false;
        double fall = PlayerActions.fallDistance(p);
        if (weapon == WEAPON_MACE) return fall > MACE_MIN_FALL && !p.isSprinting();
        return fall > 0.0 && !p.isSprinting();
    }

    private void update(MinecraftClient mc) {
        ClientPlayerEntity player = mc.player;

        // world / player change, screen open, disconnected: drop all state, nothing is carried over
        if (player == null || mc.world == null || mc.interactionManager == null || mc.currentScreen != null) {
            reset();
            return;
        }
        if (player != lastPlayer) {
            reset();
            lastPlayer = player;
        }

        long now = System.nanoTime();
        double dt = lastNanos == 0L ? 0.0 : Math.min((now - lastNanos) / 1_000_000.0, MAX_FRAME_MS);
        lastNanos = now;
        if (measuring && player.getAttackCooldownProgress(0.0f) >= 1.0f) {
            measuredCooldownMs = (now - lastAttackNanos) / 1_000_000.0;
            measuring = false;
        }
        boolean crystal = crystalMode();
        if (crystal != lastCrystal) { // mode changed: start clean
            lastCrystal = crystal;
            waitMs = 0.0;
            armed = false;
            drop();
        }
        if (waitMs > 0.0) waitMs -= dt;

        if (holdMode.get() && activateKey.isBound() && !KeyInput.isDown(activateKey.get())) {
            drop();
            return;
        }

        int weapon = weaponOf(player);
        if (weaponCheck.get() && weapon == WEAPON_NONE) {
            drop();
            return;
        }

        EntityHitResult hit = validTarget(mc, player);
        if (hit == null) {
            drop();
            return;
        }

        if (crystal) {
            // fixed rhythm: the timer runs continuously, a hit is queued whenever it is due and a player is aimed at
            if (pending != null || waitMs > 0.0 || player.isUsingItem()) return;
            if (onlyCriticals.get() && !critReady(player, weapon)) return;
            pending = hit.getEntity(); // sent at the start of the next tick, see onTick()
            double jitter = timingJitter.get();
            double next = timingMs.get() + (jitter > 0.0 ? ThreadLocalRandom.current().nextDouble(-jitter, jitter) : 0.0);
            waitMs = Math.max(1.0, next);
            return;
        }

        boolean humanized = humanize.get();
        Entity target = hit.getEntity();

        // reaction time: a target that just entered the crosshair (or replaced the previous one)
        // always waits at least one fresh delay
        if (!hadTarget || target != lastTarget) {
            hadTarget = true;
            lastTarget = target;
            pending = null;
            double reaction = humanized ? humanReaction(weapon) : rollDelay(weapon);
            if (reaction > waitMs) waitMs = reaction;
        }

        // a hit is already queued for the next tick: nothing more to decide this frame
        if (pending != null) return;

        if (player.isUsingItem()) return;
        if (!cooldownReady(player, now)) {
            armed = false;
            return;
        }
        // Only Criticals: no attack window opens until the hit would be a crit (the timers keep running meanwhile)
        if (onlyCriticals.get() && !critReady(player, weapon)) {
            armed = false;
            return;
        }

        // Humanize: the moment the cooldown is full a short, varying click lag starts
        if (humanized && !armed) {
            armed = true;
            double lag = human.delay(Math.max(delay.get(), 1.0), CLICK_LAG_SIGMA, CLICK_LAG_DRIFT, CLICK_LAG_HESITATION)
                    + human.skewedBetween(weaponMin(weapon), weaponMax(weapon));
            if (lag > waitMs) waitMs = lag;
        }
        if (waitMs > 0.0) return;

        // one attack window: roll the chance once, then start the next wait either way
        boolean doHit = rollHit(humanized);
        armed = false;
        if (humanized) {
            waitMs = human.delay(MIN_GAP_MEDIAN_MS, MIN_GAP_SIGMA, CLICK_LAG_DRIFT, CLICK_LAG_HESITATION);
            if (doHit) pending = target; // sent at the start of the next tick, see onTick()
        } else {
            if (doHit) strike(mc, target);
            waitMs = rollDelay(weapon);
        }
    }

    /** The crosshair entity if it is a valid, in-range target, else null. No allocation. */
    private EntityHitResult validTarget(MinecraftClient mc, ClientPlayerEntity player) {
        if (!(mc.crosshairTarget instanceof EntityHitResult hit)) return null;
        Entity e = hit.getEntity();
        if (e == player || !(e instanceof LivingEntity living)) return null;
        if (crystalMode() && !(e instanceof PlayerEntity)) return null;
        if (!living.isAlive() || living.isSpectator() || !living.isAttackable()) return null;

        Vec3d p = hit.getPos();
        double dx = p.x - player.getX();
        double dy = p.y - player.getEyeY();
        double dz = p.z - player.getZ();
        double d2 = dx * dx + dy * dy + dz * dz;

        double a = rangeMin.get();
        double b = rangeMax.get();
        double lo = Math.min(a, b);
        double hi = Math.max(a, b);
        if (d2 < lo * lo || d2 > hi * hi) return null;
        return aimedInside(player, e) ? hit : null;
    }

    /** True if the look line passes at least Edge Margin blocks inside the target's hitbox (current tick position). */
    private boolean aimedInside(ClientPlayerEntity player, Entity e) {
        double m = edgeMargin.get();
        if (m <= 0.0) return true;
        Box box = e.getBoundingBox();
        // never shrink more than 40% of a side, so small entities keep a usable box
        Box inner = box.expand(-Math.min(m, box.getLengthX() * 0.4), -Math.min(m, box.getLengthY() * 0.4),
                -Math.min(m, box.getLengthZ() * 0.4));
        Vec3d eye = player.getEyePos();
        Vec3d end = eye.add(player.getRotationVec(1.0f).multiply(Math.max(rangeMax.get(), 0.0) + 1.0));
        return inner.contains(eye) || inner.raycast(eye, end).isPresent();
    }

    private int weaponOf(ClientPlayerEntity player) {
        ItemStack stack = player.getMainHandStack();
        if (stack.isIn(ItemTags.SWORDS)) return WEAPON_SWORD;
        if (stack.isIn(ItemTags.AXES)) return WEAPON_AXE;
        if (stack.isOf(Items.MACE)) return WEAPON_MACE;
        return WEAPON_NONE;
    }

    private double weaponMin(int weapon) {
        return weapon == WEAPON_SWORD ? swordMin.get() : weapon == WEAPON_AXE ? axeMin.get()
                : weapon == WEAPON_MACE ? maceMin.get() : 0.0;
    }

    private double weaponMax(int weapon) {
        return weapon == WEAPON_SWORD ? swordMax.get() : weapon == WEAPON_AXE ? axeMax.get()
                : weapon == WEAPON_MACE ? maceMax.get() : 0.0;
    }

    /** Humanize: skewed reaction to a new target plus a skewed weapon extra. */
    private double humanReaction(int weapon) {
        return human.delay(reactionMs.get(), REACTION_SIGMA, REACTION_DRIFT, REACTION_HESITATION)
                + human.skewedBetween(weaponMin(weapon), weaponMax(weapon));
    }

    /** Original timing (Humanize OFF): Delay +/- Random Delay plus a uniform weapon delay. */
    private double rollDelay(int weapon) {
        ThreadLocalRandom rng = ThreadLocalRandom.current();
        double t = delay.get();
        double jitter = randomDelay.get();
        if (jitter > 0.0) t += rng.nextDouble(-jitter, jitter);
        if (weapon == WEAPON_SWORD) t += between(swordMin.get(), swordMax.get(), rng);
        else if (weapon == WEAPON_AXE) t += between(axeMin.get(), axeMax.get(), rng);
        else if (weapon == WEAPON_MACE) t += between(maceMin.get(), maceMax.get(), rng);
        return Math.max(0.0, t);
    }

    private static double between(double a, double b, ThreadLocalRandom rng) {
        double lo = Math.min(a, b);
        double hi = Math.max(a, b);
        return hi > lo ? rng.nextDouble(lo, hi) : lo;
    }

    private boolean rollHit(boolean humanized) {
        ThreadLocalRandom rng = ThreadLocalRandom.current();
        double chance = hitChance.get();
        if (randomHit.get() && randomHitKey.isBound() && KeyInput.isDown(randomHitKey.get())) {
            chance = RANDOM_CHANCE_MIN + rng.nextDouble() * (RANDOM_CHANCE_MAX - RANDOM_CHANCE_MIN);
        }
        if (humanized) return human.roll(chance);
        return rng.nextDouble(100.0) < chance;
    }
}
