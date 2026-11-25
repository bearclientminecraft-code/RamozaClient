package dev.ramoza.client.gui.render;

import dev.ramoza.client.RamozaClientMod;
import net.minecraft.text.StyleSpriteSource;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.HashMap;
import java.util.Map;

/**
 * The ONLY place that touches the Style font API. Font definitions live in
 * assets/ramozaclient/font/{inter,blacksword}.json.
 */
public final class Fonts {
    /** true = vanilla Minecraft pixel font; false = bundled Inter (default). */
    private static final boolean VANILLA_FONT = false;
    private static final Style INTER = VANILLA_FONT ? Style.EMPTY : Style.EMPTY.withFont(
            new StyleSpriteSource.Font(Identifier.of(RamozaClientMod.MOD_ID, "inter")));
    private static final Style WATERMARK = Style.EMPTY.withFont(
            new StyleSpriteSource.Font(Identifier.of(RamozaClientMod.MOD_ID, "blacksword")));

    private static final int CACHE_LIMIT = 2048;
    private static final Map<String, Text> INTER_CACHE = new HashMap<>();
    private static final Map<String, Text> WATERMARK_CACHE = new HashMap<>();

    private Fonts() {}

    /** Text objects are cached: the GUI asks for the same few dozen strings every frame (render thread only). */
    public static Text text(String s, UiFont f) {
        boolean mark = f == UiFont.WATERMARK;
        Map<String, Text> cache = mark ? WATERMARK_CACHE : INTER_CACHE;
        Text t = cache.get(s);
        if (t == null) {
            if (cache.size() >= CACHE_LIMIT) cache.clear();
            t = Text.literal(s).setStyle(mark ? WATERMARK : INTER);
            cache.put(s, t);
        }
        return t;
    }
}
