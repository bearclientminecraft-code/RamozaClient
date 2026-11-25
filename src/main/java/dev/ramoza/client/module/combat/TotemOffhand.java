package dev.ramoza.client.module.combat;

import dev.ramoza.client.module.Category;
import dev.ramoza.client.module.ClientModule;
import dev.ramoza.client.module.DelayTimer;
import dev.ramoza.client.setting.BoolSetting;
import dev.ramoza.client.setting.NumberSetting;
import dev.ramoza.client.util.PlayerActions;
import dev.ramoza.client.util.TotemUtil;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.network.ClientPlayerEntity;

import java.util.ArrayList;
import java.util.List;

/**
 * No totem in the offhand: a totem is put on the Totem Slot (if it is not there yet), that slot is selected,
 * "swap hands" (F) moves it to the offhand, and your previous slot is selected again.
 * Open Inventory on: the move to the Totem Slot is done with the inventory screen opened and closed again.
 * <p>
 * Steps run one per tick with Delay +/- Random Delay between them. The slot selection and the F swap are always
 * in different ticks, because the slot packet is only sent by the interaction manager on the next tick.
 */
public final class TotemOffhand extends ClientModule {
    private static final int RETRY_COOLDOWN_TICKS = 10;

    private final NumberSetting totemSlot = add(new NumberSetting("Totem Slot",
            "Hotbar slot (1-9) used as the stop on the way to the offhand", 9, 1, 9, 1));
    private final BoolSetting openInventory = add(new BoolSetting("Open Inventory",
            "Open the inventory screen while moving the totem to the Totem Slot", false));
    private final DelayTimer delay = DelayTimer.attach(this, 0);

    private final List<Runnable> steps = new ArrayList<>();
    private int index;
    private long tickCounter;
    private long lastStepTick;
    private long nextStartTick;
    private boolean openedByUs;
    private boolean selectedByUs;
    private int previousSlot;

    public TotemOffhand() {
        super("Totem Offhand", "Offhand without a totem: moves one via the Totem Slot and takes it with F", Category.COMBAT);
    }

    @Override
    protected void onEnable() { cancel(); }

    @Override
    protected void onDisable() { cancel(); }

    @Override
    public void onTick() {
        MinecraftClient mc = MinecraftClient.getInstance();
        ClientPlayerEntity player = mc.player;
        tickCounter++;
        if (player == null || mc.interactionManager == null) {
            steps.clear();
            return;
        }
        if (!steps.isEmpty()) {
            if (tickCounter > lastStepTick && delay.elapsed()) runNextStep(mc);
            return;
        }
        if (tickCounter < nextStartTick) return;
        if (mc.currentScreen != null && !(mc.currentScreen instanceof InventoryScreen)) return;
        if (TotemUtil.isTotem(player.getOffHandStack())) return;

        int target = TotemUtil.hotbarIndex(totemSlot);
        boolean onTarget = TotemUtil.isTotem(player.getInventory().getStack(target));
        if (!onTarget && TotemUtil.findTotem(player, true, target) < 0) return; // no totem anywhere

        steps.clear();
        index = 0;
        if (!onTarget) {
            if (openInventory.get()) steps.add(this::open);
            steps.add(this::moveToTarget);
            if (openInventory.get()) steps.add(this::close);
        }
        steps.add(this::select);
        steps.add(this::swapHands);
        steps.add(this::restore);
        delay.restart();
        lastStepTick = tickCounter - 1;
    }

    private void runNextStep(MinecraftClient mc) {
        Runnable step = steps.get(index++);
        if (index >= steps.size()) { steps.clear(); nextStartTick = tickCounter + RETRY_COOLDOWN_TICKS; }
        step.run();
        lastStepTick = tickCounter;
        delay.restart();
    }

    private void open() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.currentScreen == null) {
            mc.setScreen(new InventoryScreen(mc.player));
            openedByUs = true;
        }
    }

    private void moveToTarget() {
        MinecraftClient mc = MinecraftClient.getInstance();
        int target = TotemUtil.hotbarIndex(totemSlot);
        if (TotemUtil.isTotem(mc.player.getInventory().getStack(target))) return;
        int source = TotemUtil.findTotem(mc.player, true, target);
        if (source < 0) { cancel(); return; }
        TotemUtil.swapToHotbar(mc, source, target);
    }

    private void close() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (openedByUs && mc.currentScreen instanceof InventoryScreen) mc.setScreen(null);
        openedByUs = false;
    }

    private void select() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.currentScreen != null && !openedByUs && !(mc.currentScreen instanceof InventoryScreen)) { cancel(); return; }
        int target = TotemUtil.hotbarIndex(totemSlot);
        if (!TotemUtil.isTotem(mc.player.getInventory().getStack(target))) { cancel(); return; }
        previousSlot = PlayerActions.selectedSlot(mc.player);
        selectedByUs = true;
        PlayerActions.selectSlot(mc.player, target);
    }

    private void swapHands() {
        MinecraftClient mc = MinecraftClient.getInstance();
        ClientPlayerEntity player = mc.player;
        // someone else already filled the offhand, or the slot no longer holds the totem: leave everything alone
        if (TotemUtil.isTotem(player.getOffHandStack())
                || !TotemUtil.isTotem(player.getMainHandStack())) {
            return;
        }
        TotemUtil.swapHands(mc);
    }

    private void restore() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (selectedByUs && mc.player != null) PlayerActions.selectSlot(mc.player, previousSlot);
        selectedByUs = false;
    }

    /** Drops the running sequence and undoes what it changed on the client side (screen, selected slot). */
    private void cancel() {
        steps.clear();
        index = 0;
        nextStartTick = tickCounter + RETRY_COOLDOWN_TICKS;
        MinecraftClient mc = MinecraftClient.getInstance();
        if (openedByUs && mc.currentScreen instanceof InventoryScreen) mc.setScreen(null);
        openedByUs = false;
        if (selectedByUs && mc.player != null) PlayerActions.selectSlot(mc.player, previousSlot);
        selectedByUs = false;
    }
}
