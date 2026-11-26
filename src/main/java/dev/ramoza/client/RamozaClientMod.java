package dev.ramoza.client;

import dev.ramoza.client.core.RamozaCore;
import dev.ramoza.client.gui.RamozaScreen;
import dev.ramoza.client.gui.render.Gfx;
import dev.ramoza.client.gui.render.McRenderer;
import dev.ramoza.client.hud.HudStats;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;

public final class RamozaClientMod implements ClientModInitializer {
    public static final String MOD_ID = "ramozaclient";

    private static RamozaCore core;
    private static KeyBinding openKey;
    private static KeyBinding restoreKey;

    public static RamozaCore core() { return core; }

    @Override
    public void onInitializeClient() {
        core = new RamozaCore(FabricLoader.getInstance().getConfigDir());

        KeyBinding.Category category = KeyBinding.Category.create(Identifier.of(MOD_ID, "main"));
        openKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.ramozaclient.open_gui", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_RIGHT_SHIFT, category));
        restoreKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.ramozaclient.restore_gui", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_END, category));

        // watermark HUD (the ClickGUI draws it itself while it is open)
        McRenderer hudRenderer = new McRenderer();
        Gfx hudGfx = new Gfx(hudRenderer);
        HudRenderCallback.EVENT.register((context, tickCounter) -> {
            MinecraftClient mc = MinecraftClient.getInstance();
            if (mc.player == null || mc.options.hudHidden || mc.currentScreen instanceof RamozaScreen) return;
            hudRenderer.begin(context, mc.textRenderer);
            core.watermark.render(hudGfx, context.getScaledWindowWidth(), context.getScaledWindowHeight(), 0f, -1, -1);
        });
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> HudStats.reset());
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> HudStats.reset());
        ClientTickEvents.END_CLIENT_TICK.register(HudStats::tick);

        // modules tick before the game reads input, so Sprint / No Jump Delay take effect this tick
        ClientTickEvents.START_CLIENT_TICK.register(client -> core.modules.tick());

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (restoreKey.wasPressed()) core.setGuiHidden(false);
            while (openKey.wasPressed()) {
                if (client.currentScreen == null && !core.isGuiHidden()) {
                    client.setScreen(new RamozaScreen(core,
                            () -> KeyBindingHelper.getBoundKeyOf(openKey).getCode()));
                }
            }
        });
    }
}
