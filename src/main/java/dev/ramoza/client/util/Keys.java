package dev.ramoza.client.util;

/** GLFW key codes used by the GUI layer (kept here so the UI code has no LWJGL dependency). */
public final class Keys {
    public static final int NONE = -1;
    public static final int SPACE = 32;
    public static final int ESCAPE = 256;
    public static final int ENTER = 257;
    public static final int TAB = 258;
    public static final int BACKSPACE = 259;
    public static final int INSERT = 260;
    public static final int DELETE = 261;
    public static final int RIGHT = 262;
    public static final int LEFT = 263;
    public static final int DOWN = 264;
    public static final int UP = 265;
    public static final int PAGE_UP = 266;
    public static final int PAGE_DOWN = 267;
    public static final int HOME = 268;
    public static final int END = 269;
    public static final int KP_ENTER = 335;
    public static final int LEFT_SHIFT = 340;
    public static final int LEFT_CONTROL = 341;
    public static final int LEFT_ALT = 342;
    public static final int RIGHT_SHIFT = 344;
    public static final int RIGHT_CONTROL = 345;
    public static final int RIGHT_ALT = 346;
    public static final int F = 70;

    /** Mouse buttons are stored as MOUSE_BASE + GLFW button (0 = left, 1 = right, 2 = middle, 3+ = side). */
    public static final int MOUSE_BASE = 1000;
    public static final int MOUSE_LAST = MOUSE_BASE + 7;

    private Keys() {}

    public static boolean isMouse(int code) { return code >= MOUSE_BASE && code <= MOUSE_LAST; }

    public static int mouse(int button) { return MOUSE_BASE + button; }

    public static String nameOf(int key) {
        if (key == NONE) return "None";
        if (isMouse(key)) {
            switch (key - MOUSE_BASE) {
                case 0: return "LMB";
                case 1: return "RMB";
                case 2: return "MMB";
                default: return "Mouse " + (key - MOUSE_BASE + 1);
            }
        }
        if (key >= 48 && key <= 57) return String.valueOf((char) key);
        if (key >= 65 && key <= 90) return String.valueOf((char) key);
        if (key >= 290 && key <= 301) return "F" + (key - 289);
        switch (key) {
            case SPACE: return "Space";
            case ESCAPE: return "Esc";
            case ENTER: return "Enter";
            case KP_ENTER: return "Enter";
            case TAB: return "Tab";
            case BACKSPACE: return "Backspace";
            case INSERT: return "Insert";
            case DELETE: return "Delete";
            case RIGHT: return "Right";
            case LEFT: return "Left";
            case DOWN: return "Down";
            case UP: return "Up";
            case PAGE_UP: return "PgUp";
            case PAGE_DOWN: return "PgDn";
            case HOME: return "Home";
            case END: return "End";
            case LEFT_SHIFT: return "LShift";
            case RIGHT_SHIFT: return "RShift";
            case LEFT_CONTROL: return "LCtrl";
            case RIGHT_CONTROL: return "RCtrl";
            case LEFT_ALT: return "LAlt";
            case RIGHT_ALT: return "RAlt";
            default: return "Key " + key;
        }
    }
}
