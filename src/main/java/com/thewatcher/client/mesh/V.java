package com.thewatcher.client.mesh;

/** Tiny immutable 3D vector (model space, in pixels: 16 = 1 block, +y is down, -z is the front). */
public record V(float x, float y, float z) {
    public static V of(double x, double y, double z) {
        return new V((float) x, (float) y, (float) z);
    }

    public V add(V o) { return new V(x + o.x, y + o.y, z + o.z); }
    public V sub(V o) { return new V(x - o.x, y - o.y, z - o.z); }
    public V mul(float s) { return new V(x * s, y * s, z * s); }
    public V mul(V o) { return new V(x * o.x, y * o.y, z * o.z); }
    public float dot(V o) { return x * o.x + y * o.y + z * o.z; }
    public V cross(V o) { return new V(y * o.z - z * o.y, z * o.x - x * o.z, x * o.y - y * o.x); }
    public float len() { return (float) Math.sqrt(x * x + y * y + z * z); }

    public V norm() {
        float l = len();
        return l < 1.0e-6f ? this : mul(1f / l);
    }

    public static V lerp(V a, V b, float t) {
        return new V(a.x + (b.x - a.x) * t, a.y + (b.y - a.y) * t, a.z + (b.z - a.z) * t);
    }
}
