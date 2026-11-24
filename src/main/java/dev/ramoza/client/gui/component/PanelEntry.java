package dev.ramoza.client.gui.component;

import dev.ramoza.client.gui.FocusManager;
import dev.ramoza.client.gui.UiContext;
import dev.ramoza.client.gui.anim.SmoothFloat;

/** A row-like block inside a {@link CategoryPanel}. Handles smooth show/hide for search filtering. */
public abstract class PanelEntry extends Component {
    protected final SmoothFloat match = new SmoothFloat(1f, 14f);
    private boolean matchTarget = true;

    public void setMatch(boolean m) {
        matchTarget = m;
        match.setTarget(m ? 1f : 0f);
    }

    public boolean matchTarget() { return matchTarget; }

    public float matchValue() { return match.get(); }

    public void tickMatch(float dt) { match.update(dt); }

    public abstract boolean matches(String query);

    /** Full height when completely visible (animated internally, e.g. while expanding). */
    public abstract float contentHeight(UiContext ctx);

    public float visibleHeight(UiContext ctx) {
        float mt = match.get();
        return mt < 0.01f ? 0f : contentHeight(ctx) * mt;
    }

    /**
     * Lays the entry out at (x, y) and, if {@code draw} is true, renders it. When false only
     * the bounds are updated (so keyboard navigation can still scroll to off-screen entries).
     */
    public abstract void renderEntry(UiContext ctx, float x, float y, float w, boolean draw);

    public abstract void collectFocus(FocusManager fm, CategoryPanel panel);
}
