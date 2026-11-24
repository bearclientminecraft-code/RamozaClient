package dev.ramoza.client.setting;

import dev.ramoza.client.util.Keys;

public final class KeybindSetting extends Setting<Integer> {
    public KeybindSetting(String name, String description, int defaultKey) {
        super(name, description, defaultKey);
    }

    public boolean isBound() { return value != Keys.NONE; }

    @Override public String serialize() { return Integer.toString(value); }

    @Override public void deserialize(String s) {
        try {
            value = Integer.parseInt(s);
        } catch (NumberFormatException ignored) {
            // keep current value
        }
    }
}
