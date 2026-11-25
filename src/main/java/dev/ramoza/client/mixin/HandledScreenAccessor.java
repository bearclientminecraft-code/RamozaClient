package dev.ramoza.client.mixin;

import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.screen.slot.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(HandledScreen.class)
public interface HandledScreenAccessor {
    /** Slot under the mouse cursor, or null (vanilla updates it every frame while the screen is drawn). */
    @Accessor("focusedSlot")
    Slot ramoza$getFocusedSlot();
}
