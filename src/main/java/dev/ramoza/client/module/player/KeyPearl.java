package dev.ramoza.client.module.player;

import dev.ramoza.client.module.Category;
import dev.ramoza.client.module.ClientModule;
import dev.ramoza.client.module.DelayTimer;
import dev.ramoza.client.setting.KeybindSetting;
import dev.ramoza.client.setting.NumberSetting;
import dev.ramoza.client.util.KeyInput;
import dev.ramoza.client.util.Keys;
import dev.ramoza.client.util.PlayerActions;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;

/**
 * One key press: switch to the pearl slot, throw the pearl, switch to the totem slot. The pause between
 * the three actions is {@code Delay (ms)} +/- {@code Random Delay}, rolled again for every action.
 * Nothing happens if the pearl slot does not hold an ender pearl.
 */
public final class KeyPearl extends ClientModule {
    private final KeybindSetting key = add(new KeybindSetting("Key",
            "Press to throw a pearl", Keys.NONE));
    private final NumberSetting pearlSlot = add(new NumberSetting("Pearl Slot",
            "Hotbar slot (1-9) with the ender pearl", 2, 1, 9, 1));
    private final NumberSetting totemSlot = add(new NumberSetting("Totem Slot",
            "Hotbar slot (1-9) to switch to after the throw", 1, 1, 9, 1));
    private final DelayTimer delay = DelayTimer.attach(this, 0);

    private enum Step { IDLE, THROW, RETURN }

    private Step step = Step.IDLE;
    private boolean wasDown;

    public KeyPearl() {
        super("Key Pearl", "Key: switch to pearl, throw it, switch back to the totem slot", Category.PLAYER);
    }

    @Override
    protected void onEnable() { step = Step.IDLE; wasDown = false; }

    @Override
    public void onTick() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.interactionManager == null) {
            step = Step.IDLE;
            wasDown = false;
            return;
        }
        boolean down = mc.currentScreen == null && KeyInput.isDown(key.get());
        boolean pressed = down && !wasDown;
        wasDown = down;

        if (step == Step.IDLE) {
            if (!pressed || !begin(mc)) return;
        }
        // with a 0 ms delay the whole sequence runs inside this one tick
        for (int i = 0; i < 2 && step != Step.IDLE && delay.elapsed(); i++) advance(mc);
    }

    private boolean begin(MinecraftClient mc) {
        int slot = slot(pearlSlot);
        if (!mc.player.getInventory().getStack(slot).isOf(Items.ENDER_PEARL)) return false;
        PlayerActions.selectSlot(mc.player, slot);
        step = Step.THROW;
        delay.restart();
        return true;
    }

    private void advance(MinecraftClient mc) {
        switch (step) {
            case THROW -> {
                PlayerActions.use(mc, Hand.MAIN_HAND);
                step = Step.RETURN;
                delay.restart();
            }
            case RETURN -> {
                PlayerActions.selectSlot(mc.player, slot(totemSlot));
                step = Step.IDLE;
            }
            default -> step = Step.IDLE;
        }
    }

    private static int slot(NumberSetting s) { return s.get().intValue() - 1; }
}
