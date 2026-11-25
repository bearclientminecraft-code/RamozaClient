package dev.ramoza.client.util;

import dev.ramoza.client.setting.NumberSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

/** Shared helpers of the four totem modules. Slot numbers here are player screen handler slots unless stated. */
public final class TotemUtil {
    /** Button value of a SWAP click that targets the offhand. */
    private static final int OFFHAND_BUTTON = 40;

    private TotemUtil() {}

    public static boolean isTotem(ItemStack stack) {
        return stack.isOf(Items.TOTEM_OF_UNDYING);
    }

    /** "Totem Slot" setting (1-9) -> hotbar index 0-8. */
    public static int hotbarIndex(NumberSetting setting) {
        return setting.get().intValue() - 1;
    }

    /**
     * Screen slot of a totem to take: main inventory first (9-35), then, if {@code includeHotbar}, hotbar slots
     * other than {@code exceptHotbar}. Returns -1 when there is none.
     */
    public static int findTotem(ClientPlayerEntity player, boolean includeHotbar, int exceptHotbar) {
        for (int i = 9; i < 36; i++) if (isTotem(player.getInventory().getStack(i))) return i;
        if (includeHotbar) {
            for (int i = 0; i < 9; i++) {
                if (i != exceptHotbar && isTotem(player.getInventory().getStack(i))) return PlayerActions.screenSlot(i);
            }
        }
        return -1;
    }

    /** Swaps the stack in {@code screenSlot} with hotbar slot {@code hotbarIndex} (like pressing 1-9 over a slot). */
    public static void swapToHotbar(MinecraftClient mc, int screenSlot, int hotbarIndex) {
        mc.interactionManager.clickSlot(mc.player.playerScreenHandler.syncId, screenSlot, hotbarIndex,
                SlotActionType.SWAP, mc.player);
    }

    /** Swaps the stack in {@code screenSlot} with the offhand (like pressing F over a slot). */
    public static void swapToOffhand(MinecraftClient mc, int screenSlot) {
        mc.interactionManager.clickSlot(mc.player.playerScreenHandler.syncId, screenSlot, OFFHAND_BUTTON,
                SlotActionType.SWAP, mc.player);
    }

    /** The "swap hands" (F) action: main hand item <-> offhand item, same packet vanilla sends. */
    public static void swapHands(MinecraftClient mc) {
        if (mc.getNetworkHandler() == null) return;
        mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(
                PlayerActionC2SPacket.Action.SWAP_ITEM_WITH_OFFHAND, BlockPos.ORIGIN, Direction.DOWN));
    }
}
