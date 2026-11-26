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
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

/**
 * Two anchors per press: place -> charge -> explode, twice.
 * <p>
 * Why a stone floor used to give only one explosion: the first explosion destroys weak blocks (stone) around the
 * anchor, so the second anchor had nothing to be placed on. Respawn anchors themselves survive explosions (same
 * blast resistance as obsidian). So the default Second Spot = <b>Beside</b> places the second anchor right after the
 * first, BEFORE anything is charged or exploded, on a free block next to it that has a solid, non-anchor neighbour as
 * support; then both are charged and exploded. It does not matter what the blocks are made of. Nothing is placed in
 * the air and nothing inside your own hitbox; if no such spot exists only one anchor is used.
 * <p>
 * Second Spot = Same / Lower keeps the old behaviour: the second anchor goes into the spot of the first one (or one
 * below) after the first exploded, using any solid neighbour as support. Those two depend on the support surviving.
 * <p>
 * Mode: Silent clicks the chosen blocks even if you look away. Legit clicks only where you really look (live ray at
 * every step) and stops when you look away. In Legit the second anchor goes on the block you look at after the first
 * explosion (within 3 blocks of the first one), whatever Second Spot says, because two different anchors cannot both
 * be under one crosshair.
 */
public final class DoubleAnchor extends MacroModule {
    private static final int WAIT_TICKS = 20;
    private static final double LEGIT_RANGE_SQ = 9.0; // 3 blocks

    private final ModeSetting mode = add(AnchorAim.modeSetting());
    private final NumberSetting anchorSlot = add(new NumberSetting("Anchor Slot",
            "Hotbar slot (1-9) with respawn anchors (two are used)", 1, 1, 9, 1));
    private final NumberSetting glowstoneSlot = add(new NumberSetting("GlowStone Slot",
            "Hotbar slot (1-9) with glowstone (two are used)", 2, 1, 9, 1));
    private final NumberSetting totemSlot = add(new NumberSetting("Totem Slot",
            "Hotbar slot (1-9) with a totem. It is held while an anchor is exploded", 9, 1, 9, 1));

    private final ModeSetting secondSpot = add(new ModeSetting("Second Spot",
            "Beside: second anchor is placed next to the first before the explosion, works on any block. "
                    + "Same / Lower: after the first explosion, in its spot / one block below (needs the support to survive)",
            "Beside", "Beside", "Same", "Lower"));

    private final Runnable[] beside = {this::place, this::placeSecond, this::charge, this::chargeSecond,
            this::explode, this::explodeSecond, this::restore};
    private final Runnable[] first = {this::place, this::charge, this::explode};
    private final Runnable[] second = {this::place, this::charge, this::explode, this::restore};

    private static final int IDLE = 0, FIRST = 1, WAIT = 2, SECOND = 3;
    private int phase = IDLE;
    private int waited;
    private boolean besideRun;
    private BlockHitResult base;
    private BlockPos anchorPos;
    private BlockHitResult secondBase;
    private BlockPos secondPos;
    private int previousSlot;

    public DoubleAnchor() {
        super("Double Anchor", "Key: two anchors, each charged and exploded", Category.COMBAT);
    }

    @Override
    protected void onEnable() {
        super.onEnable();
        phase = IDLE;
    }

    @Override
    protected void onDisable() {
        runner.abort();
        restore();
        phase = IDLE;
    }

    private boolean legit() { return AnchorAim.isLegit(mode); }

    @Override
    protected void tick(MinecraftClient mc) {
        ClientPlayerEntity player = mc.player;
        switch (phase) {
            case IDLE -> start(mc, player);
            case FIRST -> {
                if (!runner.running()) {
                    if (besideRun) {
                        phase = IDLE;
                    } else {
                        phase = WAIT;
                        waited = 0;
                    }
                }
            }
            case WAIT -> {
                waited++;
                if (legit()) {
                    BlockHitResult live = AnchorAim.looking(mc);
                    if (live != null && !mc.world.getBlockState(live.getBlockPos()).isOf(Blocks.RESPAWN_ANCHOR)) {
                        BlockPos spot = live.getBlockPos().offset(live.getSide());
                        if (free(mc, player, spot) && spot.getSquaredDistance(anchorPos) <= LEGIT_RANGE_SQ) {
                            anchorPos = spot;
                            base = live;
                            phase = SECOND;
                            runner.start(second);
                            return;
                        }
                    }
                } else {
                    BlockPos spot = "Lower".equals(secondSpot.get()) ? anchorPos.down() : anchorPos;
                    BlockHitResult click = supportFor(mc, player, spot);
                    if (click != null) {
                        anchorPos = spot;
                        base = click;
                        phase = SECOND;
                        runner.start(second);
                        return;
                    }
                }
                if (waited > WAIT_TICKS) finish();
            }
            case SECOND -> {
                if (!runner.running()) phase = IDLE;
            }
            default -> phase = IDLE;
        }
    }

