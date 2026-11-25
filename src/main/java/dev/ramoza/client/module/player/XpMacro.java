package dev.ramoza.client.module.player;

import dev.ramoza.client.module.Category;
import dev.ramoza.client.module.ClientModule;
import dev.ramoza.client.module.DelayTimer;
import dev.ramoza.client.setting.BoolSetting;
import dev.ramoza.client.setting.KeybindSetting;
import dev.ramoza.client.setting.ModeSetting;
import dev.ramoza.client.util.KeyInput;
import dev.ramoza.client.util.Keys;
import dev.ramoza.client.util.PlayerActions;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.Hand;

/**
 * Throws Bottles o' Enchanting in a loop. The bottle can be in either hand, anywhere in the hotbar or
 * (optionally) in the main inventory: the macro finds it, throws it and puts everything back.
 * The delay between throws is {@code Delay (ms)} +/- {@code Random Delay}, rolled again for every throw.
 * It does not aim: look where you want the bottles to land (usually straight down).
 */
public final class XpMacro extends ClientModule {
    private final ModeSetting activation = add(new ModeSetting("Activation",
            "Always: throws while the module is on. Hold Key: only while the key is held",
            "Always", "Always", "Hold Key"));
    private final KeybindSetting key = add(new KeybindSetting("Key",
            "Used in Hold Key mode", Keys.NONE));
    private final BoolSetting searchInventory = add(new BoolSetting("Search Inventory",
            "Also take bottles from the main inventory (swaps them in and back)", true));
    private final DelayTimer delay = DelayTimer.attach(this, 50);

    public XpMacro() {
        super("XP Macro", "Spams Bottles o' Enchanting from anywhere in your inventory", Category.PLAYER);
    }

    @Override
    public void onTick() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.interactionManager == null || mc.currentScreen != null) return;
        if (activation.get().equals("Hold Key") && !KeyInput.isDown(key.get())) return;
        if (!delay.elapsed()) return;
        if (throwBottle(mc)) delay.restart();
    }

    private boolean throwBottle(MinecraftClient mc) {
        ClientPlayerEntity player = mc.player;
        if (player.getMainHandStack().isOf(Items.EXPERIENCE_BOTTLE)) return PlayerActions.use(mc, Hand.MAIN_HAND);
        if (player.getOffHandStack().isOf(Items.EXPERIENCE_BOTTLE)) return PlayerActions.use(mc, Hand.OFF_HAND);

        int hotbar = PlayerActions.findInHotbar(player, Items.EXPERIENCE_BOTTLE);
        if (hotbar >= 0) {
            int previous = PlayerActions.selectedSlot(player);
            PlayerActions.selectSlot(player, hotbar);
            boolean ok = PlayerActions.use(mc, Hand.MAIN_HAND);
            PlayerActions.selectSlot(player, previous);
            return ok;
        }

        if (!searchInventory.get()) return false;
        int inventory = PlayerActions.findInMain(player, Items.EXPERIENCE_BOTTLE);
        if (inventory < 0) return false;
        int selected = PlayerActions.selectedSlot(player);
        int syncId = player.playerScreenHandler.syncId;
        // SWAP with hotbar button N swaps the clicked slot with hotbar slot N; doing it twice restores both
        mc.interactionManager.clickSlot(syncId, inventory, selected, SlotActionType.SWAP, player);
        boolean ok = PlayerActions.use(mc, Hand.MAIN_HAND);
        mc.interactionManager.clickSlot(syncId, inventory, selected, SlotActionType.SWAP, player);
        return ok;
    }
}
