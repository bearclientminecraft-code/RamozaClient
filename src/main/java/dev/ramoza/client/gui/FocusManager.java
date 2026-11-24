package dev.ramoza.client.gui;

import dev.ramoza.client.gui.component.CategoryPanel;
import dev.ramoza.client.gui.component.KeybindButton;
import dev.ramoza.client.util.MathUtil;

import java.util.ArrayList;
import java.util.List;

/** Keeps the flat, visible focus order (rebuilt every frame) and the current focus. */
public final class FocusManager {
    private final List<Focusable> order = new ArrayList<>();
    private final List<CategoryPanel> owners = new ArrayList<>();
    private Focusable current;
    private KeybindButton capturing;

    public void begin() {
        order.clear();
        owners.clear();
    }

    public void add(Focusable f, CategoryPanel owner) {
        order.add(f);
        owners.add(owner);
    }

    public void end() {
        if (current != null && !order.contains(current)) set(null);
    }

    public void set(Focusable f) {
        if (current == f) return;
        if (current != null) current.setFocused(false);
        current = f;
        if (f != null) f.setFocused(true);
        if (capturing != f) capturing = null;
    }

    public Focusable current() { return current; }

    public CategoryPanel ownerOf(Focusable f) {
        int i = order.indexOf(f);
        return i < 0 ? null : owners.get(i);
    }

    /** Previous / next element. Stops at the ends. */
    public Focusable move(int dir) {
        if (order.isEmpty()) return null;
        int i = order.indexOf(current);
        int n = i < 0 ? (dir > 0 ? 0 : order.size() - 1) : MathUtil.clamp(i + dir, 0, order.size() - 1);
        set(order.get(n));
        return current;
    }

    /** Moves focus to the same row of the neighbouring panel (clamped to its length). */
    public Focusable jumpPanel(int dir) {
        CategoryPanel cur = ownerOf(current);
        if (cur == null) return null;
        List<CategoryPanel> panels = new ArrayList<>();
        for (CategoryPanel o : owners) if (o != null && !panels.contains(o)) panels.add(o);
        int ni = panels.indexOf(cur) + dir;
        if (ni < 0 || ni >= panels.size()) return null;
        CategoryPanel target = panels.get(ni);

        int curFirst = -1;
        int rel = 0;
        int cursor = order.indexOf(current);
        for (int i = 0; i < order.size(); i++) {
            if (owners.get(i) == cur) {
                if (curFirst < 0) curFirst = i;
                if (i == cursor) rel = i - curFirst;
            }
        }
        int first = -1;
        int count = 0;
        for (int i = 0; i < order.size(); i++) {
            if (owners.get(i) == target) {
                if (first < 0) first = i;
                count++;
            }
        }
        if (first < 0) return null;
        set(order.get(first + Math.min(rel, count - 1)));
        return current;
    }

    public void focusFirst() {
        if (!order.isEmpty()) set(order.get(0));
    }

    public KeybindButton capturing() { return capturing; }
    public void beginCapture(KeybindButton b) { capturing = b; }
    public void endCapture() { capturing = null; }
}
