package com.thewatcher.client.mesh;

import java.util.Arrays;

/**
 * Builds smooth, organic meshes out of lofted tubes (limbs, tendons, claws, teeth) and
 * sculpted ellipsoids (skull, ribcage, joints). Normals are computed from the final,
 * deformed surface so lighting looks smooth instead of blocky.
 *
 * Output: float[] of quads, 11 floats per vertex (x, y, z, u, v, nx, ny, nz, r, g, b), 4 vertices per quad.
 * r,g,b = baked ambient occlusion x tint x blood stains: creases and sockets come out darker, and anything
 * near a stain (wounds, mouths, claws, drips) is soaked red.
 * Positions are in pixels (16 = 1 block). Does not depend on Minecraft, so it can be unit-tested
 * and exported outside the game.
 */
public final class MeshBuilder {
    public static final int STRIDE = 11;

    /** Which cell of the generated 5x2 texture atlas a surface samples. */
    public enum Swatch {
        SKIN(0, 0), BONE(1, 0), HAIR(2, 0), CLOTH(3, 0), PALE(4, 0),
        FLESH(0, 1), EYE(1, 1), SCALP(2, 1), KERATIN(3, 1), HIDE(4, 1);
        public static final int COLS = 5, ROWS = 2;
        public final int col, row;
        public final float u0, v0;
        Swatch(int col, int row) { this.col = col; this.row = row; this.u0 = col / (float) COLS; this.v0 = row / (float) ROWS; }
    }

    /** One texture tile covers about this many model pixels, so detail is the same size on every body part. */
    private static final float TEX_TILE = 9f;

    /** Sculpting: given a unit direction from the centre, return per-axis radius multipliers. */
    @FunctionalInterface
    public interface Deform {
        V apply(V dir);
    }

    private float[] data = new float[8192];
    private int size;

    /** A blood stain: ellipsoidal region (in this mesh's space) that tints vertices towards a colour. */
    private record Stain(V c, V r, float red, float green, float blue, float strength) {}
    private final java.util.List<Stain> stains = new java.util.ArrayList<>();

    /** Fresh (wet, bright) or old (dark, brown) blood soaking everything within {@code radii} of {@code center}. */
    public MeshBuilder blood(V center, V radii, boolean fresh, float strength) {
        if (fresh) stains.add(new Stain(center, radii, 0.44f, 0.025f, 0.03f, Math.min(1f, strength * 1.15f)));
        else stains.add(new Stain(center, radii, 0.21f, 0.04f, 0.035f, Math.min(1f, strength * 1.15f)));
        return this;
    }

    public MeshBuilder blood(V center, float radius, boolean fresh, float strength) {
        return blood(center, V.of(radius, radius, radius), fresh, strength);
    }

    public MeshBuilder clearBlood() {
        stains.clear();
        return this;
    }

    /** For temporary, local stains: remember how many there are... */
    public int bloodMark() { return stains.size(); }

    /** ...and drop any added since. */
    public MeshBuilder bloodReset(int mark) {
        while (stains.size() > mark) stains.remove(stains.size() - 1);
        return this;
    }

    public float currentTint() { return tint; }

    // Settings applied to the primitives that follow.
    private float tint = 1f, roughAmp = 0f, roughFreq = 1f, aoK = 1.2f;

    /** Darken/brighten everything after this (1 = normal). Used for hair, bruised areas, etc. */
    public MeshBuilder tint(float t) { this.tint = t; return this; }
    /** Lumpy, uneven surface: displacement in pixels and how fine the lumps are. */
    public MeshBuilder rough(float amp, float freq) { this.roughAmp = amp; this.roughFreq = freq; return this; }
    /** Strength of baked ambient occlusion in creases. */
    public MeshBuilder ao(float k) { this.aoK = k; return this; }

    public float[] build() {
        return Arrays.copyOf(data, size);
    }

    // ------------------------------------------------------------------ primitives

    private void vertex(V p, V n, Swatch s, float u, float v) {
        vertex(p, n, s, u, v, tint);
    }

    private void vertex(V p, V n, Swatch s, float u, float v, float shade) {
        vertex(p, n, s, u, v, shade, 0f);
    }

