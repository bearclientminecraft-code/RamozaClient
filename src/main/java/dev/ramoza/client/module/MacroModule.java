package dev.ramoza.client.module;

import dev.ramoza.client.setting.KeybindSetting;
import dev.ramoza.client.util.KeyInput;
import dev.ramoza.client.util.Keys;
import net.minecraft.client.MinecraftClient;

/**
 * Base for the macro modules. Every macro gets the same three settings: Activate Key, Delay (ms) 0-200 and
 * Random Delay 0-200, plus a {@link MacroRunner} that paces its steps.
 * <p>
 * Activate Key meaning: macros that start on a press use {@link #pressed}; macros that react to your own
 * attacks or falls use {@link #gate()} (key held, or always on when no key is bound).
 */
public abstract class MacroModule extends ClientModule {
    protected final KeybindSetting activateKey = add(new KeybindSetting("Activate Key",
            "Trigger key (see module description). Unbound = always armed for automatic modules", Keys.NONE));
    protected final DelayTimer delay = DelayTimer.attach(this, 0);
    protected final MacroRunner runner = new MacroRunner(delay);

    private boolean keyDown;
    private boolean wasDown;
    private boolean pressed;

    protected MacroModule(String name, String description, Category category) {
        super(name, description, category);
    }

    /** True for exactly one tick when the Activate Key goes down (no screen open). */
    protected final boolean pressed() { return pressed; }

    /** Armed state for automatic macros: key held, or always when no key is bound. */
    public final boolean gate() { return !activateKey.isBound() || keyDown; }

    @Override
    protected void onEnable() {
        keyDown = false;
        wasDown = false;
        pressed = false;
    }

    @Override
    protected void onDisable() { runner.flush(); }

    @Override
    public final void onTick() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.interactionManager == null) {
            runner.abort();
            keyDown = false;
            wasDown = false;
            pressed = false;
            return;
        }
        keyDown = mc.currentScreen == null && activateKey.isBound() && KeyInput.isDown(activateKey.get());
        pressed = keyDown && !wasDown;
        wasDown = keyDown;
        runner.beginTick();
        runner.tick();
        tick(mc);
    }

    /** Per-tick logic; player and interaction manager are non-null here. */
    protected abstract void tick(MinecraftClient mc);
}
