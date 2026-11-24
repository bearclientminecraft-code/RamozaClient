package dev.ramoza.client.setting;

public abstract class Setting<T> {
    private final String name;
    private final String description;
    protected T value;

    protected Setting(String name, String description, T defaultValue) {
        this.name = name;
        this.description = description;
        this.value = defaultValue;
    }

    public String name() { return name; }
    public String description() { return description; }
    public T get() { return value; }
    public void set(T v) { this.value = v; }

    public abstract String serialize();
    public abstract void deserialize(String s);
}
