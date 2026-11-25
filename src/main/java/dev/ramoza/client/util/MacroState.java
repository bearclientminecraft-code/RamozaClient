package dev.ramoza.client.util;

/** Shared flag: true while a macro fires a vanilla attack itself, so attack hooks do not react to it. */
public final class MacroState {
    public static boolean syntheticAttack;

    private MacroState() {}
}
