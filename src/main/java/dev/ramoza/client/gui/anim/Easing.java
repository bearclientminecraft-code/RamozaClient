package dev.ramoza.client.gui.anim;

public enum Easing {
    LINEAR {
        @Override public float apply(float t) { return t; }
    },
    EASE_OUT_CUBIC {
        @Override public float apply(float t) { float u = 1f - t; return 1f - u * u * u; }
    },
    EASE_IN_OUT_CUBIC {
        @Override public float apply(float t) {
            if (t < 0.5f) return 4f * t * t * t;
            float u = -2f * t + 2f;
            return 1f - u * u * u / 2f;
        }
    },
    SMOOTHSTEP {
        @Override public float apply(float t) { return t * t * (3f - 2f * t); }
    };

    /** @param t progress in 0..1 (clamped) */
    public abstract float apply(float t);

    public float applyClamped(float t) {
        return apply(t < 0f ? 0f : Math.min(t, 1f));
    }
}
