package dev.ramoza.client.module.combat;

import dev.ramoza.client.module.Category;
import dev.ramoza.client.module.MacroModule;
import dev.ramoza.client.setting.ModeSetting;
import dev.ramoza.client.setting.NumberSetting;
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
 * Like Anchor Macro, twice: place -> charge -> explode, then the second anchor goes into the spot the first one
 * occupied (Second Spot = Same: placed the moment the first anchor is gone) or one block below it (Lower: placed right
 * after the first explode click). Any solid neighbouring block of that spot is used as the support to click on,
 * so it also works when the explosion removed the block the first anchor stood on. The second anchor is charged and
 * exploded, then you go back to your slot. Nothing is placed inside your own hitbox. If no spot with a support
 * appears within about a second the macro stops.
 */
public final class DoubleAnchor extends MacroModule {
    private static final int WAIT_TICKS = 20;

    private final NumberSetting anchorSlot = add(new NumberSetting("Anchor Slot",
            "Hotbar slot (1-9) with respawn anchors (two are used)", 1, 1, 9, 1));
    private final NumberSetting glowstoneSlot = add(new NumberSetting("GlowStone Slot",
            "Hotbar slot (1-9) with glowstone (two are used)", 2, 1, 9, 1));
    private final NumberSetting totemSlot = add(new NumberSetting("Totem Slot",
            "Hotbar slot (1-9) with a totem. It is held while an anchor is exploded", 9, 1, 9, 1));

    private final ModeSetting secondSpot = add(new ModeSetting("Second Spot",
            "Same: the second anchor goes where the first one was, as soon as it is gone. Lower: one block below, right after the first explode",
            "Same", "Same", "Lower"));

    private final Runnable[] first = {this::place, this::charge, this::explode};
    private final Runnable[] second = {this::place, this::charge, this::explode, this::restore};

    private static final int IDLE = 0, FIRST = 1, WAIT = 2, SECOND = 3;
    private int phase = IDLE;
    private int waited;
    private BlockHitResult base;
    private BlockPos anchorPos;
    private int previousSlot;

    public DoubleAnchor() {
        super("Double Anchor", "Key: anchor, charge, explode, then a second anchor on the same spot", Category.COMBAT);
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

    @Override
    protected void tick(MinecraftClient mc) {
        ClientPlayerEntity player = mc.player;
        switch (phase) {
            case IDLE -> start(mc, player);
            case FIRST -> {
                if (!runner.running()) {
                    phase = WAIT;
                    waited = 0;
                }
            }
            case WAIT -> {
                waited++;
                BlockPos spot = "Lower".equals(secondSpot.get()) ? anchorPos.down() : anchorPos;
                BlockHitResult click = supportFor(mc, player, spot);
                if (click != null) {
                    anchorPos = spot;
                    base = click;
                    phase = SECOND;
                    runner.start(second);
                } else if (waited > WAIT_TICKS) {
                    PlayerActions.hint(player, "Double Anchor: no free spot with a support for the second anchor");
                    finish();
                }
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
        if (mc.world.getRegistryKey() == World.NETHER) {
            PlayerActions.hint(player, "Double Anchor: anchors do not explode in the Nether");
            return;
        }
        if (mc.world.getBlockState(hit.getBlockPos()).isOf(Blocks.RESPAWN_ANCHOR)) {
            PlayerActions.hint(player, "Double Anchor: look at the block you want to place the anchor on");
            return;
        }
        if (!player.getInventory().getStack(TotemUtil.hotbarIndex(anchorSlot)).isOf(Items.RESPAWN_ANCHOR)) {
            PlayerActions.hint(player, "Double Anchor: no respawn anchor in the Anchor Slot");
            return;
        }
        if (!player.getInventory().getStack(TotemUtil.hotbarIndex(glowstoneSlot)).isOf(Items.GLOWSTONE)) {
            PlayerActions.hint(player, "Double Anchor: no glowstone in the GlowStone Slot");
            return;
        }
        if (!TotemUtil.isTotem(player.getInventory().getStack(TotemUtil.hotbarIndex(totemSlot)))) {
            PlayerActions.hint(player, "Double Anchor: no totem in the Totem Slot");
            return;
        }
        anchorPos = hit.getBlockPos().offset(hit.getSide());
        if (!mc.world.getBlockState(anchorPos).isReplaceable()) return;
        base = hit;
        previousSlot = PlayerActions.selectedSlot(player);
        phase = FIRST;
        runner.start(first);
    }

    /**
     * A click that places a block at {@code spot}: the spot must be free (replaceable, not in your hitbox) and have a
     * solid neighbour to click on. Prefers clicking the block below, then the sides, then above. Null if impossible.
     */
    private static BlockHitResult supportFor(MinecraftClient mc, ClientPlayerEntity player, BlockPos spot) {
        if (!mc.world.getBlockState(spot).isReplaceable()) return null;
        if (player.getBoundingBox().intersects(new Box(spot))) return null;
        Direction[] order = {Direction.DOWN, Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST, Direction.UP};
        for (Direction d : order) {
            BlockPos neighbour = spot.offset(d);
            if (mc.world.getBlockState(neighbour).isReplaceable()) continue;
            Direction face = d.getOpposite(); // the face of the neighbour that looks at the spot
            Vec3d at = Vec3d.ofCenter(neighbour).add(Vec3d.of(face.getVector()).multiply(0.5));
            return new BlockHitResult(at, face, neighbour, false);
        }
        return null;
    }

    private void finish() {
        runner.abort();
        restore();
        phase = IDLE;
    }

    private void place() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || base == null) return;
        PlayerActions.selectSlot(mc.player, TotemUtil.hotbarIndex(anchorSlot));
        PlayerActions.useOnBlock(mc, Hand.MAIN_HAND, base);
    }

    private void charge() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || anchorPos == null) return;
        PlayerActions.selectSlot(mc.player, TotemUtil.hotbarIndex(glowstoneSlot));
        PlayerActions.useOnBlock(mc, Hand.MAIN_HAND, PlayerActions.topOf(anchorPos));
    }

    private void explode() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || anchorPos == null) return;
        PlayerActions.selectSlot(mc.player, TotemUtil.hotbarIndex(totemSlot));
        PlayerActions.useOnBlock(mc, Hand.MAIN_HAND, PlayerActions.topOf(anchorPos));
    }

    private void restore() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player != null && base != null) PlayerActions.selectSlot(mc.player, previousSlot);
        base = null;
    }
}
