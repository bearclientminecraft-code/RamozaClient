package dev.ramoza.client.module.combat;

import dev.ramoza.client.module.Category;
import dev.ramoza.client.module.MacroModule;
import dev.ramoza.client.setting.BoolSetting;
import dev.ramoza.client.setting.NumberSetting;
import dev.ramoza.client.util.PlayerActions;
import dev.ramoza.client.util.TotemUtil;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;

/**
 * Activate Key (press), aiming at a block: places obsidian on the face you look at, then an end crystal on top of
 * it, then goes back to the slot you were on (unless Return is off). If you look at obsidian / bedrock, only the crystal is placed.
 */
public final class AutoCrystal extends MacroModule {
    private final NumberSetting obsidianSlot = add(new NumberSetting("Obsidian Slot",
            "Hotbar slot (1-9) with obsidian", 1, 1, 9, 1));
    private final NumberSetting crystalSlot = add(new NumberSetting("Crystal Slot",
            "Hotbar slot (1-9) with end crystals", 2, 1, 9, 1));

    private final BoolSetting returnSlot = add(new BoolSetting("Return",
            "Switch back to the slot you were on after placing. Off = stay on the last used slot", true));

    private final Runnable[] full = {this::placeObsidian, this::placeCrystal, this::restore};
    private final Runnable[] crystalOnly = {this::placeCrystal, this::restore};

    private BlockHitResult base;
    private BlockPos obsidianPos;
    private int previousSlot;

    public AutoCrystal() {
        super("Auto Crystal", "Key: places obsidian on the block you look at, then a crystal on it", Category.COMBAT);
    }

    @Override
    protected void onDisable() {
        runner.abort();
        restore();
    }

    @Override
    protected void tick(MinecraftClient mc) {
        if (!pressed() || runner.running() || mc.currentScreen != null) return;
        ClientPlayerEntity player = mc.player;
        BlockHitResult hit = PlayerActions.targetedBlock(mc);
        if (hit == null) return;

        BlockState state = mc.world.getBlockState(hit.getBlockPos());
        boolean solidBase = state.isOf(Blocks.OBSIDIAN) || state.isOf(Blocks.BEDROCK);
        int crystal = TotemUtil.hotbarIndex(crystalSlot);
        int obsidian = TotemUtil.hotbarIndex(obsidianSlot);
        if (!player.getInventory().getStack(crystal).isOf(Items.END_CRYSTAL)) return;
        if (solidBase) {
            obsidianPos = hit.getBlockPos();
        } else {
            if (!player.getInventory().getStack(obsidian).isOf(Items.OBSIDIAN)) return;
            obsidianPos = hit.getBlockPos().offset(hit.getSide());
            if (!mc.world.getBlockState(obsidianPos).isReplaceable()) return;
        }
        base = hit;
        previousSlot = PlayerActions.selectedSlot(player);
        runner.start(solidBase ? crystalOnly : full);
    }

    private void placeObsidian() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || base == null) return;
        PlayerActions.selectSlot(mc.player, TotemUtil.hotbarIndex(obsidianSlot));
        PlayerActions.useOnBlock(mc, Hand.MAIN_HAND, base);
    }

    private void placeCrystal() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || obsidianPos == null) return;
        PlayerActions.selectSlot(mc.player, TotemUtil.hotbarIndex(crystalSlot));
        PlayerActions.useOnBlock(mc, Hand.MAIN_HAND, PlayerActions.topOf(obsidianPos));
    }

    private void restore() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (returnSlot.get() && mc.player != null && base != null) PlayerActions.selectSlot(mc.player, previousSlot);
        base = null;
        obsidianPos = null;
    }
}
