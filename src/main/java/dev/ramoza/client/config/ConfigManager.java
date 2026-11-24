package dev.ramoza.client.config;

import dev.ramoza.client.core.RamozaCore;
import dev.ramoza.client.gui.theme.Appearance;
import dev.ramoza.client.module.Category;
import dev.ramoza.client.module.ClientModule;
import dev.ramoza.client.setting.Setting;
import dev.ramoza.client.util.MathUtil;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/** Stores GUI preferences, theme and module state in config/ramozaclient.properties. */
public final class ConfigManager {
    private final Path file;

    public ConfigManager(Path configDir) {
        this.file = configDir.resolve("ramozaclient.properties");
    }

    public void load(RamozaCore core) {
        if (!Files.isRegularFile(file)) return;
        Properties p = new Properties();
        try (Reader in = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            p.load(in);
        } catch (IOException e) {
            return;
        }
        GuiSettings s = core.settings;
        s.scale = MathUtil.clamp(f(p, "gui.scale", s.scale), GuiSettings.SCALE_MIN, GuiSettings.SCALE_MAX);
        s.opacity = MathUtil.clamp(f(p, "gui.opacity", s.opacity), GuiSettings.OPACITY_MIN, GuiSettings.OPACITY_MAX);
        s.animationSpeed = MathUtil.clamp(f(p, "gui.animationSpeed", s.animationSpeed), GuiSettings.SPEED_MIN, GuiSettings.SPEED_MAX);
        s.roundedCorners = MathUtil.clamp(f(p, "gui.roundedCorners", s.roundedCorners), GuiSettings.CORNERS_MIN, GuiSettings.CORNERS_MAX);
        s.blur = Boolean.parseBoolean(p.getProperty("gui.blur", Boolean.toString(s.blur)));
        s.showTooltips = Boolean.parseBoolean(p.getProperty("gui.showTooltips", Boolean.toString(s.showTooltips)));
        s.compactMode = Boolean.parseBoolean(p.getProperty("gui.compactMode", Boolean.toString(s.compactMode)));

        core.themes.setTheme((int) f(p, "gui.theme", core.themes.themeIndex()));
        try {
            core.themes.setAppearance(Appearance.valueOf(p.getProperty("gui.appearance", core.themes.appearance().name())));
        } catch (IllegalArgumentException ignored) {
            // unknown value, keep default
        }

        for (Category c : Category.values()) {
            String v = p.getProperty("panel." + c.name() + ".collapsed");
            if (v != null) s.collapsed.put(c, Boolean.parseBoolean(v));
        }

        for (ClientModule m : core.modules.all()) {
            String key = moduleKey(m);
            String en = p.getProperty(key + ".enabled");
            if (en != null) m.setEnabled(Boolean.parseBoolean(en));
            for (Setting<?> set : m.settings()) {
                String v = p.getProperty(key + ".setting." + set.name());
                if (v != null) set.deserialize(v);
            }
        }
    }

    public void save(RamozaCore core) {
        Properties p = new Properties();
        GuiSettings s = core.settings;
        p.setProperty("gui.scale", Float.toString(s.scale));
        p.setProperty("gui.opacity", Float.toString(s.opacity));
        p.setProperty("gui.animationSpeed", Float.toString(s.animationSpeed));
        p.setProperty("gui.roundedCorners", Float.toString(s.roundedCorners));
        p.setProperty("gui.blur", Boolean.toString(s.blur));
        p.setProperty("gui.showTooltips", Boolean.toString(s.showTooltips));
        p.setProperty("gui.compactMode", Boolean.toString(s.compactMode));
        p.setProperty("gui.theme", Integer.toString(core.themes.themeIndex()));
        p.setProperty("gui.appearance", core.themes.appearance().name());

        for (Category c : Category.values()) {
            p.setProperty("panel." + c.name() + ".collapsed", Boolean.toString(s.collapsed.getOrDefault(c, false)));
        }
        for (ClientModule m : core.modules.all()) {
            String key = moduleKey(m);
            p.setProperty(key + ".enabled", Boolean.toString(m.isEnabled()));
            for (Setting<?> set : m.settings()) {
                p.setProperty(key + ".setting." + set.name(), set.serialize());
            }
        }

        try {
            Files.createDirectories(file.getParent());
            try (Writer out = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
                p.store(out, "Ramoza Client");
            }
        } catch (IOException ignored) {
            // config is best-effort
        }
    }

    private static String moduleKey(ClientModule m) {
        return "module." + m.category().name() + "." + m.name().replace(' ', '_');
    }

    private static float f(Properties p, String key, float def) {
        String v = p.getProperty(key);
        if (v == null) return def;
        try {
            return Float.parseFloat(v);
        } catch (NumberFormatException e) {
            return def;
        }
    }
}