    private static boolean fleshy(Swatch s) {
        return s == Swatch.SKIN || s == Swatch.PALE || s == Swatch.HIDE || s == Swatch.FLESH || s == Swatch.SCALP;
    }

    /** {@code warm}: 0..1 how deep in a crease this is - blood under thin skin shows through there (redder, darker). */
    private void vertex(V p, V n, Swatch s, float u, float v, float shade, float warm) {
        if (size + STRIDE > data.length) data = Arrays.copyOf(data, data.length * 2);
        u = Math.max(0f, Math.min(1f, u));
        v = Math.max(0f, Math.min(1f, v));
        data[size++] = p.x();
        data[size++] = p.y();
        data[size++] = p.z();
        data[size++] = s.u0 + (0.006f + u * 0.988f) / Swatch.COLS;
        data[size++] = s.v0 + (0.006f + v * 0.988f) / Swatch.ROWS;
        data[size++] = n.x();
        data[size++] = n.y();
        data[size++] = n.z();
        float r = shade * (1f + 0.06f * warm), g = shade * (1f - 0.26f * warm), b = shade * (1f - 0.2f * warm);
        for (Stain st : stains) {
            V d = p.sub(st.c());
            // Blood runs down (+y is down): below the stain, narrow lanes stretch it into drip runs.
            float ry = st.r().y();
            if (d.y() > 0f) {
                float lane = noise3(p.x() * 1.9f + 31f, 5.3f, p.z() * 1.9f + 7f);
                lane = Math.max(0f, Math.min(1f, (lane - 0.48f) / 0.22f));
                ry *= 1f + 2.2f * lane * lane;
            }
            float q = (d.x() / st.r().x()) * (d.x() / st.r().x()) + (d.y() / ry) * (d.y() / ry)
                    + (d.z() / st.r().z()) * (d.z() / st.r().z());
            // Ragged, splattered edge instead of a soft round blob.
            q += (noise3(p.x() * 1.3f + 11f, p.y() * 1.3f, p.z() * 1.3f - 4f) - 0.5f) * 0.9f
                    + (noise3(p.x() * 4.1f, p.y() * 4.1f + 9f, p.z() * 4.1f) - 0.5f) * 0.35f;
            if (q >= 1f) continue;
            float k = Math.min(1f, (1f - q) * 2.4f);
            k = k * k * (3 - 2 * k) * st.strength();
            r += (shade * st.red() * 1.4f - r) * k;
            g += (shade * st.green() * 1.4f - g) * k;
            b += (shade * st.blue() * 1.4f - b) * k;
        }
        data[size++] = r;
        data[size++] = g;
        data[size++] = b;
    }

