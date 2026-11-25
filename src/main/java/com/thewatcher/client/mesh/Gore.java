package com.thewatcher.client.mesh;

import java.util.Random;

import static com.thewatcher.client.mesh.MeshBuilder.Swatch.BONE;
import static com.thewatcher.client.mesh.MeshBuilder.Swatch.FLESH;
import static com.thewatcher.client.mesh.MeshBuilder.Swatch.SKIN;

/**
 * Gore building blocks for the creature meshes: torn wounds, broken bones jutting through the skin,
 * exposed bone, and blood running and dripping.
 * Blood on surrounding skin is done with {@link MeshBuilder#blood} stains, added before the skin is built.
 */
public final class Gore {
    private Gore() {}

    static V[] basis(V n) {
        n = n.norm();
        V a = Math.abs(n.y()) < 0.9f ? V.of(0, 1, 0) : V.of(1, 0, 0);
        V t1 = a.sub(n.mul(a.dot(n))).norm();
        V t2 = n.cross(t1).norm();
        return new V[]{t1, t2};
    }

    /** Closed ragged loop around {@code c} in the plane {@code b0,b1}: radius jitters so the tear is uneven. */
    static V[] tornRing(V c, V[] b, V nn, float r, float jag, float lift, Random rnd, int n) {
        V[] path = new V[n + 1];
        float[] off = new float[n];
        for (int k = 0; k < n; k++) off[k] = (rnd.nextFloat() - 0.5f) * 2f;
        for (int k = 0; k <= n; k++) {
            int i = k % n;
            double a = k * Math.PI * 2 / n;
            float rr = r * (1f + jag * off[i] + 0.12f * (float) Math.sin(a * 3 + off[0] * 3));
            path[k] = c.add(b[0].mul((float) Math.cos(a) * rr)).add(b[1].mul((float) Math.sin(a) * rr))
                    .add(nn.mul(lift * (0.6f + 0.4f * off[(i + 3) % n])));
        }
        return path;
    }

    /**
     * A torn, open wound: a funnel of raw meat sinking into a near-black wet hole, a thick torn lip of
     * swollen flesh all the way round, shreds of muscle inside and curled-back flaps of skin.
     */
    public static void wound(MeshBuilder mb, V c, V n, float r, Random rnd) {
        float t = mb.currentTint();
        V nn = n.norm();
        V[] b = basis(nn);
        int mark = mb.bloodMark();
        mb.blood(c, r * 2.4f, true, 0.95f);
        // the hole: a funnel sinking in (seen from the inside, so it shades dark), with a black wet bottom
        mb.tint(0.3f).rough(0.22f * r, 2.2f).ao(2.4f)
                .tube(c.add(nn.mul(r * 0.1f)), c.sub(nn.mul(r * 1.1f)), r * 1.02f, r * 0.25f, FLESH, 14, 6, -0.15f, 1f, 1f, b[0], false, true);
        mb.tint(0.12f).rough(0f, 1f).ellipsoid(c.sub(nn.mul(r * 1.0f)), r * 0.42f, r * 0.42f, r * 0.42f, 6, 8, FLESH, null);
        // shreds of torn muscle hanging inside the hole
        mb.tint(0.75f).rough(0f, 1f).ao(1.2f);
        for (int k = 0; k < 5; k++) {
            double a = rnd.nextDouble() * Math.PI * 2;
            V s0 = c.add(b[0].mul((float) Math.cos(a) * r * 0.85f)).add(b[1].mul((float) Math.sin(a) * r * 0.85f));
            V s1 = c.sub(nn.mul(r * (0.3f + rnd.nextFloat() * 0.5f))).add(b[0].mul((rnd.nextFloat() - 0.5f) * r * 0.6f))
                    .add(b[1].mul((rnd.nextFloat() - 0.5f) * r * 0.6f));
            V mid = V.lerp(s0, s1, 0.5f).add(nn.mul(r * 0.15f));
            mb.loft(new V[]{s0, mid, s1}, r * 0.11f, r * 0.04f, 5, FLESH);
        }
        // thick, swollen, ragged lip of raw flesh all the way round
        mb.tint(0.95f).rough(0.09f * r + 0.05f, 3.2f).ao(1.6f);
        mb.loft(tornRing(c, b, nn, r * 1.02f, 0.13f, r * 0.08f, rnd, 22), r * 0.2f, r * 0.2f, 8, FLESH);
        // curled-back flaps of skin
        mb.tint(0.75f).rough(0.06f, 2f);
        for (int k = 0; k < 3; k++) {
            double a = rnd.nextDouble() * Math.PI * 2;
            V dir = b[0].mul((float) Math.cos(a)).add(b[1].mul((float) Math.sin(a)));
            V p0 = c.add(dir.mul(r * 1.2f));
            V[] path = {p0, p0.add(dir.mul(r * 0.35f)).add(nn.mul(r * 0.3f)), p0.add(dir.mul(r * 0.45f)).add(nn.mul(r * 0.6f))};
            V side = dir.cross(nn).norm();
            mb.ribbon(path, r * 0.5f, r * 0.2f, path[0].sub(nn.mul(r)), SKIN, 0.2f, 0.8f, 0.9f, 0.6f);
        }
        mb.bloodReset(mark).tint(t).rough(0f, 1f);
    }

