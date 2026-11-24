package dev.ramoza.client.setting;

import dev.ramoza.client.util.MathUtil;

public final class NumberSetting extends Setting<Double> {
    private final double min;
    private final double max;
    private final double step;

    public NumberSetting(String name, String description, double def, double min, double max, double step) {
        super(name, description, def);
        this.min = min;
        this.max = max;
        this.step = step;
        this.value = snap(def);
    }

    public double min() { return min; }
    public double max() { return max; }
    public double step() { return step; }

    @Override public void set(Double v) { this.value = snap(v); }

    private double snap(double v) {
        double c = MathUtil.clamp(v, min, max);
        if (step <= 0) return c;
        double snapped = Math.round((c - min) / step) * step + min;
        return MathUtil.clamp(snapped, min, max);
    }

    public String format() {
        if (step > 0 && step >= 1 && step == Math.floor(step)) return String.valueOf(Math.round(value));
        return String.format(java.util.Locale.ROOT, "%.2f", value);
    }

    @Override public String serialize() { return Double.toString(value); }

    @Override public void deserialize(String s) {
        try {
            set(Double.parseDouble(s));
        } catch (NumberFormatException ignored) {
            // keep current value
        }
    }
}
