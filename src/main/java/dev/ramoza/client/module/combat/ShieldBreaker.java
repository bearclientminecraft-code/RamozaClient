package dev.ramoza.client.module.combat;

import dev.ramoza.client.RamozaClientMod;
import dev.ramoza.client.core.RamozaCore;
import dev.ramoza.client.module.Category;
import dev.ramoza.client.module.ClientModule;
import dev.ramoza.client.setting.BoolSetting;
import dev.ramoza.client.setting.ModeSetting;
import dev.ramoza.client.setting.NumberSetting;
import dev.ramoza.client.util.PlayerActions;
import dev.ramoza.client.util.Timing;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.registry.tag.ItemTags;

/**
 * Switches to an axe for a target that blocks with a shield, hits it (an axe disables the shield) and goes back.
 * <ul>
 *   <li><b>On Attack</b>: starts when YOU left-click a blocking target. With Auto Hit the module sends the hit
 *       itself (after Hit Delay); without it your own click goes out with the axe in hand.</li>
 *   <li><b>Auto</b>: starts by itself when the crosshair is on a blocking target. With Auto Hit it also hits.</li>
 *   <li><b>Swap Mode Normal</b>: the hotbar slot really changes. <b>Silent</b>: only the server is told to use the
 *       axe slot, your own hotbar selection does not move; the slot packet back always happens.</li>
 *   <li>Cooldown Check is evaluated before the swap (a swap itself restarts the cooldown).</li>
 *   <li>Pause On Use: nothing happens while you eat, drink, draw a bow or hold up your own shield.</li>
 * </ul>
 */
public final class ShieldBreaker extends ClientModule {
    private static final int IDLE = 0, WAIT_HIT = 1, WAIT_RETURN = 2;
    private static final long LOCK_MS = 300;

    private final ModeSetting mode = add(new ModeSetting("Mode",
            "On Attack: starts when you left-click a blocking target. Auto: starts when you aim at one",
            "On Attack", "On Attack", "Auto"));
    private final ModeSetting swapMode = add(new ModeSetting("Swap Mode",
            "Normal: the hotbar slot changes. Silent: only the server switches, your selection stays", "Normal", "Normal", "Silent"));
    private final BoolSetting autoHit = add(new BoolSetting("Auto Hit",
            "The module sends the axe hit itself. OFF: it only swaps (On Attack: your own click is the hit)", true));
    private final NumberSetting hitDelay = add(new NumberSetting("Hit Delay",
            "Milliseconds between the swap and the hit", 0, 0, 500, 5));
    private final NumberSetting hitRandom = add(new NumberSetting("Hit Random Delay",
            "Hit Delay is shifted by a random value in +/- this many ms", 0, 0, 200, 5));
    private final BoolSetting returnSlot = add(new BoolSetting("Return",
            "Go back to the previous slot afterwards (Silent mode always goes back)", true));
    private final NumberSetting returnDelay = add(new NumberSetting("Return Delay",
            "Milliseconds between the hit and going back", 100, 0, 1000, 5));
    private final NumberSetting returnRandom = add(new NumberSetting("Return Random Delay",
            "Return Delay is shifted by a random value in +/- this many ms", 0, 0, 500, 5));
    private final BoolSetting cooldownCheck = add(new BoolSetting("Cooldown Check",
            "Only swap when your attack cooldown is full", true));
    private final BoolSetting pauseOnUse = add(new BoolSetting("Pause On Use",
            "Do not swap while you eat, drink, draw a bow or hold up your own shield", true));

    private int state = IDLE;
    private Entity target;
    private int previousSlot;
    private boolean swapped;
    private long actAtMs;
    private long lockUntilMs;

    public ShieldBreaker() {
        super("Shield Breaker", "Swaps to an axe and hits a blocking target to break its shield", Category.COMBAT);
    }

    /** Enabled instance or null. */
    public static ShieldBreaker active() {
        RamozaCore core = RamozaClientMod.core();
        if (core == null) return null;
        ShieldBreaker m = core.modules.get(ShieldBreaker.class);
        return m != null && m.isEnabled() ? m : null;
    }