    /** A broken bone stabbing out through the skin: splintered end, marrow, a torn collar of flesh, blood. */
    public static float[] boneSpur(V base, V dir, float len, float r, Random rnd) {
        MeshBuilder mb = new MeshBuilder();
        boneSpur(mb, base, dir, len, r, rnd);
        return mb.build();
    }

    public static void boneSpur(MeshBuilder mb, V base, V dir, float len, float r, Random rnd) {
        float t = mb.currentTint();
        dir = dir.norm();
        V tip = base.add(dir.mul(len));
        int mark = mb.bloodMark();
        mb.blood(base, len * 0.75f, true, 0.8f);                     // fresh blood on the lower part of the bone
        mb.tint(1f).rough(0.04f, 3f).ao(1.2f)
                .tube(base.sub(dir.mul(r * 1.5f)), tip, r, r * 0.85f, BONE, 8, 6, 0.08f, 1f, 1f, null, false, false);
        V[] b = basis(dir);
        for (int k = 0; k < 4; k++) {                                // splintered, jagged end
            double a = k * Math.PI / 2 + rnd.nextDouble() * 0.6;
            V side = b[0].mul((float) Math.cos(a)).add(b[1].mul((float) Math.sin(a)));
            V s0 = tip.sub(dir.mul(r * 0.4f)).add(side.mul(r * 0.45f));
            mb.cone(s0, s0.add(dir.mul(r * (1.2f + rnd.nextFloat() * 1.4f))).add(side.mul(r * 0.4f)), r * 0.38f, BONE);
        }
        mb.tint(0.55f).ellipsoid(tip.sub(dir.mul(r * 0.1f)), r * 0.55f, r * 0.55f, r * 0.55f, 5, 7, FLESH, null);   // marrow
        mb.bloodReset(mark);
        wound(mb, base, dir, r * 1.5f, rnd);                         // where it tore out
        mb.tint(t);
    }

    /** A bare patch of bone showing through a hole in the skin (skull, shin, spine). */
    public static void exposedBone(MeshBuilder mb, V c, V n, float r, Random rnd) {
        float t = mb.currentTint();
        int mark = mb.bloodMark();
        mb.blood(c, r * 1.9f, true, 0.6f);
        V nn = n.norm();
        V[] b = basis(nn);
        // flattened bone plate, slightly proud of the surface
        mb.tint(1f).rough(0.05f, 3f).ao(1.3f);
        for (int k = 0; k < 5; k++) {
            V p = c.add(b[0].mul((rnd.nextFloat() - 0.5f) * r)).add(b[1].mul((rnd.nextFloat() - 0.5f) * r));
            mb.ellipsoid(p, r * 0.55f, r * 0.55f, r * 0.55f, 6, 8, BONE, null);
        }
        mb.bloodReset(mark);
        mb.tint(1f).rough(0.12f, 2.5f).ao(1.4f);
        mb.loft(tornRing(c, b, nn, r * 1.12f, 0.15f, r * 0.05f, rnd, 20), r * 0.2f, r * 0.2f, 7, FLESH);   // raw torn rim
        mb.tint(t).rough(0f, 1f);
    }

    /** A string of blood running off {@code start} along {@code down}, swinging slightly, with a drop on the end. */
    public static void drip(MeshBuilder mb, V start, V down, float len, float r, Random rnd) {
        float t = mb.currentTint();
        down = down.norm();
        V side = basis(down)[0];
        int n = 7;
        V[] path = new V[n + 1];
        float wob = (rnd.nextFloat() - 0.5f) * 0.4f;
        for (int i = 0; i <= n; i++) {
            float k = (float) i / n;
            path[i] = start.add(down.mul(len * k)).add(side.mul((float) Math.sin(k * 3.0) * wob));
        }
        mb.tint(0.62f).rough(0f, 1f).ao(0.4f).loft(path, r, r * 0.55f, 5, FLESH)
                .ellipsoid(path[n].add(down.mul(r * 0.6f)), r * 1.3f, r * 1.7f, r * 1.3f, 5, 7, FLESH, null)
                .tint(t);
    }

