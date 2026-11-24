package dev.ramoza.client.module;

import dev.ramoza.client.setting.Setting;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A feature entry shown in the ClickGUI. This base only stores state (enabled flag + settings);
 * override {@link #onEnable()} / {@link #onDisable()} in your own subclasses.
 */
public class ClientModule {
    private final String name;
    private final String description;
    private final Category category;
    private final List<Setting<?>> settings = new ArrayList<>();
    private boolean enabled;

    public ClientModule(String name, String description, Category category) {
        this.name = name;
        this.description = description;
        this.category = category;
    }

    public <S extends Setting<?>> S add(S setting) {
        settings.add(setting);
        return setting;
    }

    public String name() { return name; }
    public String description() { return description; }
    public Category category() { return category; }
    public List<Setting<?>> settings() { return Collections.unmodifiableList(settings); }
    public boolean isEnabled() { return enabled; }

    public void setEnabled(boolean value) {
        if (value == enabled) return;
        enabled = value;
        if (enabled) onEnable(); else onDisable();
    }

    public void toggle() { setEnabled(!enabled); }

    protected void onEnable() {}
    protected void onDisable() {}
}
