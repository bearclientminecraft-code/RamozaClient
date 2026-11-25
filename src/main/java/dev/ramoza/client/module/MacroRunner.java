package dev.ramoza.client.module;

/**
 * Tick-driven task scheduler for a macro: runs a fixed list of steps one after another, waiting
 * {@code Delay +/- Random Delay} (a fresh roll after every step) between them. Never blocks the game thread.
 * <p>
 * With Humanize on, at most ONE step runs per client tick and at least one full tick separates two steps,
 * so a slot switch and the following attack can never land in the same tick.
 */
public final class MacroRunner {
    private final DelayTimer delay;
    private Runnable[] steps;
    private int index;
    private long tickCounter;
    private long lastStepTick = Long.MIN_VALUE / 2;

    public MacroRunner(DelayTimer delay) { this.delay = delay; }

    public boolean running() { return steps != null; }

    /** Must be called exactly once per client tick, before {@link #tick()}. */
    public void beginTick() { tickCounter++; }

    /** Starts a sequence and runs its first step right away. */
    public void start(Runnable[] sequence) {
        steps = sequence;
        index = 0;
        delay.reset();
        tick();
    }

    /** Starts a sequence that waits one delay before its first step (used when the first action already happened). */
    public void startDelayed(Runnable[] sequence) {
        steps = sequence;
        index = 0;
        lastStepTick = tickCounter; // the caller's own action (e.g. the swap) counts as this tick's step
        delay.restart();
    }

    /** Runs every step whose wait has passed. Called once per client tick and from hooks that need it sooner. */
    public void tick() {
        boolean human = delay.humanized();
        while (steps != null && delay.elapsed()) {
            if (human && tickCounter <= lastStepTick) return; // one step per tick, >= 1 tick between steps
            Runnable step = steps[index++];
            if (index >= steps.length) steps = null; // clear first: a step may start a new sequence
            step.run();
            lastStepTick = tickCounter;
            delay.restart();
        }
    }

    /** Runs all remaining steps immediately, ignoring delays (used when the module is turned off mid-sequence). */
    public void flush() {
        while (steps != null) {
            Runnable step = steps[index++];
            if (index >= steps.length) steps = null;
            step.run();
        }
    }

    /** Drops the sequence without running it (player left the world). */
    public void abort() { steps = null; }
}
