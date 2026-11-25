package com.thewatcher.client.mesh;

/**
 * Per-creature animation memory, so motion carries over between frames: springs (with overshoot and
 * wobble), smoothed blends between poses, lagging secondary motion, timers, and turn speed.
 * All rates are per game tick (20 per second).
 */
public final class AnimState {
    private static final int SLOTS = 64;
    private final float[] x = new float[SLOTS], v = new float[SLOTS];
    private final boolean[] ready = new boolean[SLOTS];

    public float age, dt;
    private float lastAge = Float.NaN;
    public long lastTouched;

    // jerky head tracking
    public float heldYaw, heldPitch, nextSnap;
    // turning
    private float lastBodyYaw = Float.NaN;
    public float turnRate;
    // free timers
    public float t0, t1, t2;

    /** Call once per frame with the creature's age (ticks + partial tick). */
    public void tick(float age) {
        if (Float.isNaN(lastAge) || age < lastAge || age - lastAge > 20f) dt = 0f;
        else dt = Math.min(age - lastAge, 1f);
        lastAge = age;
        this.age = age;
    }

    /** Body yaw in degrees; tracks how fast the creature is turning (degrees per tick, smoothed). */
    public void bodyYaw(float deg) {
        if (!Float.isNaN(lastBodyYaw) && dt > 0f) {
            float d = deg - lastBodyYaw;
            while (d > 180f) d -= 360f;
            while (d < -180f) d += 360f;
            turnRate += (d / dt - turnRate) * (1f - (float) Math.exp(-0.25f * dt));
        }
        lastBodyYaw = deg;
    }

    /** Exponential approach to {@code target}; {@code rate} ~ fraction per tick. */
    public float smooth(int i, float target, float rate) {
        if (!ready[i]) { x[i] = target; ready[i] = true; }
        x[i] += (target - x[i]) * (1f - (float) Math.exp(-rate * dt));
        return x[i];
    }

    /** Damped spring towards {@code target}: low damping overshoots and wobbles. */
    public float spring(int i, float target, float stiffness, float damping) {
        if (!ready[i]) { x[i] = target; v[i] = 0f; ready[i] = true; }
        if (dt <= 0f) return x[i];
        int n = Math.max(1, (int) Math.ceil(dt / 0.2f));
        float h = dt / n;
        for (int k = 0; k < n; k++) {
            float a = stiffness * (target - x[i]) - damping * v[i];
            v[i] += a * h;
            x[i] += v[i] * h;
        }
        return x[i];
    }

    public float velocity(int i) { return v[i]; }

    public float value(int i) { return x[i]; }

    /** Deterministic noise in [0,1) for event "bucket" n (e.g. which few seconds we're in). */
    public static float hash(int n, int salt) {
        int h = n * 0x27d4eb2d ^ salt * 0x165667b1;
        h ^= h >>> 15;
        h *= 0x2c1b3c6d;
        h ^= h >>> 12;
        return (h & 0xFFFF) / 65536f;
    }

    /** Smooth 0..1..0 pulse while {@code t} is inside [start, start+len). */
    public static float pulse(float t, float start, float len) {
        if (t < start || t >= start + len) return 0f;
        return (float) Math.sin((t - start) / len * Math.PI);
    }

    public static float lerp(float a, float b, float t) { return a + (b - a) * t; }
}
