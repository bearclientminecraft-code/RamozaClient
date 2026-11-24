package dev.ramoza.client.gui.component;

import dev.ramoza.client.gui.FocusManager;
import dev.ramoza.client.gui.Metrics;
import dev.ramoza.client.gui.UiContext;
import dev.ramoza.client.gui.render.Gfx;
import dev.ramoza.client.gui.render.Icons;
import dev.ramoza.client.gui.theme.Palette;

import java.util.function.IntConsumer;

/** Bottom-right bar: Theme / Appearance, GUI Settings, Self Destruct (GUI hide). */
public final class BottomBar extends Component {
    private static final float MARGIN = 8f;
    private static final float INNER = 3f;
    private static final float BTN_H = 18f;
    private static final float GAP = 2f;
    private static final float CONFIRM_TIME = 3f;

    private final PillButton appearance;
    private final PillButton settings;
    private final PillButton destruct;
    private final Runnable onSelfDestruct;
    private float confirmLeft;

    public BottomBar(Runnable toggleAppearance, IntConsumer cycleAppearance,
                     Runnable toggleSettings, Runnable onSelfDestruct) {
        this.onSelfDestruct = onSelfDestruct;
        appearance = new PillButton(Icons.CONTRAST, "Appearance", "Switch Dark / Light / Liquid Glass", toggleAppearance);
        appearance.setOnArrow(cycleAppearance);
        settings = new PillButton(Icons.GEAR, "GUI Settings", "Scale, opacity, animations, blur...", toggleSettings);
        destruct = new PillButton(Icons.POWER, "Self Destruct", "Self Destruct", "Hide the GUI until it is restored (see README)", this::onDestructClicked);
        destruct.setDanger(true);
    }

    private void onDestructClicked() {
        if (confirmLeft > 0f) {
            confirmLeft = 0f;
            destruct.setText("Self Destruct");
            onSelfDestruct.run();
        } else {
            confirmLeft = CONFIRM_TIME;
            destruct.setText("Confirm?");
        }
    }

    public void setAppearanceOpen(boolean open) { appearance.setHighlighted(open); }
    public void setSettingsOpen(boolean open) { settings.setHighlighted(open); }

    public void collectFocus(FocusManager fm) {
        fm.add(appearance, null);
        fm.add(settings, null);
        fm.add(destruct, null);
    }

    public float appearanceRight() { return appearance.x + appearance.w; }
    public float top() { return y; }
    public float right() { return x + w; }

    public void render(UiContext ctx, float vw, float vh) {
        Gfx g = ctx.g;
        Palette p = ctx.p;
        Metrics m = ctx.m;

        if (confirmLeft > 0f) {
            confirmLeft -= ctx.dt;
            if (confirmLeft <= 0f) {
                confirmLeft = 0f;
                destruct.setText("Self Destruct");
            }
        }

        float w1 = appearance.preferredWidth(g, m);
        float w2 = settings.preferredWidth(g, m);
        float w3 = destruct.preferredWidth(g, m);
        float total = INNER * 2f + w1 + w2 + w3 + GAP * 2f;
        float height = BTN_H + INNER * 2f;
        float bx = vw - MARGIN - total;
        float by = vh - MARGIN - height;
        setBounds(bx, by, total, height);
        float r = Math.min(height / 2f, m.radius + 3f);

        g.shadow(bx, by, total, height, r, 4, p.shadow());
        g.roundRect(bx, by, total, height, r, p.panel());
        g.roundRectV(bx, by, total, height, r, p.gloss(), 0);
        g.roundBorder(bx, by, total, height, r, 1, p.border());

        float cx = bx + INNER;
        appearance.setBounds(cx, by + INNER, w1, BTN_H);
        cx += w1 + GAP;
        settings.setBounds(cx, by + INNER, w2, BTN_H);
        cx += w2 + GAP;
        destruct.setBounds(cx, by + INNER, w3, BTN_H);

        appearance.render(ctx);
        settings.render(ctx);
        destruct.render(ctx);
    }

    @Override
    public boolean mouseClicked(UiContext ctx, double mx, double my, int button) {
        if (appearance.mouseClicked(ctx, mx, my, button)) return true;
        if (settings.mouseClicked(ctx, mx, my, button)) return true;
        if (destruct.mouseClicked(ctx, mx, my, button)) return true;
        return hit(mx, my);
    }
}
