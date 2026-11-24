package dev.ramoza.client.gui.component;

import dev.ramoza.client.setting.BoolSetting;
import dev.ramoza.client.setting.KeybindSetting;
import dev.ramoza.client.setting.ModeSetting;
import dev.ramoza.client.setting.NumberSetting;
import dev.ramoza.client.setting.Setting;

/** Maps a module {@link Setting} to the matching reusable control. */
public final class ControlFactory {
    private ControlFactory() {}

    public static Control create(Setting<?> s) {
        if (s instanceof BoolSetting) {
            BoolSetting b = (BoolSetting) s;
            return new Toggle(b.name(), b.description(), b::get, b::set);
        }
        if (s instanceof NumberSetting) {
            NumberSetting n = (NumberSetting) s;
            return new Slider(n.name(), n.description(), n::get, n::set, n.min(), n.max(), n.step(),
                    v -> n.format());
        }
        if (s instanceof ModeSetting) {
            ModeSetting m = (ModeSetting) s;
            return new Dropdown(m.name(), m.description(), m.options(), m::index, m::setIndex);
        }
        if (s instanceof KeybindSetting) {
            KeybindSetting k = (KeybindSetting) s;
            return new KeybindButton(k.name(), k.description(), k::get, k::set);
        }
        throw new IllegalArgumentException("Unsupported setting type: " + s.getClass());
    }
}