    private void start(MinecraftClient mc, ClientPlayerEntity player) {
        if (!pressed() || mc.currentScreen != null) return;
        BlockHitResult hit = PlayerActions.targetedBlock(mc);
        if (hit == null) return;
        if (mc.world.getRegistryKey() == World.NETHER) return; // anchors do not explode in the Nether
        if (mc.world.getBlockState(hit.getBlockPos()).isOf(Blocks.RESPAWN_ANCHOR)) return;
        if (!player.getInventory().getStack(TotemUtil.hotbarIndex(anchorSlot)).isOf(Items.RESPAWN_ANCHOR)) return;
        if (!player.getInventory().getStack(TotemUtil.hotbarIndex(glowstoneSlot)).isOf(Items.GLOWSTONE)) return;
        if (!TotemUtil.isTotem(player.getInventory().getStack(TotemUtil.hotbarIndex(totemSlot)))) return;
        anchorPos = hit.getBlockPos().offset(hit.getSide());
        if (!mc.world.getBlockState(anchorPos).isReplaceable()) return;
        base = hit;
        secondBase = null;
        secondPos = null;
        previousSlot = PlayerActions.selectedSlot(player);
        besideRun = !legit() && "Beside".equals(secondSpot.get());
        phase = FIRST;
        runner.start(besideRun ? beside : first);
    }

    /** A spot where a block can be placed: replaceable and not inside your own hitbox. */
    private static boolean free(MinecraftClient mc, ClientPlayerEntity player, BlockPos spot) {
        return mc.world.getBlockState(spot).isReplaceable() && !player.getBoundingBox().intersects(new Box(spot));
    }

    /**
     * A click that places a block at {@code spot}: the spot must be free and have a solid neighbour to click on.
     * Respawn anchors are never used as the support (clicking a charged one would explode it instead of placing).
     * Prefers clicking the block below, then the sides, then above. Null if impossible.
     */
    private static BlockHitResult supportFor(MinecraftClient mc, ClientPlayerEntity player, BlockPos spot) {
        if (!free(mc, player, spot)) return null;
        Direction[] order = {Direction.DOWN, Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST, Direction.UP};
        for (Direction d : order) {
            BlockPos neighbour = spot.offset(d);
            if (mc.world.getBlockState(neighbour).isReplaceable()) continue;
            if (mc.world.getBlockState(neighbour).isOf(Blocks.RESPAWN_ANCHOR)) continue;
            Direction face = d.getOpposite(); // the face of the neighbour that looks at the spot
            Vec3d at = Vec3d.ofCenter(neighbour).add(Vec3d.of(face.getVector()).multiply(0.5));
            return new BlockHitResult(at, face, neighbour, false);
        }
        return null;
    }

    private void finish() {
        cancel();
    }

    private void cancel() {
        runner.abort();
        restore();
        phase = IDLE;
    }

    private BlockHitResult placeClick(MinecraftClient mc) {
        return legit() ? AnchorAim.looking(mc, base.getBlockPos(), base.getSide()) : base;
    }

    private BlockHitResult anchorClick(MinecraftClient mc) {
        return legit() ? AnchorAim.looking(mc, anchorPos) : PlayerActions.topOf(anchorPos);
    }

    private void place() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || base == null) return;
        BlockHitResult click = placeClick(mc);
        if (click == null) { cancel(); return; }
        PlayerActions.selectSlot(mc.player, TotemUtil.hotbarIndex(anchorSlot));
        PlayerActions.useOnBlock(mc, Hand.MAIN_HAND, click);
    }

    /** Beside mode: the second anchor, placed while the first one is still uncharged. */
    private void placeSecond() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || anchorPos == null) return;
        Direction[] sides = {Direction.EAST, Direction.WEST, Direction.SOUTH, Direction.NORTH, Direction.UP};
        for (Direction d : sides) {
            BlockPos spot = anchorPos.offset(d);
            BlockHitResult click = supportFor(mc, mc.player, spot);
            if (click == null) continue;
            secondPos = spot;
            secondBase = click;
            PlayerActions.selectSlot(mc.player, TotemUtil.hotbarIndex(anchorSlot));
            PlayerActions.useOnBlock(mc, Hand.MAIN_HAND, click);
            return;
        }
        secondPos = null; // no spot with a support: only the first anchor is used
    }

    private void charge() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || anchorPos == null) return;
        BlockHitResult click = anchorClick(mc);
        if (click == null) { cancel(); return; }
        PlayerActions.selectSlot(mc.player, TotemUtil.hotbarIndex(glowstoneSlot));
        PlayerActions.useOnBlock(mc, Hand.MAIN_HAND, click);
    }

    private void chargeSecond() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || secondPos == null) return;
        PlayerActions.selectSlot(mc.player, TotemUtil.hotbarIndex(glowstoneSlot));
        PlayerActions.useOnBlock(mc, Hand.MAIN_HAND, PlayerActions.topOf(secondPos));
    }

    private void explode() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || anchorPos == null) return;
        BlockHitResult click = anchorClick(mc);
        if (click == null) { cancel(); return; }
        PlayerActions.selectSlot(mc.player, TotemUtil.hotbarIndex(totemSlot));
        PlayerActions.useOnBlock(mc, Hand.MAIN_HAND, click);
    }

    private void explodeSecond() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || secondPos == null) return;
        PlayerActions.selectSlot(mc.player, TotemUtil.hotbarIndex(totemSlot));
        PlayerActions.useOnBlock(mc, Hand.MAIN_HAND, PlayerActions.topOf(secondPos));
    }

    private void restore() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player != null && base != null) PlayerActions.selectSlot(mc.player, previousSlot);
        base = null;
        secondBase = null;
        secondPos = null;
    }
}
