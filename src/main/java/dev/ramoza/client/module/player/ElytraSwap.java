package dev.ramoza.client.module.player;

import dev.ramoza.client.module.Category;
import dev.ramoza.client.module.MacroModule;
import dev.ramoza.client.setting.ModeSetting;
import dev.ramoza.client.util.PlayerActions;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.screen.slot.SlotActionType;

/**
 * Swaps chestplate and elytra without opening the inventory, using three clicks on the player screen handler
 * (pick up the item, place it on armor slot 6, put the old chest piece where the new one was), each click paced
 * by Delay +/- Random Delay.
 * <p>
 * Mode Key: Activate Key swaps (chestplate -> elytra, elytra -> best chestplate).
 * Mode Auto: elytra when you are falling for a while, best chestplate again once you land.
 */
public final class ElytraSwap extends MacroModule {
    private static final int CHEST_SCREEN_SLOT = 6;
    private static final double AUTO_FALL_BLOCKS = 3.0;

    private final ModeSetting mode = add(new ModeSetting("Mode",
            "Key: swap on Activate Key. Auto: elytra while falling, chestplate on landing", "Key", "Key", "Auto"));

    private final Runnable[] sequence = {this::pickUp, this::placeOnChest, this::putBack};
    private int itemSlot; // player screen handler slot of the item being equipped

    public ElytraSwap() {
        super("Elytra Swap", "Swaps chestplate and elytra without opening the inventory", Category.PLAYER);
    }

    @Override
    protected void tick(MinecraftClient mc) {
        if (runner.running()) return;
        ClientPlayerEntity player = mc.player;
        boolean wearingElytra = player.getEquippedStack(EquipmentSlot.CHEST).isOf(Items.ELYTRA);

        if (mode.get().equals("Auto")) {
            boolean falling = !player.isOnGround() && player.getVelocity().y < 0
                    && PlayerActions.fallDistance(player) > AUTO_FALL_BLOCKS;
            if (falling && !wearingElytra) begin(player, false);
            else if (player.isOnGround() && wearingElytra) begin(player, true);
        } else if (pressed()) {
            begin(player, wearingElytra);
        }
    }

    /** Picks the replacement (elytra, or best chestplate when {@code toChestplate}) and starts the three clicks. */
    private void begin(ClientPlayerEntity player, boolean toChestplate) {
        int inventoryIndex = toChestplate ? bestChestplate(player) : PlayerActions.findAnywhere(player, Items.ELYTRA);
        if (inventoryIndex < 0) return;
        itemSlot = PlayerActions.screenSlot(inventoryIndex);
        runner.start(sequence);
    }

    private void pickUp() { click(itemSlot); }

    private void placeOnChest() { click(CHEST_SCREEN_SLOT); }

    private void putBack() { click(itemSlot); }

    private void click(int slot) {
        MinecraftClient mc = MinecraftClient.getInstance();
        ClientPlayerEntity player = mc.player;
        if (player == null || mc.interactionManager == null) return;
        mc.interactionManager.clickSlot(player.playerScreenHandler.syncId, slot, 0, SlotActionType.PICKUP, player);
    }

    /** Inventory index (0-35) of the best chestplate, or -1. Ranked by material, found by item id suffix. */
    private static int bestChestplate(ClientPlayerEntity player) {
        int best = -1;
        int bestRank = 0;
        for (int i = 0; i < 36; i++) {
            ItemStack stack = player.getInventory().getStack(i);
            if (stack.isEmpty()) continue;
            String path = Registries.ITEM.getId(stack.getItem()).getPath();
            if (!path.endsWith("_chestplate")) continue;
            int rank = rank(path);
            if (rank > bestRank) {
                bestRank = rank;
                best = i;
            }
        }
        return best;
    }

    private static int rank(String path) {
        if (path.startsWith("netherite")) return 7;
        if (path.startsWith("diamond")) return 6;
        if (path.startsWith("iron")) return 5;
        if (path.startsWith("chainmail")) return 4;
        if (path.startsWith("golden")) return 3;
        if (path.startsWith("copper")) return 2;
        return 1; // leather and anything else
    }
}
