package dev.ramoza.client.util;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.item.Item;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.text.Text;

/** Small helpers for hotbar switching and item use, so modules share one place that touches these APIs. */
public final class PlayerActions {
    private PlayerActions() {}

    public static int selectedSlot(ClientPlayerEntity player) {
        return player.getInventory().getSelectedSlot();
    }

    /** Selects a hotbar slot (0-8). The slot packet is sent by the interaction manager on the next use / tick. */
    public static void selectSlot(ClientPlayerEntity player, int slot) {
        player.getInventory().setSelectedSlot(slot);
    }

    /** Uses the item in {@code hand} like a right click. Returns true if the game accepted it. */
    public static boolean use(MinecraftClient mc, Hand hand) {
        ActionResult result = mc.interactionManager.interactItem(mc.player, hand);
        if (!result.isAccepted()) return false;
        mc.player.swingHand(hand);
        return true;
    }

    /** First hotbar slot (0-8) holding the item, or -1. */
    public static int findInHotbar(ClientPlayerEntity player, Item item) {
        for (int i = 0; i < 9; i++) if (player.getInventory().getStack(i).isOf(item)) return i;
        return -1;
    }

    /** First main-inventory slot (9-35, same index as the player screen handler) holding the item, or -1. */
    public static int findInMain(ClientPlayerEntity player, Item item) {
        for (int i = 9; i < 36; i++) if (player.getInventory().getStack(i).isOf(item)) return i;
        return -1;
    }

    /** Entity under the crosshair (vanilla reach), or null. */
    public static Entity targetedEntity(MinecraftClient mc) {
        return mc.crosshairTarget instanceof EntityHitResult hit ? hit.getEntity() : null;
    }

    /** Attack + arm swing, the same pair vanilla sends for a left click on an entity. */
    public static void attack(MinecraftClient mc, Entity target) {
        mc.interactionManager.attackEntity(mc.player, target);
        mc.player.swingHand(Hand.MAIN_HAND);
    }

    public static double fallDistance(ClientPlayerEntity player) {
        return player.fallDistance;
    }

    /** Hotbar slot (0-8) -> player screen handler slot index (36-44); main inventory 9-35 map to themselves. */
    public static int screenSlot(int inventoryIndex) {
        return inventoryIndex < 9 ? 36 + inventoryIndex : inventoryIndex;
    }

    /** First slot of the whole inventory (hotbar first, then main) holding the item, as inventory index, or -1. */
    public static int findAnywhere(ClientPlayerEntity player, Item item) {
        for (int i = 0; i < 36; i++) if (player.getInventory().getStack(i).isOf(item)) return i;
        return -1;
    }

    /** Right click on a block with the item in {@code hand}, like vanilla. True if the game accepted it. */
    public static boolean useOnBlock(MinecraftClient mc, Hand hand, BlockHitResult hit) {
        ActionResult result = mc.interactionManager.interactBlock(mc.player, hand, hit);
        if (!result.isAccepted()) return false;
        mc.player.swingHand(hand);
        return true;
    }

    /** A click on the top face of a block, at its centre. */
    public static BlockHitResult topOf(BlockPos pos) {
        return new BlockHitResult(Vec3d.ofCenter(pos).add(0.0, 0.5, 0.0), Direction.UP, pos, false);
    }

    /** The block under the crosshair, or null when the crosshair is not on a block. */
    public static BlockHitResult targetedBlock(MinecraftClient mc) {
        return mc.crosshairTarget instanceof BlockHitResult hit && hit.getType() == net.minecraft.util.hit.HitResult.Type.BLOCK
                ? hit : null;
    }

    /** Short message above the hotbar (why a macro did not start). */
    public static void hint(ClientPlayerEntity player, String text) {
        player.sendMessage(Text.literal(text), true);
    }
}
