package dev.ramoza.client.core;

import dev.ramoza.client.config.ConfigManager;
import dev.ramoza.client.config.GuiSettings;
import dev.ramoza.client.gui.theme.ThemeManager;
import dev.ramoza.client.module.ModuleManager;

import java.nio.file.Path;

/** Shared state of the client: settings, themes, modules, config. */
public final class RamozaCore {
    public static final String NAME = "Ramoza";
    public static final String VERSION = "1.0.0";

    public final GuiSettings settings = new GuiSettings();
    public final ThemeManager themes = new ThemeManager(settings);
    public final ModuleManager modules = new ModuleManager();
    private final ConfigManager config;

    /** "Self Destruct" (GUI prototype): while true the GUI refuses to open. Not persisted. */
    private boolean guiHidden;

    public RamozaCore(Path configDir) {
        this.config = new ConfigManager(configDir);
        modules.registerDefaults();
        config.load(this);
        themes.snap();
    }

    public boolean isGuiHidden() { return guiHidden; }
    public void setGuiHidden(boolean hidden) { this.guiHidden = hidden; }

    public void save() { config.save(this); }
}
