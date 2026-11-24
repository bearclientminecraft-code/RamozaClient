package dev.ramoza.client.setting;

import java.util.Arrays;
import java.util.List;

public final class ModeSetting extends Setting<String> {
    private final List<String> options;

    public ModeSetting(String name, String description, String def, String... options) {
        super(name, description, def);
        this.options = Arrays.asList(options);
        if (!this.options.contains(def)) this.value = this.options.get(0);
    }

    public List<String> options() { return options; }
    public int index() { return Math.max(0, options.indexOf(value)); }
    public void setIndex(int i) { value = options.get(Math.floorMod(i, options.size())); }

    @Override public String serialize() { return value; }

    @Override public void deserialize(String s) {
        if (options.contains(s)) value = s;
    }
}
