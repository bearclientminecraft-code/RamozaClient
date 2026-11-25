package dev.ramoza.client.module.player;

import dev.ramoza.client.RamozaClientMod;
import dev.ramoza.client.core.RamozaCore;
import dev.ramoza.client.module.Category;
import dev.ramoza.client.module.ClientModule;
import dev.ramoza.client.setting.BoolSetting;

/**
 * Stops the local player from being pushed. The actual work is done by the mixins
 * ({@code EntityMixin}, {@code PlayerEntityMixin}); this class only holds state and settings.
 */
public final class NoPush extends ClientModule {
    private final BoolSetting entities = add(new BoolSetting("Entities",
            "Ignore pushing from other entities (players, mobs, boats...)", true));
    private final BoolSetting liquids = add(new BoolSetting("Liquids",
            "Ignore push from flowing water and lava", true));

    public NoPush() {
        super("No Push", "You are not pushed by entities or liquids", Category.PLAYER);
    }

    public boolean blocksEntityPush() { return entities.get(); }

    public boolean blocksLiquidPush() { return liquids.get(); }

    /** The enabled instance, or null (module off / client not ready). Used by mixins. */
    public static NoPush active() {
        RamozaCore core = RamozaClientMod.core();
        if (core == null) return null;
        NoPush m = core.modules.get(NoPush.class);
        return m != null && m.isEnabled() ? m : null;
    }
}
