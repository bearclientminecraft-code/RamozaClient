package dev.ramoza.client.gui;

/** Something the keyboard focus can land on. */
public interface Focusable {
    float fx();
    float fy();
    float fw();
    float fh();

    void setFocused(boolean focused);

    boolean isFocused();

    /** Enter / Space. */
    default boolean onEnter(UiContext ctx) { return false; }

    /** Left (-1) / Right (+1). Return false to let the GUI jump to the neighbouring panel. */
    default boolean onHorizontal(UiContext ctx, int dir) { return false; }
}
