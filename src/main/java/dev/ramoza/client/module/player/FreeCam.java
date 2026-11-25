package dev.ramoza.client.module.player;

import dev.ramoza.client.RamozaClientMod;
import dev.ramoza.client.core.RamozaCore;
import dev.ramoza.client.module.Category;
import dev.ramoza.client.module.ClientModule;
import dev.ramoza.client.setting.NumberSetting;
import dev.ramoza.client.util.KeyInput;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.GameOptions;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.util.math.Vec3d;

/**
 * Detaches the camera from the player. The player stays where it is (movement keys and mouse look are
 * redirected to the camera). Camera placement is done by {@code CameraMixin}, mouse look by {@code EntityMixin}.
 */
public final class FreeCam extends ClientModule {
    private final NumberSetting speed = add(new NumberSetting("Speed",
            "Camera speed in blocks per second", 10, 1, 50, 1));

    private boolean initialized;
    private long lastNanos;
    private double x, y, z;
    private float yaw, pitch;

    public FreeCam() {
        super("Free Cam", "Fly the camera around while your player stays in place", Category.PLAYER);
    }

    /** The enabled instance, or null. Used by mixins. */
    public static FreeCam active() {
        RamozaCore core = RamozaClientMod.core();
        if (core == null) return null;
        FreeCam m = core.modules.get(FreeCam.class);
        return m != null && m.isEnabled() ? m : null;
    }

    public double x() { return x; }
    public double y() { return y; }
    public double z() { return z; }
    public float yaw() { return yaw; }
    public float pitch() { return pitch; }

    @Override
    protected void onEnable() {
        initialized = false; // the start pose is taken from the player on the first frame
        lastNanos = 0;
    }

    @Override
    protected void onDisable() {
        initialized = false;
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null || mc.options == null) return;
        // movement keys were forced up while active: give them back their real physical state
        for (KeyBinding k : movementKeys(mc.options)) k.setPressed(KeyInput.isDown(k));
    }

    @Override
    public void onTick() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.world == null) {
            setEnabled(false); // left the world (or config enabled it at startup)
            return;
        }
        // the player must not move: release the movement keys before the game reads input
        for (KeyBinding k : movementKeys(mc.options)) k.setPressed(false);
    }

    /** Mouse look, same sensitivity factor as vanilla {@code Entity.changeLookDirection}. */
    public void look(double dx, double dy) {
        yaw += (float) dx * 0.15f;
        pitch = Math.max(-90f, Math.min(90f, pitch + (float) dy * 0.15f));
    }

    /** Per-frame camera update (frame-rate independent). Returns false if there is no pose yet. */
    public boolean updateCamera() {
        MinecraftClient mc = MinecraftClient.getInstance();
        ClientPlayerEntity player = mc.player;
        if (player == null) return false;
        if (!initialized) {
            Vec3d eye = player.getEyePos();
            x = eye.x;
            y = eye.y;
            z = eye.z;
            yaw = player.getYaw();
            pitch = player.getPitch();
            initialized = true;
            lastNanos = 0;
        }
        long now = System.nanoTime();
        double dt = lastNanos == 0 ? 0 : Math.min((now - lastNanos) / 1_000_000_000.0, 0.1);
        lastNanos = now;

        if (mc.currentScreen == null && dt > 0) {
            GameOptions o = mc.options;
            double forward = axis(o.forwardKey) - axis(o.backKey);
            double strafe = axis(o.rightKey) - axis(o.leftKey);
            double vertical = axis(o.jumpKey) - axis(o.sneakKey);
            double rad = Math.toRadians(yaw);
            double sin = Math.sin(rad), cos = Math.cos(rad);
            // Minecraft: yaw 0 looks along +Z, forward = (-sin, cos), right = (-cos, -sin)
            double dx = -sin * forward - cos * strafe;
            double dz = cos * forward - sin * strafe;
            double len = Math.sqrt(dx * dx + dz * dz);
            if (len > 1) {
                dx /= len;
                dz /= len;
            }
            double step = speed.get() * dt;
            x += dx * step;
            y += vertical * step;
            z += dz * step;
        }
        return true;
    }

    private static double axis(KeyBinding key) { return KeyInput.isDown(key) ? 1 : 0; }

    private static KeyBinding[] movementKeys(GameOptions o) {
        return new KeyBinding[] {o.forwardKey, o.backKey, o.leftKey, o.rightKey, o.jumpKey, o.sneakKey};
    }
}
