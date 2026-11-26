package dev.ramoza.client.hud;

import dev.ramoza.client.util.MathUtil;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.network.ServerInfo;

/**
 * Live numbers for the watermark. The only place of the HUD that reads game state.
 * <ul>
 *   <li>FPS: {@code MinecraftClient.getCurrentFps()}.</li>
 *   <li>Ping: latency from the player list entry.</li>
 *   <li>BPS: horizontal distance per client tick x 20, smoothed.</li>
 *   <li>TPS: ESTIMATE. A vanilla server sends a time update every 20 server ticks, so TPS = 20 / (seconds between
 *       two updates), averaged over the last five intervals. Servers that do not send time updates on a 20 tick
 *       schedule, or heavy network jitter, make it inaccurate. Shown as "--" until two updates arrived.</li>
 * </ul>
 */
public final class HudStats {
    private static final int RING = 6;
    private static final long[] ring = new long[RING];
    private static int count;
    private static double tps = Double.NaN;

    private static double bps;
    private static double lastX, lastZ;
    private static boolean hasLast;

    private HudStats() {}

    /** Resets everything (joining / leaving a world). */
    public static void reset() {
        count = 0;
        tps = Double.NaN;
        bps = 0;
        hasLast = false;
    }

    /** Called (main thread) when a world time update packet is handled. */
    public static void onWorldTimeUpdate() {
        long now = System.nanoTime();
        if (count == RING) {
            System.arraycopy(ring, 1, ring, 0, RING - 1);
            count--;
        }
        ring[count++] = now;
        if (count >= 2) {
            double seconds = (ring[count - 1] - ring[0]) / 1.0e9;
            if (seconds > 0.0) tps = MathUtil.clamp(20.0 * (count - 1) / seconds, 0.0, 20.0);
        }
    }

    /** Called once per client tick. */
    public static void tick(MinecraftClient mc) {
        if (mc.player == null) {
            hasLast = false;
            bps = 0;
            return;
        }
        double x = mc.player.getX();
        double z = mc.player.getZ();
        if (hasLast) {
            double speed = Math.hypot(x - lastX, z - lastZ) * 20.0;
            if (speed > 200.0) speed = 0.0; // teleport
            bps += (speed - bps) * 0.3;
            if (bps < 0.05) bps = 0.0;
        }
        lastX = x;
        lastZ = z;
        hasLast = true;
    }

    public static double tps() { return tps; }
    public static double bps() { return bps; }

    public static int fps(MinecraftClient mc) { return mc.getCurrentFps(); }

    public static int ping(MinecraftClient mc) {
        ClientPlayNetworkHandler handler = mc.getNetworkHandler();
        if (handler == null || mc.player == null) return 0;
        PlayerListEntry entry = handler.getPlayerListEntry(mc.player.getUuid());
        return entry == null ? 0 : entry.getLatency();
    }

    public static String server(MinecraftClient mc) {
        ServerInfo info = mc.getCurrentServerEntry();
        return info == null ? "Singleplayer" : info.address;
    }
}
