package dev.ramoza.client.module;

import dev.ramoza.client.setting.BoolSetting;
import dev.ramoza.client.setting.NumberSetting;

import java.util.concurrent.ThreadLocalRandom;
import java.util.function.BooleanSupplier;
import java.util.function.DoubleSupplier;

/**
 * Millisecond delay with jitter. Every {@link #restart()} (= every new action) rolls a fresh target.
 * <p>
 * Plain mode: {@code base ± uniform(random)}, never below 0.
 * <p>
 * Humanize mode: {@code base + reaction (+ uniform jitter)}, where reaction is a right-skewed (log-normal)
 * sample with a slowly drifting, correlated bias plus rare "hesitation" outliers. Together with
 * {@link MacroRunner}'s one-step-per-tick rule this removes the two obvious signatures:
 * several actions in one tick, and a constant or uniformly distributed interval.
 * <p>
 * Modules are ticked 20 times per second, so a delay is checked once per client tick (~50 ms).
 */
public final class DelayTimer {
    public static final double MAX_MS = 200;

    /** Median of the human reaction component, ms. */
    private static final double REACTION_MEDIAN_MS = 30;
    /** Spread of the reaction component (log-space sigma). */
    private static final double REACTION_SIGMA = 0.45;
    /** Chance of a rare slow action (distraction / hesitation). */
    private static final double HESITATION_CHANCE = 0.05;

    private final DoubleSupplier baseMs;
    private final DoubleSupplier jitterMs;
    private final BooleanSupplier humanize;
    private boolean started;
    private long startNanos;
    private double targetMs;
    private double drift; // AR(1) state: consecutive intervals are correlated, not independent

    public DelayTimer(DoubleSupplier baseMs, DoubleSupplier jitterMs) {
        this(baseMs, jitterMs, () -> false);
    }

    public DelayTimer(DoubleSupplier baseMs, DoubleSupplier jitterMs, BooleanSupplier humanize) {
        this.baseMs = baseMs;
        this.jitterMs = jitterMs;
        this.humanize = humanize;
    }

    /** Adds "Delay (ms)", "Random Delay" and "Humanize" to the module and returns a timer bound to them. */
    public static DelayTimer attach(ClientModule module, double defaultMs) {
        NumberSetting delay = module.add(new NumberSetting("Delay (ms)",
                "Delay between actions in milliseconds (checked once per tick, about 50 ms)", defaultMs, 0, MAX_MS, 1));
        NumberSetting random = randomSetting(module);
        BoolSetting human = module.add(new BoolSetting("Humanize",
                "Human-like timing: one action per tick, skewed and correlated delays, rare slow actions", true));
        return new DelayTimer(delay::get, random::get, human::get);
    }

    /** Adds only the "Random Delay" slider (for modules that already have their own delay setting). */
    public static NumberSetting randomSetting(ClientModule module) {
        return module.add(new NumberSetting("Random Delay",
                "Each new action waits Delay +/- this many milliseconds (0 = off)", 0, 0, MAX_MS, 1));
    }

    public boolean humanized() { return humanize.getAsBoolean(); }

    /** True when no action has happened yet or the current target has passed. */
    public boolean elapsed() {
        return !started || (System.nanoTime() - startNanos) / 1_000_000.0 >= targetMs;
    }

    /** Marks an action as done: starts the wait for the next one with a freshly rolled target. */
    public void restart() {
        started = true;
        startNanos = System.nanoTime();
        targetMs = roll();
    }

    /** Forget the last action: {@link #elapsed()} is true again. */
    public void reset() { started = false; }

    private double roll() {
        double base = baseMs.getAsDouble();
        double jitter = jitterMs.getAsDouble();
        ThreadLocalRandom rng = ThreadLocalRandom.current();
        double t = base;
        if (jitter > 0) t += rng.nextDouble(-jitter, jitter);
        if (humanize.getAsBoolean()) {
            drift = 0.6 * drift + 0.4 * rng.nextGaussian();
            double reaction = REACTION_MEDIAN_MS * Math.exp(REACTION_SIGMA * rng.nextGaussian() + 0.25 * drift);
            if (rng.nextDouble() < HESITATION_CHANCE) reaction += 50 + rng.nextDouble() * 100;
            t += reaction;
        }
        return Math.max(0, t);
    }
}
