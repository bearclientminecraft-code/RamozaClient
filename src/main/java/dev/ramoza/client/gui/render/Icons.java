package dev.ramoza.client.gui.render;

import dev.ramoza.client.module.Category;

/** Tiny pixel-art icons ('X' = filled). Drawn with plain rectangles, no textures needed. */
public final class Icons {
    private Icons() {}

    public static final String[] SHIELD = {
            "XXXXXXXXX",
            "X...X...X",
            "X...X...X",
            "XXXXXXXXX",
            "X...X...X",
            ".X..X..X.",
            "..X.X.X..",
            "...XXX...",
            "....X...."
    };

    public static final String[] EYE = {
            ".........",
            "...XXX...",
            ".XX...XX.",
            "X..XXX..X",
            "X.XXXXX.X",
            "X..XXX..X",
            ".XX...XX.",
            "...XXX...",
            "........."
    };

    public static final String[] GRID = {
            "XXXX.XXXX",
            "XXXX.XXXX",
            "XXXX.XXXX",
            "XXXX.XXXX",
            ".........",
            "XXXX.XXXX",
            "XXXX.XXXX",
            "XXXX.XXXX",
            "XXXX.XXXX"
    };

    public static final String[] CHEST = {
            ".XXXXXXX.",
            "XXXXXXXXX",
            "X.......X",
            "XXXXXXXXX",
            "X...X...X",
            "X...X...X",
            "X.......X",
            "XXXXXXXXX",
            "........."
    };

    public static final String[] PERSON = {
            "...XXX...",
            "..XXXXX..",
            "..XXXXX..",
            "...XXX...",
            ".........",
            ".XXXXXXX.",
            "XXXXXXXXX",
            "XXXXXXXXX",
            "XXXXXXXXX"
    };

    public static final String[] SEARCH = {
            ".XXXX....",
            "X....X...",
            "X....X...",
            "X....X...",
            "X....X...",
            ".XXXXX...",
            "......X..",
            ".......X.",
            "........X"
    };

    public static final String[] GEAR = {
            "....X....",
            ".X.XXX.X.",
            "..XXXXX..",
            ".XXX.XXX.",
            "XXXX.XXXX",
            ".XXX.XXX.",
            "..XXXXX..",
            ".X.XXX.X.",
            "....X...."
    };

    public static final String[] CONTRAST = {
            "..XXXXX..",
            ".XXXX..X.",
            "XXXXX...X",
            "XXXXX...X",
            "XXXXX...X",
            "XXXXX...X",
            "XXXXX...X",
            ".XXXX..X.",
            "..XXXXX.."
    };

    public static final String[] POWER = {
            "....X....",
            "....X....",
            ".X..X..X.",
            "X...X...X",
            "X...X...X",
            "X.......X",
            ".X.....X.",
            "..XXXXX..",
            "........."
    };

    public static final String[] PALETTE = {
            "..XXXXX..",
            ".X.....X.",
            "X..X.X..X",
            "X.......X",
            "X.XX....X",
            "X.XX..XX.",
            ".X...XX..",
            "..XXXX...",
            "........."
    };

    public static final String[] CLOSE = {
            "X...X",
            ".X.X.",
            "..X..",
            ".X.X.",
            "X...X"
    };

    public static final String[] CHECK = {
            "....X",
            "...X.",
            "X.X..",
            ".X..."
    };

    public static final String[] CHEVRON_RIGHT = {
            "X..",
            ".X.",
            "..X",
            ".X.",
            "X.."
    };

    public static final String[] CHEVRON_DOWN = {
            "X...X",
            ".X.X.",
            "..X.."
    };

    public static final String[] TRIANGLE_DOWN = {
            "XXXXX",
            ".XXX.",
            "..X.."
    };

    public static final String[] TRIANGLE_RIGHT = {
            "X..",
            "XX.",
            "XXX",
            "XX.",
            "X.."
    };

    public static String[] forCategory(Category c) {
        switch (c) {
            case COMBAT: return SHIELD;
            case RENDER: return EYE;
            case MISC: return GRID;
            case POT_CHEATS: return CHEST;
            default: return PERSON;
        }
    }

    /** File name (without .png) of the category texture in textures/gui/category/. */
    public static String textureName(Category c) {
        switch (c) {
            case COMBAT: return "combat";
            case RENDER: return "render";
            case MISC: return "misc";
            case POT_CHEATS: return "pot_cheats";
            default: return "player";
        }
    }

    public static int width(String[] icon) { return icon[0].length(); }
    public static int height(String[] icon) { return icon.length; }
}
