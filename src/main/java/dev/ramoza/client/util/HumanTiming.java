package dev.ramoza.client.util;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Human-like random sources for timing and hit decisions. Replaces flat uniform jitter, which is easy
 * to recognise statistically, with three properties real input has:
 * <ul>
 *   <li>right-skewed delays (log-normal): most values near the median, a long tail of slow ones;</li>
 *   <li>correlation: consecutive values drift together (attention / fatigue) instead of being independent;</li>
 *   <li>streaks: misses and hits cluster instead of being independent coin flips.</li>
 * </ul>
 * One instance per module, render thread only. Allocation-free.
 */
public final class HumanTiming {
    /** AR(1) coefficient of the timing drift: how strongly the next delay follows the previous one. */
    private static final double DRIFT_KEEP = 0.85;
    private static final double DRIFT_NOISE = Math.sqrt(1.0 - DRIFT_KEEP * DRIFT_KEEP);
    /** AR(1) coefficient of the focus state that modulates the hit chance. */
    private static final double FOCUS_KEEP = 0.90;
    private static final double FOCUS_NOISE = Math.sqrt(1.0 - FOCUS_KEEP * FOCUS_KEEP);
    /** How strongly focus moves the hit chance (in logit space). */
    private static final double FOCUS_WEIGHT = 0.7;

    private double drift;
    private double focus;

    public void reset() {
        drift = 0.0;
        focus = 0.0;
    }

    /**
     * Log-normal delay in ms around {@code medianMs}.
     *
     * @param sigma            log-space spread (0.2 = tight, 0.5 = loose)
     * @param driftWeight      how much the slowly varying alertness state scales this value
     * @param hesitationChance chance of a rare slow outlier (+80..250 ms)
     */
    public double delay(double medianMs, double sigma, double driftWeight, double hesitationChance) {
        ThreadLocalRandom rng = ThreadLocalRandom.current();
        drift = DRIFT_KEEP * drift + DRIFT_NOISE * rng.nextGaussian();
        double t = medianMs * Math.exp(sigma * rng.nextGaussian() + driftWeight * drift);
        if (rng.nextDouble() < hesitationChance) t += 80.0 + rng.nextDouble() * 170.0;
        return t;
    }

    /** Value in [lo, hi] skewed towards lo (no clipping, so no pile-up at the bounds). */
    public double skewedBetween(double a, double b) {
        double lo = Math.min(a, b);
        double hi = Math.max(a, b);
        if (hi <= lo) return lo;
        return lo + (hi - lo) * Math.pow(ThreadLocalRandom.current().nextDouble(), 1.6);
    }

    /**
     * Hit decision whose long-run rate is about {@code chancePercent}, but with streaks: a slowly varying
     * focus state shifts the probability up and down between calls. 0 and 100 stay exact.
     */
    public boolean roll(double chancePercent) {
        if (chancePercent >= 100.0) return true;
        if (chancePercent <= 0.0) return false;
        ThreadLocalRandom rng = ThreadLocalRandom.current();
        focus = FOCUS_KEEP * focus + FOCUS_NOISE * rng.nextGaussian();
        double p = chancePercent / 100.0;
        double logit = Math.log(p / (1.0 - p)) + FOCUS_WEIGHT * focus;
        double effective = 1.0 / (1.0 + Math.exp(-logit));
        return rng.nextDouble() < effective;
    }
}
