package dev.ramoza.client.util;

import dev.ramoza.client.setting.ModeSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

/**
 * Shared "Mode" logic of the anchor modules.
 * <ul>
 *   <li><b>Silent</b>: clicks go to the blocks chosen when the macro started, wherever you look.</li>
 *   <li><b>Legit</b>: every click uses the block you are looking at right now (a live ray from your eyes along your
 *       current rotation). If it is not the expected block the macro stops.</li>
 * </ul>
 * The ray is cast again at every step instead of reading {@code mc.crosshairTarget}, because the crosshair is only
 * refreshed once per rendered frame and would still show the old block when several steps run in one tick.
 */
public final class AnchorAim {
    public static final String SILENT = "Silent";
    public static final String LEGIT = "Legit";
    /** Vanilla survival block interaction range. */
    private static final double REACH = 4.5;

    private AnchorAim() {}

    public static ModeSetting modeSetting() {
        return new ModeSetting("Mode",
                "Silent: clicks the blocks even if you look away. Legit: every click goes where you really look, "
                        + "if you look away the macro stops",
                SILENT, SILENT, LEGIT);
    }

    public static boolean isLegit(ModeSetting mode) { return LEGIT.equals(mode.get()); }

    /** The block under your eyes' ray right now, or null when the ray hits no block within reach. */
    public static BlockHitResult looking(MinecraftClient mc) {
        if (mc.player == null) return null;
        HitResult h = mc.player.raycast(REACH, 1.0f, false);
        return h instanceof BlockHitResult b && b.getType() == HitResult.Type.BLOCK ? b : null;
    }

    /** The live ray hit if (and only if) it is on the block {@code pos}; otherwise null. */
    public static BlockHitResult looking(MinecraftClient mc, BlockPos pos) {
        BlockHitResult b = looking(mc);
        return b != null && b.getBlockPos().equals(pos) ? b : null;
    }

    /** The live ray hit if it is on the face {@code side} of block {@code pos}; otherwise null. */
    public static BlockHitResult looking(MinecraftClient mc, BlockPos pos, Direction side) {
        BlockHitResult b = looking(mc, pos);
        return b != null && b.getSide() == side ? b : null;
    }
}
