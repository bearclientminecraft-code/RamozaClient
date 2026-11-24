package dev.ramoza.client.gui.anim;

/** Fixed-duration tween driven by delta time (seconds). Always finishes. */
public final class Animation {
    private final float duration;
    private final Easing easing;
    private float from;
    private float to;
    private float value;
    private float elapsed;
    private boolean running;

    public Animation(float initial, float duration, Easing easing) {
        this.value = initial;
        this.from = initial;
        this.to = initial;
        this.duration = duration;
        this.easing = easing;
    }

    public void animateTo(float target) {
        if (target == to) return;
        from = value;
        to = target;
        elapsed = 0f;
        running = true;
    }

    public void snap(float v) {
        value = v;
        from = v;
        to = v;
        running = false;
    }

    public void update(float dt) {
        if (!running) return;
        elapsed += dt;
        float t = duration <= 0f ? 1f : Math.min(1f, elapsed / duration);
        value = from + (to - from) * easing.apply(t);
        if (t >= 1f) {
            value = to;
            running = false;
        }
    }

    public float get() { return value; }
    public float target() { return to; }
    public boolean isRunning() { return running; }
}
