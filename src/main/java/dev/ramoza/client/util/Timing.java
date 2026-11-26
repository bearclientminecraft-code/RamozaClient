package dev.ramoza.client.util;

import java.util.concurrent.ThreadLocalRandom;

/** Small helpers for "Delay +/- Random Delay" style settings. */
public final class Timing {
    private Timing() {}

    /** {@code base +/- uniform(random)} in milliseconds, never below 0. */
    public static double jitter(double base, double random) {
        double t = base;
        if (random > 0.0) t += ThreadLocalRandom.current().nextDouble(-random, random);
        return Math.max(0.0, t);
    }

    public static long nowMs() { return System.nanoTime() / 1_000_000L; }
}
