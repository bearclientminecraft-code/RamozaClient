package dev.ramoza.client.gui.anim;

/**
 * Frame-rate independent exponential follower: the value approaches the target by
 * {@code 1 - exp(-speed * dt)} each update and snaps once it is close enough, so it always settles.
 */
public final class SmoothFloat {
    private static final float EPSILON = 0.0005f;

    private final float speed;
    private float value;
    private float target;

    public SmoothFloat(float initial, float speed) {
        this.value = initial;
        this.target = initial;
        this.speed = speed;
    }

    public void setTarget(float t) { target = t; }

    public void snap(float v) {
        value = v;
        target = v;
    }

    public void update(float dt) {
        if (value == target) return;
        float k = 1f - (float) Math.exp(-speed * dt);
        value += (target - value) * k;
        if (Math.abs(target - value) < EPSILON) value = target;
    }

    public float get() { return value; }
    public float target() { return target; }
    public boolean isSettled() { return value == target; }
}
