package dev.ramoza.client.module;

import dev.ramoza.client.module.combat.AutoDoubleHand;
import dev.ramoza.client.module.combat.AutoInventoryTotem;
import dev.ramoza.client.module.combat.AutoMace;
import dev.ramoza.client.module.combat.HoverTotem;
import dev.ramoza.client.module.combat.LungeSwap;
import dev.ramoza.client.module.combat.MaceSwap;
import dev.ramoza.client.module.combat.StunSlam;
import dev.ramoza.client.module.combat.TotemOffhand;
import dev.ramoza.client.module.combat.Trigger;
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
     * Registers every real module. A category with no modules is simply not drawn by the GUI,
     * so adding a module to an empty category makes its panel appear automatically.
     */
    public void registerDefaults() {
        register(new AutoDoubleHand());
        register(new AutoInventoryTotem());
        register(new AutoMace());
        register(new HoverTotem());
        register(new LungeSwap());
        register(new MaceSwap());
        register(new StunSlam());
        register(new TotemOffhand());
        register(new Trigger());
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

    /** Case-insensitive comparison where digit runs compare by value ("Mace 2" < "Mace 10"). */
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
