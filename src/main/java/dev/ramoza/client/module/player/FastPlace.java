package dev.ramoza.client.module.player;

import dev.ramoza.client.mixin.MinecraftClientAccessor;
import dev.ramoza.client.module.Category;
import dev.ramoza.client.module.ClientModule;
import dev.ramoza.client.module.DelayTimer;
import dev.ramoza.client.setting.BoolSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.BlockItem;

/**
 * Replaces the vanilla 4 tick pause between right-click uses while the use key is held with
 * {@code Delay (ms)} +/- {@code Random Delay}. The pause is re-rolled after every use.
 */
public final class FastPlace extends ClientModule {
    private final BoolSetting blocksOnly = add(new BoolSetting("Blocks Only",
            "Only speed up when holding a block (keeps pearls, potions, snowballs at vanilla speed)", true));
    private final DelayTimer delay = DelayTimer.attach(this, 0);

    public FastPlace() {
        super("Fast Place", "Places blocks faster while you hold right click", Category.PLAYER);
    }

    @Override
    public void onTick() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.currentScreen != null || !mc.options.useKey.isPressed()) return;
        if (blocksOnly.get() && !(mc.player.getMainHandStack().getItem() instanceof BlockItem)) return;

        MinecraftClientAccessor acc = (MinecraftClientAccessor) mc;
        if (delay.elapsed()) {
            acc.ramoza$setItemUseCooldown(0); // the game places in this tick
            delay.restart();
        } else {
            // vanilla counts the cooldown down once before reading input, so 2 still blocks this tick
            acc.ramoza$setItemUseCooldown(2);
        }
    }
}
