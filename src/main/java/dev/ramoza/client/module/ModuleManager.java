package dev.ramoza.client.module;

import dev.ramoza.client.module.combat.AutoMace;
import dev.ramoza.client.module.combat.LungeSwap;
import dev.ramoza.client.module.combat.MaceSwap;
import dev.ramoza.client.module.combat.StunSlam;
import dev.ramoza.client.module.player.AutoRespawn;
import dev.ramoza.client.module.player.ElytraSwap;
import dev.ramoza.client.module.player.FastPlace;
import dev.ramoza.client.module.player.FreeCam;
import dev.ramoza.client.module.player.KeyPearl;
import dev.ramoza.client.module.player.NoJumpDelay;
import dev.ramoza.client.module.player.NoPush;
import dev.ramoza.client.module.player.PearlCatch;
import dev.ramoza.client.module.player.Sprint;
import dev.ramoza.client.module.player.XpMacro;
import dev.ramoza.client.setting.BoolSetting;
import dev.ramoza.client.setting.KeybindSetting;
import dev.ramoza.client.setting.ModeSetting;
import dev.ramoza.client.setting.NumberSetting;
import dev.ramoza.client.util.Keys;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public final class ModuleManager {
    private static final Comparator<ClientModule> BY_NAME = (a, b) -> naturalCompare(a.name(), b.name());

    private final List<ClientModule> modules = new ArrayList<>();

    public void register(ClientModule module) { modules.add(module); }

    public List<ClientModule> all() { return Collections.unmodifiableList(modules); }

    /** Modules of one category, sorted alphabetically (case-insensitive, numbers in natural order). */
    public List<ClientModule> in(Category category) {
        List<ClientModule> out = new ArrayList<>();
        for (ClientModule m : modules) if (m.category() == category) out.add(m);
        out.sort(BY_NAME);
        return out;
    }

    /** First registered module of the given type, or null. */
    public <T extends ClientModule> T get(Class<T> type) {
        for (ClientModule m : modules) if (type.isInstance(m)) return type.cast(m);
        return null;
    }

    /** Ticks every enabled module. Called from RamozaClientMod on START_CLIENT_TICK. */
    public void tick() {
        for (ClientModule m : modules) if (m.isEnabled()) m.onTick();
    }

    /**
     * Real modules plus GUI-only placeholders for the categories that do not have modules yet
     * (so every control type and the scrolling can be seen). Replace placeholders as modules appear.
     */
    public void registerDefaults() {
        placeholders(Category.COMBAT, 12);
        placeholders(Category.RENDER, 5);
        placeholders(Category.MISC, 5);
        placeholders(Category.POT_CHEATS, 4);

        register(new AutoMace());
        register(new LungeSwap());
        register(new MaceSwap());
        register(new StunSlam());
        register(new AutoRespawn());
        register(new ElytraSwap());
        register(new FastPlace());
        register(new FreeCam());
        register(new KeyPearl());
        register(new NoJumpDelay());
        register(new NoPush());
        register(new PearlCatch());
        register(new Sprint());
        register(new XpMacro());
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

    /** Case-insensitive comparison where digit runs compare by value ("Example 2" < "Example 10"). */
    static int naturalCompare(String a, String b) {
        int i = 0, j = 0;
        while (i < a.length() && j < b.length()) {
            char ca = a.charAt(i), cb = b.charAt(j);
            if (Character.isDigit(ca) && Character.isDigit(cb)) {
                int si = i, sj = j;
                while (i < a.length() && Character.isDigit(a.charAt(i))) i++;
                while (j < b.length() && Character.isDigit(b.charAt(j))) j++;
                String na = a.substring(si, i).replaceFirst("^0+(?=.)", "");
                String nb = b.substring(sj, j).replaceFirst("^0+(?=.)", "");
                if (na.length() != nb.length()) return Integer.compare(na.length(), nb.length());
                int c = na.compareTo(nb);
                if (c != 0) return c;
            } else {
                char la = Character.toLowerCase(ca), lb = Character.toLowerCase(cb);
                if (la != lb) return Character.compare(la, lb);
                i++;
                j++;
            }
        }
        int rest = Integer.compare(a.length() - i, b.length() - j);
        return rest != 0 ? rest : a.compareTo(b);
    }
}