    /**
     * Guts spilling out of a wound and hanging down in loops: a thick, segmented, glistening coil of intestine
     * that bulges out first, then sags under its own weight and swings around, with a thinner loop beside it.
     */
    public static void entrails(MeshBuilder mb, V start, V out, float len, float r, Random rnd) {
        float t0 = mb.currentTint();
        V down = V.of(0, 1, 0), o = out.norm();
        V side = o.cross(down);
        side = side.len() < 1e-3 ? V.of(1, 0, 0) : side.norm();
        int mark = mb.bloodMark();
        mb.blood(start, r * 4.5f, true, 1f);
        for (int coil = 0; coil < 2; coil++) {
            float L = coil == 0 ? len : len * 0.6f, rr = coil == 0 ? r : r * 0.7f;
            double ph = rnd.nextDouble() * Math.PI * 2;
            int n = 30;
            V[] path = new V[n];
            for (int k = 0; k < n; k++) {
                float s = k / (n - 1f);
                float spill = (float) Math.sin(Math.min(1f, s * 2.4f) * Math.PI / 2) * L * 0.22f;
                float hang = L * (float) Math.pow(s, 1.25) * 0.72f;
                float swing = (float) Math.sin(s * Math.PI * 3.6 + ph) * L * 0.11f * (0.35f + s);
                float bulge = (float) Math.cos(s * Math.PI * 2.7 + ph) * L * 0.07f;
                path[k] = start.add(o.mul(spill + bulge)).add(down.mul(hang)).add(side.mul(swing + (coil == 0 ? 0 : rr * 2.2f)));
            }
            mb.tint(1.08f).rough(0.03f, 3f).ao(1.4f).loft(path, rr, rr * 0.85f, 10, FLESH);
            mb.tint(1.15f).rough(0.02f, 2f);
            for (int k = 1; k < n; k += 2) {                                       // segmented bulges, like real gut
                float b = rr * (1.08f + 0.12f * rnd.nextFloat());
                mb.ellipsoid(path[k], b, b, b, 7, 9, FLESH, null);
            }
            mb.tint(0.55f);
            for (int k = 4; k < n - 4; k += 7) {                                   // dark veins of the mesentery
                V a = path[k], b = path[k + 3].add(o.mul(-rr * 0.6f));
                mb.tube(a, b, rr * 0.18f, rr * 0.12f, FLESH, 5, 4, 0f, 1f, 1f, null, true, true);
            }
            mb.tint(1f);
            drip(mb, path[n - 1], V.of(0, 1, 0), 1.2f + rnd.nextFloat() * 1.6f, rr * 0.25f, rnd);
        }
        mb.bloodReset(mark).tint(t0).rough(0f, 1f);
    }

    /**
     * Skin torn away: striated red muscle fibres running across the patch, white tendon ends, and a ragged
     * curled lip of skin round the edge.
     */
    public static void flayed(MeshBuilder mb, V c, V n, V along, float r, Random rnd) {
        float t0 = mb.currentTint();
        V nn = n.norm(), al = along.sub(nn.mul(along.dot(nn))).norm();
        V across = nn.cross(al).norm();
        int mark = mb.bloodMark();
        mb.blood(c, r * 1.8f, true, 0.85f);
        mb.tint(0.35f).rough(0.06f, 2f).ao(2f)
                .ellipsoid(c.sub(nn.mul(r * 0.62f)), r * 0.66f, r * 0.66f, r * 0.66f, 8, 10, FLESH, null);   // dark under-layer
        int fibres = 11;
        for (int i = 0; i < fibres; i++) {
            float u = (i / (fibres - 1f) - 0.5f) * 1.7f;
            float half = r * (float) Math.sqrt(Math.max(0.05, 1 - u * u / 0.75f));
            V mid = c.add(across.mul(u * r)).add(nn.mul(r * 0.12f));
            V a = mid.sub(al.mul(half)), b = mid.add(al.mul(half));
            V m2 = V.lerp(a, b, 0.5f).add(nn.mul(r * 0.1f));
            mb.tint(0.85f + 0.3f * rnd.nextFloat()).rough(0.01f, 4f).ao(1.6f)
                    .loft(new V[]{a, m2, b}, r * 0.12f, r * 0.12f, 6, FLESH);
            mb.tint(1f).rough(0f, 1f);
            if (i % 3 == 1) mb.tube(b, b.add(al.mul(r * 0.3f)), r * 0.06f, r * 0.04f, BONE, 5, 3, 0f, 1f, 1f, null, true, true);   // tendon
        }
        mb.tint(0.9f).rough(0.06f, 2.5f).ao(1.4f)
                .loft(tornRing(c, basis(nn), nn, r * 1.05f, 0.12f, r * 0.06f, rnd, 20), r * 0.14f, r * 0.14f, 6, SKIN);
        mb.bloodReset(mark).tint(t0).rough(0f, 1f);
    }

}
