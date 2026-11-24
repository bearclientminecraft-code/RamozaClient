package dev.ramoza.client.module;

import dev.ramoza.client.setting.BoolSetting;
import dev.ramoza.client.setting.KeybindSetting;
import dev.ramoza.client.setting.ModeSetting;
import dev.ramoza.client.setting.NumberSetting;
import dev.ramoza.client.util.Keys;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class ModuleManager {
    private final List<ClientModule> modules = new ArrayList<>();

    public void register(ClientModule module) { modules.add(module); }

    public List<ClientModule> all() { return Collections.unmodifiableList(modules); }

    public List<ClientModule> in(Category category) {
        List<ClientModule> out = new ArrayList<>();
        for (ClientModule m : modules) if (m.category() == category) out.add(m);
        return out;
    }

    /**
     * GUI-only placeholders so every control type and the scrolling can be seen immediately.
     * Replace this with your real modules (see README).
     */
    public void registerDefaults() {
        placeholders(Category.COMBAT, 12);
        placeholders(Category.RENDER, 5);
        placeholders(Category.MISC, 5);
        placeholders(Category.POT_CHESTS, 4);
        placeholders(Category.PLAYER, 6);
    }

    private void placeholders(Category category, int count) {
        for (int i = 1; i <= count; i++) {
            ClientModule m = new ClientModule("Example " + i,
                    "Placeholder entry. Replace with a real module.", category);
            if (i == 1) {
                m.add(new BoolSetting("Option", "Example toggle", true));
                m.add(new NumberSetting("Amount", "Example slider", 5, 0, 10, 0.5));
                m.add(new ModeSetting("Mode", "Example dropdown", "Alpha", "Alpha", "Beta", "Gamma"));
                m.add(new KeybindSetting("Bind", "Example keybind", Keys.NONE));
            } else if (i == 2) {
                m.add(new BoolSetting("Option", "Example toggle", false));
                m.add(new NumberSetting("Range", "Example slider", 3, 1, 6, 1));
            }
            register(m);
        }
    }
}