    /** Called by the attack hook. Returns true if the click was taken over (vanilla attack is skipped). */
    public static boolean handleAttack() {
        ShieldBreaker m = active();
        return m != null && "On Attack".equals(m.mode.get()) && m.tryStart(MinecraftClient.getInstance(), true);
    }

    @Override
    protected void onDisable() {
        if (state != IDLE) finish(MinecraftClient.getInstance());
    }

    @Override
    public void onTick() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.world == null || mc.interactionManager == null) {
            state = IDLE;
            swapped = false;
            target = null;
            return;
        }
        long now = Timing.nowMs();
        switch (state) {
            case IDLE -> {
                if ("Auto".equals(mode.get())) tryStart(mc, false);
            }
            case WAIT_HIT -> {
                if (mc.currentScreen != null) finish(mc);
                else if (now >= actAtMs) hit(mc);
            }
            case WAIT_RETURN -> {
                if (now >= actAtMs || mc.currentScreen != null) finish(mc);
            }
            default -> state = IDLE;
        }
    }

    private boolean tryStart(MinecraftClient mc, boolean fromClick) {
        ClientPlayerEntity player = mc.player;
        if (state != IDLE || Timing.nowMs() < lockUntilMs) return false;
        if (player == null || mc.interactionManager == null || mc.currentScreen != null) return false;
        if (pauseOnUse.get() && player.isUsingItem()) return false;

        Entity e = PlayerActions.targetedEntity(mc);
        if (!(e instanceof LivingEntity living) || !living.isAlive() || !isBlocking(living)) return false;

        int axe = findAxe(player);
        if (axe < 0) return false;
        if (cooldownCheck.get() && player.getAttackCooldownProgress(0.0f) < 1.0f) return false;

        int current = PlayerActions.selectedSlot(player);
        swapped = axe != current;
        previousSlot = current;
        target = e;
        if (swapped) swapTo(mc, axe);

        boolean ownHit = autoHit.get();
        long now = Timing.nowMs();
        if (ownHit) {
            state = WAIT_HIT;
            actAtMs = now + Math.round(Timing.jitter(hitDelay.get(), hitRandom.get()));
            if (actAtMs <= now) hit(mc); // no delay: hit in the same tick as the swap
        } else {
            state = WAIT_RETURN;
            actAtMs = now + Math.round(Timing.jitter(returnDelay.get(), returnRandom.get()));
        }
        // our own hit replaces the click; without Auto Hit the click continues with the axe in hand
        return fromClick && ownHit;
    }

    private void hit(MinecraftClient mc) {
        Entity t = target;
        if (t != null && t.isAlive() && PlayerActions.targetedEntity(mc) == t) PlayerActions.attack(mc, t);
        state = WAIT_RETURN;
        actAtMs = Timing.nowMs() + Math.round(Timing.jitter(returnDelay.get(), returnRandom.get()));
        if (actAtMs <= Timing.nowMs()) finish(mc);
    }

    private void finish(MinecraftClient mc) {
        if (swapped && mc.player != null) {
            boolean silent = "Silent".equals(swapMode.get());
            if (silent) sendSlot(mc, previousSlot);
            else if (returnSlot.get()) PlayerActions.selectSlot(mc.player, previousSlot);
        }
        swapped = false;
        target = null;
        state = IDLE;
        lockUntilMs = Timing.nowMs() + LOCK_MS;
    }

    private void swapTo(MinecraftClient mc, int slot) {
        if ("Silent".equals(swapMode.get())) sendSlot(mc, slot);
        else PlayerActions.selectSlot(mc.player, slot);
    }

    private static void sendSlot(MinecraftClient mc, int slot) {
        if (mc.getNetworkHandler() != null) mc.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(slot));
    }

    private static int findAxe(ClientPlayerEntity player) {
        for (int i = 0; i < 9; i++) if (player.getInventory().getStack(i).isIn(ItemTags.AXES)) return i;
        return -1;
    }

    private static boolean isBlocking(LivingEntity e) {
        return e.isBlocking() || (e.isUsingItem() && e.getActiveItem().isOf(Items.SHIELD));
    }
}
