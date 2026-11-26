package dev.ramoza.client.module.combat;

import dev.ramoza.client.module.Category;
import dev.ramoza.client.module.MacroModule;
import dev.ramoza.client.setting.ModeSetting;
import dev.ramoza.client.setting.NumberSetting;
import dev.ramoza.client.util.AnchorAim;
import dev.ramoza.client.util.PlayerActions;
import dev.ramoza.client.util.TotemUtil;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * Activate Key (press), aiming at a block: places a respawn anchor on the face you look at, charges it with
 * glowstone, switches to the Totem Slot and right-clicks it (that is what makes it explode), then returns to your slot.
 * If you already look at an anchor, the placing step is skipped. Does nothing in the Nether (anchors do not explode there).
 * <p>
 * Mode: Silent clicks the blocks chosen at the start even if you look away. Legit clicks only where you really look
 * (live ray at every step) and stops, returning your slot, as soon as you look away from the expected block.
 */
public final class AnchorMacro extends MacroModule {
    private final ModeSetting mode = add(AnchorAim.modeSetting());
    private final NumberSetting anchorSlot = add(new NumberSetting("Anchor Slot",
            "Hotbar slot (1-9) with respawn anchors", 1, 1, 9, 1));
    private final NumberSetting glowstoneSlot = add(new NumberSetting("GlowStone Slot",
            "Hotbar slot (1-9) with glowstone", 2, 1, 9, 1));
    private final NumberSetting totemSlot = add(new NumberSetting("Totem Slot",
            "Hotbar slot (1-9) with a totem. It is held while the anchor is exploded", 9, 1, 9, 1));

    private final Runnable[] full = {this::place, this::charge, this::explode, this::restore};
    private final Runnable[] existing = {this::charge, this::explode, this::restore};

    private BlockHitResult base;
    private BlockPos anchorPos;
    private int previousSlot;

    public AnchorMacro() {
        super("Anchor Macro", "Key: places an anchor, charges it and explodes it", Category.COMBAT);
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
        if (mc.world.getRegistryKey() == World.NETHER) return; // anchors do not explode in the Nether
        boolean isAnchor = mc.world.getBlockState(hit.getBlockPos()).isOf(Blocks.RESPAWN_ANCHOR);
        if (!isAnchor && !player.getInventory().getStack(TotemUtil.hotbarIndex(anchorSlot)).isOf(Items.RESPAWN_ANCHOR)) return;
        if (!player.getInventory().getStack(TotemUtil.hotbarIndex(glowstoneSlot)).isOf(Items.GLOWSTONE)) return;
        if (!TotemUtil.isTotem(player.getInventory().getStack(TotemUtil.hotbarIndex(totemSlot)))) return;
        if (isAnchor) {
            anchorPos = hit.getBlockPos();
        } else {
            anchorPos = hit.getBlockPos().offset(hit.getSide());
            if (!mc.world.getBlockState(anchorPos).isReplaceable()) return;
        }
        base = hit;
        previousSlot = PlayerActions.selectedSlot(player);
        runner.start(isAnchor ? existing : full);
    }

    /** Silent: the stored click. Legit: the live ray on the stored block and face (null = looking away). */
    private BlockHitResult placeClick(MinecraftClient mc) {
        return AnchorAim.isLegit(mode) ? AnchorAim.looking(mc, base.getBlockPos(), base.getSide()) : base;
    }

    /** Silent: top face of the anchor. Legit: the live ray on the anchor (null = looking away). */
    private BlockHitResult anchorClick(MinecraftClient mc) {
        return AnchorAim.isLegit(mode) ? AnchorAim.looking(mc, anchorPos) : PlayerActions.topOf(anchorPos);
    }

    private void cancel() {
        runner.abort();
        restore();
    }

    private void place() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || base == null) return;
        BlockHitResult click = placeClick(mc);
        if (click == null) { cancel(); return; }
        PlayerActions.selectSlot(mc.player, TotemUtil.hotbarIndex(anchorSlot));
        PlayerActions.useOnBlock(mc, Hand.MAIN_HAND, click);
    }

    private void charge() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || anchorPos == null) return;
        BlockHitResult click = anchorClick(mc);
        if (click == null) { cancel(); return; }
        PlayerActions.selectSlot(mc.player, TotemUtil.hotbarIndex(glowstoneSlot));
        PlayerActions.useOnBlock(mc, Hand.MAIN_HAND, click);
    }

    private void explode() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || anchorPos == null) return;
        BlockHitResult click = anchorClick(mc);
        if (click == null) { cancel(); return; }
        PlayerActions.selectSlot(mc.player, TotemUtil.hotbarIndex(totemSlot));
        PlayerActions.useOnBlock(mc, Hand.MAIN_HAND, click);
    }

    private void restore() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player != null && base != null) PlayerActions.selectSlot(mc.player, previousSlot);
        base = null;
        anchorPos = null;
    }
}
