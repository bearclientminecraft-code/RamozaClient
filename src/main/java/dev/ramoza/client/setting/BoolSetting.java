package dev.ramoza.client.setting;

public final class BoolSetting extends Setting<Boolean> {
    public BoolSetting(String name, String description, boolean def) {
        super(name, description, def);
    }

    public void toggle() { value = !value; }

    @Override public String serialize() { return Boolean.toString(value); }
    @Override public void deserialize(String s) { value = Boolean.parseBoolean(s); }
}
