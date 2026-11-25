package dev.ramoza.client.module.combat;

import dev.ramoza.client.module.Category;
import dev.ramoza.client.module.ClientModule;
import dev.ramoza.client.setting.NumberSetting;
import dev.ramoza.client.util.TotemUtil;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.network.ClientPlayerEntity;

/**
 * You open the inventory (E): once, on opening, a totem from the main inventory is moved into the Totem Slot.
 * Does nothing when that slot already holds a totem or no totem is in the inventory.
 */
public final class AutoInventoryTotem extends ClientModule {
    private final NumberSetting totemSlot = add(new NumberSetting("Totem Slot",
            "Hotbar slot (1-9) the totem is moved to", 9, 1, 9, 1));

    private boolean wasOpen;

    public AutoInventoryTotem() {
        super("Auto Inventory Totem", "Opening the inventory moves a totem to the Totem Slot", Category.COMBAT);
    }

    @Override
    protected void onEnable() { wasOpen = false; }

    @Override
    public void onTick() {
        MinecraftClient mc = MinecraftClient.getInstance();
        ClientPlayerEntity player = mc.player;
        boolean open = player != null && mc.interactionManager != null && mc.currentScreen instanceof InventoryScreen;
        boolean justOpened = open && !wasOpen;
        wasOpen = open;
        if (!justOpened) return;

        int target = TotemUtil.hotbarIndex(totemSlot);
        if (TotemUtil.isTotem(player.getInventory().getStack(target))) return;
        int source = TotemUtil.findTotem(player, false, target);
        if (source >= 0) TotemUtil.swapToHotbar(mc, source, target);
    }
}
