package dev.ramoza.client.module.render;

import dev.ramoza.client.module.Category;
import dev.ramoza.client.module.ClientModule;
import dev.ramoza.client.setting.BoolSetting;
import dev.ramoza.client.setting.NumberSetting;

/**
 * HUD watermark: two rounded rows of small info pills. This class only holds the settings;
 * drawing and dragging live in {@link dev.ramoza.client.hud.WatermarkHud}. While the ClickGUI is open the
 * watermark can be dragged with the left mouse button, the position is saved in the config.
 */
public final class Watermark extends ClientModule {
    private final BoolSetting username = add(new BoolSetting("Username", "Your nickname", true));
    private final BoolSetting fps = add(new BoolSetting("FPS", "Frames per second", true));
    private final BoolSetting logo = add(new BoolSetting("Logo", "Client logo (letter R, Blacksword font)", true));
    private final BoolSetting ping = add(new BoolSetting("Ping", "Latency to the server in ms", true));
    private final BoolSetting time = add(new BoolSetting("Time", "Local time", true));
    private final BoolSetting coords = add(new BoolSetting("Coords", "Your block coordinates", true));
    private final BoolSetting tps = add(new BoolSetting("TPS", "Estimated server ticks per second", true));
    private final BoolSetting bps = add(new BoolSetting("BPS", "Your horizontal speed in blocks per second", true));
    private final BoolSetting server = add(new BoolSetting("Server", "Server address (or Singleplayer)", false));
    private final NumberSetting rounding = add(new NumberSetting("Corner Rounding",
            "0 = square corners, 100 = fully round ends", 70, 0, 100, 1));
    private final NumberSetting opacity = add(new NumberSetting("Background Opacity",
            "Opacity of the pill background (0-100)", 75, 0, 100, 1));

    public Watermark() {
        super("Watermark", "HUD info pills. Drag it with the mouse while the ClickGUI is open", Category.RENDER);
        setEnabled(true);
    }

    public boolean showUsername() { return username.get(); }
    public boolean showFps() { return fps.get(); }
    public boolean showLogo() { return logo.get(); }
    public boolean showPing() { return ping.get(); }
    public boolean showTime() { return time.get(); }
    public boolean showCoords() { return coords.get(); }
    public boolean showTps() { return tps.get(); }
    public boolean showBps() { return bps.get(); }
    public boolean showServer() { return server.get(); }
    /** 0..1 */
    public float roundness() { return (float) (rounding.get() / 100.0); }
    /** 0..1 */
    public float backgroundAlpha() { return (float) (opacity.get() / 100.0); }
}
