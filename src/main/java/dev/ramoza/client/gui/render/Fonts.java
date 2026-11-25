package dev.ramoza.client.gui.render;

import dev.ramoza.client.RamozaClientMod;
import net.minecraft.text.StyleSpriteSource;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

/**
 * The ONLY place that touches the Style font API. Font definitions live in
 * assets/ramozaclient/font/{inter,blacksword}.json.
 */
public final class Fonts {
    private static final Style INTER = Style.EMPTY.withFont(
            new StyleSpriteSource.Font(Identifier.of(RamozaClientMod.MOD_ID, "inter")));
    private static final Style WATERMARK = Style.EMPTY.withFont(
            new StyleSpriteSource.Font(Identifier.of(RamozaClientMod.MOD_ID, "blacksword")));

    private Fonts() {}

    public static Text text(String s, UiFont f) {
        return Text.literal(s).setStyle(f == UiFont.WATERMARK ? WATERMARK : INTER);
    }
}
