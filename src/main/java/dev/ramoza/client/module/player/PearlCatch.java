package dev.ramoza.client.module.player;

import dev.ramoza.client.module.Category;
import dev.ramoza.client.module.MacroModule;
import dev.ramoza.client.setting.BoolSetting;
import dev.ramoza.client.setting.NumberSetting;
import dev.ramoza.client.util.PlayerActions;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;

/**
 * Activate Key: pearl slot + throw -> pause -> wind charge slot + throw -> pause -> back to the slot you were on.
 * Nothing happens unless the pearl slot holds an ender pearl and the wind charge slot a wind charge.
 */
public final class PearlCatch extends MacroModule {
    private final NumberSetting pearlSlot = add(new NumberSetting("Pearl Slot",
            "Hotbar slot (1-9) with the ender pearl", 2, 1, 9, 1));
    private final NumberSetting windSlot = add(new NumberSetting("WindCharge Slot",
            "Hotbar slot (1-9) with the wind charge", 3, 1, 9, 1));
    private final BoolSetting lookDown = add(new BoolSetting("Charge Look Down",
            "Throw the wind charge straight down (pitch is put back right after the throw)", true));

    private final Runnable[] sequence = {this::throwPearl, this::throwCharge, this::restore};
    private int previousSlot;

    public PearlCatch() {
        super("Pearl Catch", "Key: throw a pearl, then a wind charge to launch yourself up", Category.PLAYER);
    }

    @Override
    protected void tick(MinecraftClient mc) {
        if (!pressed() || runner.running()) return;
        ClientPlayerEntity player = mc.player;
        if (!player.getInventory().getStack(slot(pearlSlot)).isOf(Items.ENDER_PEARL)) return;
        if (!player.getInventory().getStack(slot(windSlot)).isOf(Items.WIND_CHARGE)) return;
        previousSlot = PlayerActions.selectedSlot(player);
        runner.start(sequence);
    }

    private void throwPearl() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) return;
        PlayerActions.selectSlot(mc.player, slot(pearlSlot));
        PlayerActions.use(mc, Hand.MAIN_HAND);
    }

    private void throwCharge() {
        MinecraftClient mc = MinecraftClient.getInstance();
        ClientPlayerEntity player = mc.player;
        if (player == null) return;
        PlayerActions.selectSlot(player, slot(windSlot));
        if (lookDown.get()) {
            float pitch = player.getPitch();
            player.setPitch(90f);
            PlayerActions.use(mc, Hand.MAIN_HAND);
            player.setPitch(pitch);
        } else {
            PlayerActions.use(mc, Hand.MAIN_HAND);
        }
    }

    private void restore() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player != null) PlayerActions.selectSlot(mc.player, previousSlot);
    }

    private static int slot(NumberSetting s) { return s.get().intValue() - 1; }
}
