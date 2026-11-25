package com.thewatcher.client.mesh;

/**
 * Helpers for building limbs: aim each bone along a direction, with geometry modelled along +y.
 * Bones rotate like vanilla ModelParts (Z * Y * X); limbs here use pitch and roll only.
 */
public final class RigMath {
    private RigMath() {}

    /** Pitch/roll that turn local +y into direction {@code d}. */
    public static float[] anglesFor(V d) {
        d = d.norm();
        float p = (float) Math.asin(Math.max(-1f, Math.min(1f, d.z())));
        float r = (float) Math.atan2(-d.x(), d.y());
        return new float[]{p, r};
    }

    /** 3x3 rotation Rz(roll) * Rx(pitch), row-major. */
    public static float[] mat(float pitch, float roll) {
        float cp = (float) Math.cos(pitch), sp = (float) Math.sin(pitch);
        float cr = (float) Math.cos(roll), sr = (float) Math.sin(roll);
        // Rz * Rx
        return new float[]{
                cr, -sr * cp, sr * sp,
                sr, cr * cp, -cr * sp,
                0f, sp, cp
        };
    }

    public static float[] mul(float[] a, float[] b) {
        float[] r = new float[9];
        for (int i = 0; i < 3; i++)
            for (int j = 0; j < 3; j++)
                r[i * 3 + j] = a[i * 3] * b[j] + a[i * 3 + 1] * b[3 + j] + a[i * 3 + 2] * b[6 + j];
        return r;
    }

    /** M^T * v (inverse of a rotation). */
    public static V invApply(float[] m, V v) {
        return V.of(m[0] * v.x() + m[3] * v.y() + m[6] * v.z(),
                m[1] * v.x() + m[4] * v.y() + m[7] * v.z(),
                m[2] * v.x() + m[5] * v.y() + m[8] * v.z());
    }

    public static final float[] IDENTITY = {1, 0, 0, 0, 1, 0, 0, 0, 1};

    /**
     * Builds a chain of bones under {@code parent} (which must be unrotated at rest). Segment k points along
     * world direction dirs[k] with length lens[k]; its mesh is a tapered tube plus a knobbly joint at the end.
     * Returns the bones; the last one ends where you can attach a hand/foot at (0, lens[last], 0).
     */
    public static Bone[] chain(Bone parent, String name, V start, V[] dirs, float[] lens, float[] radii,
                               MeshBuilder.Swatch swatch, float tint, float rough) {
        float[] bulges = new float[dirs.length];
        java.util.Arrays.fill(bulges, 0.1f);
        return chain(parent, name, start, dirs, lens, radii, bulges, 1.35f, swatch, tint, rough);
    }

    /** Same, with a muscle bulge per segment and a joint-knob size (relative to the end radius). */
    public static Bone[] chain(Bone parent, String name, V start, V[] dirs, float[] lens, float[] radii, float[] bulges,
                               float knob, MeshBuilder.Swatch swatch, float tint, float rough) {
        Bone[] out = new Bone[dirs.length];
        float[] m = IDENTITY;
        Bone p = parent;
        for (int k = 0; k < dirs.length; k++) {
            V local = invApply(m, dirs[k]);
            float[] a = anglesFor(local);
            V pivot = k == 0 ? start : V.of(0, lens[k - 1], 0);
            Bone b = p.add(new Bone(name + k, pivot.x(), pivot.y(), pivot.z(), a[0], 0f, a[1]));
            float r0 = radii[k], r1 = radii[k + 1];
            b.mesh = new MeshBuilder().tint(tint).rough(rough, 0.9f).ao(1.3f)
                    .tube(V.of(0, 0, 0), V.of(0, lens[k], 0), r0, r1, swatch, 12, 8, bulges[k], 1f, 1f, V.of(1, 0, 0), false, false)
                    .ellipsoid(V.of(0, lens[k], 0), r1 * knob * 0.92f, r1 * knob * 0.85f, r1 * knob, 10, 14, swatch, RigMath::condyle)
                    .build();
            m = mul(m, mat(a[0], a[1]));
            out[k] = b;
            p = b;
        }
        return out;
    }

    /** A knuckly joint: two bony lobes either side, a dip front and back, the kneecap/elbow point proud. */
    public static V condyle(V d) {
        float lobes = 0.16f * (MeshBuilder.bump(d, V.of(1, 0, 0), 0.35f) + MeshBuilder.bump(d, V.of(-1, 0, 0), 0.35f));
        float point = 0.12f * MeshBuilder.bump(d, V.of(0, 0.3, -0.95).norm(), 0.25f);
        float dip = -0.1f * MeshBuilder.bump(d, V.of(0, 0, 1), 0.4f);
        float m = 1f + lobes + point + dip;
        return V.of(m, m, m);
    }

    /** A vertebra under the skin: spinous process pointing back (+z), transverse knobs either side. */
    public static V vertebra(V d) {
        float m = 1f + 0.55f * MeshBuilder.bump(d, V.of(0, 0, 1), 0.3f)
                + 0.22f * (MeshBuilder.bump(d, V.of(1, 0, 0.3).norm(), 0.25f) + MeshBuilder.bump(d, V.of(-1, 0, 0.3).norm(), 0.25f))
                - 0.15f * MeshBuilder.bump(d, V.of(0, 1, 0), 0.5f) - 0.15f * MeshBuilder.bump(d, V.of(0, -1, 0), 0.5f);
        return V.of(m, m, m);
    }

    /** World rotation of the end of a chain built from these dirs (for orienting a hand/foot). */
    public static float[] chainRotation(V[] dirs) {
        float[] m = IDENTITY;
        for (V d : dirs) {
            float[] a = anglesFor(invApply(m, d));
            m = mul(m, mat(a[0], a[1]));
        }
        return m;
    }
}
