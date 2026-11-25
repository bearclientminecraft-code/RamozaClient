package dev.ramoza.client.module.combat;

import dev.ramoza.client.mixin.HandledScreenAccessor;
import dev.ramoza.client.module.Category;
import dev.ramoza.client.module.ClientModule;
import dev.ramoza.client.module.DelayTimer;
import dev.ramoza.client.setting.BoolSetting;
import dev.ramoza.client.setting.NumberSetting;
import dev.ramoza.client.util.PlayerActions;
import dev.ramoza.client.util.TotemUtil;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.screen.slot.Slot;

/**
 * In the open inventory: hovering a totem (no click needed) moves it after Delay +/- Random Delay.
 * OffHand Totem on: while the offhand has no totem the hovered one goes there first; afterwards it goes to
 * the Totem Slot (only if that slot does not already hold a totem).
 * Works in the player inventory screen only (its slot numbering is fixed).
 */
public final class HoverTotem extends ClientModule {
    private final NumberSetting totemSlot = add(new NumberSetting("Totem Slot",
            "Hotbar slot (1-9) the hovered totem is moved to", 9, 1, 9, 1));
    private final BoolSetting offhand = add(new BoolSetting("OffHand Totem",
            "Fill the offhand first, then the Totem Slot", false));
    private final DelayTimer delay = DelayTimer.attach(this, 0);

    private int hovered = -1; // screen slot under the cursor that holds a totem
    private boolean acted;

    public HoverTotem() {
        super("Hover Totem", "Hover a totem in the inventory and it moves to your slot without a click", Category.COMBAT);
    }

    @Override
    protected void onEnable() { hovered = -1; acted = false; }

    @Override
    public void onTick() {
        MinecraftClient mc = MinecraftClient.getInstance();
        ClientPlayerEntity player = mc.player;
        if (player == null || mc.interactionManager == null || !(mc.currentScreen instanceof InventoryScreen screen)) {
            hovered = -1;
            return;
        }
        int target = TotemUtil.hotbarIndex(totemSlot);
        int targetScreen = PlayerActions.screenSlot(target);

        Slot focused = ((HandledScreenAccessor) screen).ramoza$getFocusedSlot();
        int id = focused == null ? -1 : screen.getScreenHandler().slots.indexOf(focused);
        // 9-35 main inventory, 36-44 hotbar; crafting, armor and offhand slots are ignored
        if (id < 9 || id > 44 || id == targetScreen || !TotemUtil.isTotem(focused.getStack())) {
            hovered = -1;
            return;
        }
        if (id != hovered) { // cursor just arrived on a totem: new wait, new roll
            hovered = id;
            acted = false;
            delay.restart();
            return;
        }
        if (acted || !delay.elapsed()) return;
        acted = true;

        if (offhand.get() && !TotemUtil.isTotem(player.getOffHandStack())) {
            TotemUtil.swapToOffhand(mc, id);
        } else if (!TotemUtil.isTotem(player.getInventory().getStack(target))) {
            TotemUtil.swapToHotbar(mc, id, target);
        }
    }
}