    /**
     * Surface from a grid of points. Columns wrap around (P[i][cols] duplicates P[i][0]).
     * {@code tu}/{@code tv} are unwrapped texture coordinates per column/row (may run past 1; the texture
     * repeats seamlessly, and callers keep tile boundaries on grid lines).
     */
    private void grid(V[][] p, V[][] approx, Swatch s, float[] tu, float[] tv) {
        int rows = p.length - 1, cols = p[0].length - 1;

        // 1. Organic surface: push every vertex in/out with 3D noise (same noise for shared positions -> no cracks).
        if (roughAmp > 0f) {
            for (int i = 0; i <= rows; i++) {
                for (int j = 0; j < cols; j++) {
                    V q = p[i][j];
                    float n = noise3(q.x() * roughFreq, q.y() * roughFreq, q.z() * roughFreq) * 0.65f
                            + noise3(q.x() * roughFreq * 2.7f + 17f, q.y() * roughFreq * 2.7f, q.z() * roughFreq * 2.7f) * 0.35f;
                    p[i][j] = q.add(approx[i][j].mul(roughAmp * (n * 2f - 1f)));
                }
                p[i][cols] = p[i][0];
            }
        }

        // 2. Smooth normals from the final surface.
        V[][] n = new V[rows + 1][cols + 1];
        for (int i = 0; i <= rows; i++) {
            for (int j = 0; j <= cols; j++) {
                int jn = j == cols ? 1 : j + 1;
                int jp = j == 0 ? cols - 1 : j - 1;
                V around = p[i][jn].sub(p[i][jp]);
                V along = p[Math.min(rows, i + 1)][j].sub(p[Math.max(0, i - 1)][j]);
                V nn = along.cross(around);
                if (nn.len() < 1.0e-5f) {
                    nn = approx[i][j];
                } else {
                    nn = nn.norm();
                    if (nn.dot(approx[i][j]) < 0) nn = nn.mul(-1f);
                }
                n[i][j] = nn;
            }
        }

        // 3. Baked ambient occlusion from local concavity: dents (sockets, creases, between ribs) go dark.
        float[][] shade = new float[rows + 1][cols + 1];
        float[][] warm = new float[rows + 1][cols + 1];
        boolean fl = fleshy(s);
        for (int i = 0; i <= rows; i++) {
            for (int j = 0; j <= cols; j++) {
                int jn = j == cols ? 1 : j + 1;
                int jp = j == 0 ? cols - 1 : j - 1;
                V a = p[i][jn], b = p[i][jp], c = p[Math.min(rows, i + 1)][j], d = p[Math.max(0, i - 1)][j];
                V avg = a.add(b).add(c).add(d).mul(0.25f).sub(p[i][j]);
                float edge = (a.sub(p[i][j]).len() + b.sub(p[i][j]).len() + c.sub(p[i][j]).len() + d.sub(p[i][j]).len()) * 0.25f;
                float cav = edge < 1.0e-5f ? 0f : avg.dot(n[i][j]) / edge;
                float ao = 1f - aoK * Math.min(0.75f, Math.max(0f, cav * 3.2f)) + 0.08f * Math.min(1f, Math.max(0f, -cav * 3f));
                shade[i][j] = Math.max(0.08f, ao) * tint;
                warm[i][j] = fl ? Math.min(1f, Math.max(0f, cav * 4f)) : 0f;
            }
        }

        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                float bu = (float) Math.floor(tu[j] + 1e-4f), bv = (float) Math.floor(tv[i] + 1e-4f);
                float u0 = tu[j] - bu, u1 = tu[j + 1] - bu;
                float v0 = tv[i] - bv, v1 = tv[i + 1] - bv;
                vertex(p[i][j], n[i][j], s, u0, v0, shade[i][j], warm[i][j]);
                vertex(p[i][j + 1], n[i][j + 1], s, u1, v0, shade[i][j + 1], warm[i][j + 1]);
                vertex(p[i + 1][j + 1], n[i + 1][j + 1], s, u1, v1, shade[i + 1][j + 1], warm[i + 1][j + 1]);
                vertex(p[i + 1][j], n[i + 1][j], s, u0, v1, shade[i + 1][j], warm[i + 1][j]);
            }
        }
    }

    /** Texture tiling for a wrapped surface: returns {sides, rings, tilesU, vRange} adjusted so tiles land on grid lines. */
    private static float[] tiling(float circumference, float length, int sides, int rings) {
        int tilesU = Math.max(1, Math.round(circumference / TEX_TILE));
        sides = (int) Math.ceil(sides / (double) tilesU) * tilesU;
        float vRange = length / (circumference / tilesU);
        if (vRange > 1f) {
            int tilesV = Math.max(1, Math.round(vRange));
            rings = (int) Math.ceil(rings / (double) tilesV) * tilesV;
            vRange = tilesV;
        }
        return new float[]{sides, rings, tilesU, vRange};
    }

    private static float[] span(int n, float range) {
        float[] r = new float[n + 1];
        for (int i = 0; i <= n; i++) r[i] = range * i / n;
        return r;
    }

    /** Simple tapered tube a->b, both ends capped, 10 sides. */
    public MeshBuilder tube(V a, V b, float r0, float r1, Swatch s) {
        return tube(a, b, r0, r1, s, 10, 5, 0f, 1f, 1f, null, true, true);
    }

    /**
     * Lofted tube along a->b.
     * @param bulge  muscle bulge (+) or pinch (-) at the middle, as a fraction of the radius
     * @param ovU    cross-section stretch along {@code hint}
     * @param ovW    cross-section stretch perpendicular to {@code hint}
     */
    public MeshBuilder tube(V a, V b, float r0, float r1, Swatch s, int sides, int rings,
                            float bulge, float ovU, float ovW, V hint, boolean capA, boolean capB) {
        V d = b.sub(a).norm();
        float rMax = Math.max(r0, r1), lenAB = b.sub(a).len();
        boolean limb = anatomy && (s == Swatch.SKIN || s == Swatch.PALE || s == Swatch.HIDE) && rMax >= 0.5f && rMax <= 2.2f && lenAB >= 3.2f * rMax;
        if (limb) {
            sides = Math.max(sides, 18);
            rings = Math.max(rings, 20);
        }
        float circ = (float) (Math.PI * (r0 + r1) * (ovU + ovW) * 0.5f);
        float[] tl = tiling(Math.max(circ, 0.5f), b.sub(a).len(), sides, rings);
        sides = (int) tl[0];
        rings = (int) tl[1];
        float[] tu = span(sides, tl[2]), tv = span(rings, tl[3]);
        double phase = ((a.x() * 0.37 + a.y() * 0.71 + a.z() * 0.13) % 1.0) * Math.PI * 2;   // vary the pattern per part
        double ph1 = phase * 1.7 + 0.9, ph2 = phase * 2.3 + 2.1, ph3 = phase * 0.6 + 4.0;
        V h = hint != null ? hint : (Math.abs(d.y()) > 0.9f ? V.of(1, 0, 0) : V.of(0, 1, 0));
        V u = h.sub(d.mul(h.dot(d))).norm();
        V w = d.cross(u).norm();

        V[][] p = new V[rings + 1][sides + 1];
        V[][] approx = new V[rings + 1][sides + 1];
        V[] ring0 = new V[sides + 1], ringN = new V[sides + 1];
        for (int i = 0; i <= rings; i++) {
            float t = (float) i / rings;
            V c = V.lerp(a, b, t);
            float r = (r0 + (r1 - r0) * t) * (1f + bulge * (float) Math.sin(Math.PI * t));
            for (int j = 0; j <= sides; j++) {
                double ang = 2 * Math.PI * (j % sides) / sides + phase;
                float cos = (float) Math.cos(ang), sin = (float) Math.sin(ang);
                float ra = r;
                if (limb) ra = r * anatomy(t, ang, ph1, ph2, ph3, bulge);
                V off = u.mul(cos * ra * ovU).add(w.mul(sin * ra * ovW));
                p[i][j] = c.add(off);
                approx[i][j] = u.mul(cos / ovU).add(w.mul(sin / ovW)).norm();
            }
        }
        System.arraycopy(p[0], 0, ring0, 0, sides + 1);
        System.arraycopy(p[rings], 0, ringN, 0, sides + 1);
        grid(p, approx, s, tu, tv);
        if (capA) cap(ring0, a, d.mul(-1f), s);
        if (capB && r1 > 0.02f) cap(ringN, b, d, s);
        return this;
    }

    private boolean anatomy = true;

    /** Starved, sinewy limbs: flared joint ends, tendon cords and a bone ridge pressing through. On by default. */
    public MeshBuilder anatomy(boolean on) { this.anatomy = on; return this; }

    private static float gauss(float x) { return (float) Math.exp(-x * x); }

    /** Radius multiplier for a limb at {@code t} (0..1 along it) and angle {@code ang} around it. */
    private static float anatomy(float t, double ang, double ph1, double ph2, double ph3, float bulge) {
        float flare = 0.3f * (gauss(t / 0.1f) + gauss((1f - t) / 0.1f));                   // knuckly joint ends
        float pinch = -0.16f * gauss((t - 0.78f) / 0.12f) - 0.1f * gauss((t - 0.18f) / 0.07f);  // wasted just off the joints
        float belly = Math.max(0f, bulge) * 0.9f * gauss((t - 0.33f) / 0.2f);                 // what muscle is left, high up
        float lower = Math.max(0f, Math.min(1f, (t - 0.3f) / 0.5f));
        float tendon = (float) (0.26f * Math.pow(Math.max(0, Math.cos(ang * 2 + ph1)), 6)
                + 0.16f * Math.pow(Math.max(0, Math.cos(ang * 3 + ph2)), 8)) * (0.25f + 0.75f * lower);
        float ridge = (float) (0.3f * Math.pow(Math.max(0, Math.cos(ang + ph3)), 10)) * (0.35f + 0.65f * (float) Math.sin(Math.PI * t));
        return 1f + flare + pinch + belly + tendon + ridge - 0.12f;
    }

    private void cap(V[] ring, V center, V normal, Swatch s) {
        for (int j = 0; j < ring.length - 1; j++) {
            vertex(ring[j], normal, s, 0.5f, 0.5f);
            vertex(ring[j + 1], normal, s, 0.55f, 0.5f);
            vertex(center, normal, s, 0.52f, 0.55f);
            vertex(center, normal, s, 0.52f, 0.55f);
        }
    }

    /** A pointed cone: claws, teeth. */
    public MeshBuilder cone(V base, V tip, float r, Swatch s) {
        float ra = roughAmp;
        roughAmp = 0f;
        tube(base, tip, r, 0.0f, s, 7, 3, 0f, 1f, 1f, null, true, false);
        roughAmp = ra;
        return this;
    }

    /**
     * Flat, double-sided strip following a path: a hair card. Width runs perpendicular to the path and to
     * the direction away from {@code center}, so cards lie along the skull and face outwards when hanging.
     * The HAIR texture has transparent gaps between strands, so each card reads as a lock of fine hair.
     */
    public MeshBuilder ribbon(V[] path, float w0, float w1, V center, Swatch s, float uA, float uB,
                              float shadeRoot, float shadeTip) {
        int n = path.length;
        V[] left = new V[n], right = new V[n], nor = new V[n];
        for (int i = 0; i < n; i++) {
            V t = path[Math.min(n - 1, i + 1)].sub(path[Math.max(0, i - 1)]).norm();
            V out = path[i].sub(center).norm();
            V side = t.cross(out);
            if (side.len() < 1.0e-4f) side = V.of(1, 0, 0);
            side = side.norm();
            float w = (w0 + (w1 - w0) * i / (n - 1)) * 0.5f;
            left[i] = path[i].sub(side.mul(w));
            right[i] = path[i].add(side.mul(w));
            nor[i] = side.cross(t).norm();
            if (nor[i].dot(out) < 0) nor[i] = nor[i].mul(-1f);
        }
        for (int i = 0; i < n - 1; i++) {
            float v0 = (float) i / (n - 1), v1 = (float) (i + 1) / (n - 1);
            float s0 = (shadeRoot + (shadeTip - shadeRoot) * v0) * tint;
            float s1 = (shadeRoot + (shadeTip - shadeRoot) * v1) * tint;
            vertex(left[i], nor[i], s, uA, v0, s0);
            vertex(right[i], nor[i], s, uB, v0, s0);
            vertex(right[i + 1], nor[i + 1], s, uB, v1, s1);
            vertex(left[i + 1], nor[i + 1], s, uA, v1, s1);
        }
        return this;
    }

    /** Tube following a curved path (hair, sinew). Uses parallel-transport frames so it doesn't twist. */
    public MeshBuilder loft(V[] path, float r0, float r1, int sides, Swatch s) {
        int rings = path.length - 1;
        float len = 0f;
        for (int i = 0; i < rings; i++) len += path[i + 1].sub(path[i]).len();
        int tilesU = Math.max(1, Math.round((float) (Math.PI * (r0 + r1)) / TEX_TILE));
        sides = (int) Math.ceil(sides / (double) tilesU) * tilesU;
        float[] tu = span(sides, tilesU);
        float vRange = Math.min(1f, len / Math.max(0.3f, (float) (Math.PI * (r0 + r1)) / tilesU));
        float[] tv = new float[rings + 1];
        float acc = 0f;
        for (int i = 0; i <= rings; i++) {
            tv[i] = len > 0f ? acc / len * vRange : 0f;
            if (i < rings) acc += path[i + 1].sub(path[i]).len();
        }
        V[][] p = new V[rings + 1][sides + 1];
        V[][] approx = new V[rings + 1][sides + 1];
        V t0 = path[1].sub(path[0]).norm();
        V u = (Math.abs(t0.y()) > 0.9f ? V.of(1, 0, 0) : V.of(0, 1, 0));
        u = u.sub(t0.mul(u.dot(t0))).norm();
        for (int i = 0; i <= rings; i++) {
            V t = path[Math.min(rings, i + 1)].sub(path[Math.max(0, i - 1)]).norm();
            u = u.sub(t.mul(u.dot(t))).norm();
            V w = t.cross(u).norm();
            float r = r0 + (r1 - r0) * i / rings;
            for (int j = 0; j <= sides; j++) {
                double ang = 2 * Math.PI * (j % sides) / sides;
                V off = u.mul((float) Math.cos(ang)).add(w.mul((float) Math.sin(ang)));
                p[i][j] = path[i].add(off.mul(r));
                approx[i][j] = off;
            }
        }
        float ra = roughAmp;
        roughAmp = 0f;
        grid(p, approx, s, tu, tv);
        roughAmp = ra;
        return this;
    }

    public MeshBuilder ellipsoid(V c, float rx, float ry, float rz, Swatch s) {
        return ellipsoid(c, rx, ry, rz, 8, 12, s, null);
    }

    /** Ellipsoid with optional sculpting. lat = rings top-to-bottom, lon = segments around. */
    public MeshBuilder ellipsoid(V c, float rx, float ry, float rz, int lat, int lon, Swatch s, Deform f) {
        V radii = V.of(rx, ry, rz);
        float[] tu, tv;
        if (s == Swatch.EYE) {                       // the eyeball texture is painted for exact sphere coordinates
            tu = span(lon, 1f);
            tv = span(lat, 1f);
        } else {
            float circ = (float) (Math.PI * (rx + rz));
            float merid = (float) (Math.PI * (ry + (rx + rz) * 0.5f) * 0.5f);
            float[] tl = tiling(Math.max(circ, 0.5f), merid, lon, lat);
            lon = (int) tl[0];
            lat = (int) tl[1];
            tu = span(lon, tl[2]);
            tv = span(lat, tl[3]);
        }
        V[][] p = new V[lat + 1][lon + 1];
        V[][] approx = new V[lat + 1][lon + 1];
        for (int i = 0; i <= lat; i++) {
            double th = Math.PI * i / lat;
            for (int j = 0; j <= lon; j++) {
                double ph = 2 * Math.PI * (j % lon) / lon;
                V dir = V.of(Math.sin(th) * Math.cos(ph), -Math.cos(th), Math.sin(th) * Math.sin(ph));
                V m = f == null ? V.of(1, 1, 1) : f.apply(dir);
                p[i][j] = c.add(dir.mul(radii).mul(m));
                approx[i][j] = V.of(dir.x() / rx, dir.y() / ry, dir.z() / rz).norm();
            }
        }
        grid(p, approx, s, tu, tv);
        return this;
    }

    // 3D value noise for surface lumps.
    private static float hash3(int x, int y, int z) {
        int h = x * 374761393 + y * 668265263 + z * 1274126177;
        h = (h ^ (h >>> 13)) * 1103515245;
        h ^= h >>> 16;
        return (h & 0xFFFFFF) / (float) 0xFFFFFF;
    }

    public static float noise3(float x, float y, float z) {
        int xi = (int) Math.floor(x), yi = (int) Math.floor(y), zi = (int) Math.floor(z);
        float fx = x - xi, fy = y - yi, fz = z - zi;
        fx = fx * fx * (3 - 2 * fx);
        fy = fy * fy * (3 - 2 * fy);
        fz = fz * fz * (3 - 2 * fz);
        float a = lerp(lerp(hash3(xi, yi, zi), hash3(xi + 1, yi, zi), fx), lerp(hash3(xi, yi + 1, zi), hash3(xi + 1, yi + 1, zi), fx), fy);
        float b = lerp(lerp(hash3(xi, yi, zi + 1), hash3(xi + 1, yi, zi + 1), fx), lerp(hash3(xi, yi + 1, zi + 1), hash3(xi + 1, yi + 1, zi + 1), fx), fy);
        return lerp(a, b, fz);
    }

    private static float lerp(float a, float b, float t) { return a + (b - a) * t; }

    /** Smooth gaussian bump on a sphere: 1 at {@code centre}, falling off with angular distance. */
    public static float bump(V dir, V centre, float width) {
        float a = 1f - dir.dot(centre);
        return (float) Math.exp(-(a / width) * (a / width));
    }
}
