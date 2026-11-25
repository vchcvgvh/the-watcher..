package com.thewatcher.client.mesh;

import java.util.Random;

import static com.thewatcher.client.mesh.MeshBuilder.Swatch.BONE;
import static com.thewatcher.client.mesh.MeshBuilder.Swatch.CLOTH;
import static com.thewatcher.client.mesh.MeshBuilder.Swatch.FLESH;
import static com.thewatcher.client.mesh.MeshBuilder.Swatch.KERATIN;
import static com.thewatcher.client.mesh.MeshBuilder.Swatch.PALE;
import static com.thewatcher.client.mesh.MeshBuilder.bump;

/**
 * The Faceless: a tall figure in a tattered black robe, with a long pale neck and a smooth head that has
 * no face at all: just skin stretched over where the eyes should be. Its arms hang to its ankles.
 * It only moves when nobody is looking, and every time you look back it's frozen in a different pose.
 */
public final class FacelessSkeleton implements Rig {
    private static final float PI = (float) Math.PI;
    private static final float PALE_T = 1.0f, ROBE_T = 1.0f;

    public final Bone root, body, neck, head;
    public final Bone[] arm = new Bone[2], forearm = new Bone[2], hand = new Bone[2];
    public final Bone[][] finger = new Bone[2][4];

    @Override
    public Bone root() { return root; }

    public FacelessSkeleton() {
        Random rnd = new Random(0xFACE1E55);
        root = new Bone("root", 0, 0, 0, 0, 0, 0);

        // ---- robe: flares from the waist to a ragged hem that drags on the ground
        body = root.add(new Bone("body", 0, 2f, 0, 0, 0, 0));
        MeshBuilder rb = new MeshBuilder().tint(ROBE_T).rough(0.35f, 0.5f).ao(1.4f)
                .blood(V.of(0, 20.5, 0), V.of(7.5, 3.2, 7.5), false, 0.85f)          // hem soaked dark
                .blood(V.of(-2.5, 0, -2.0), V.of(1.6, 5.0, 1.2), false, 0.6f)        // where it wipes its hands
                .tube(V.of(0, 0, 0), V.of(0, 20.5, 0), 2.7f, 6.2f, CLOTH, 22, 10, -0.05f, 1f, 0.8f, V.of(1, 0, 0), false, false)
                .ellipsoid(V.of(0, -6.5, 0.1), 3.7f, 7.0f, 2.5f, 16, 22, CLOTH, null)          // robed chest
                .ellipsoid(V.of(-3.4, -11.5, 0.1), 1.8f, 1.5f, 1.8f, 8, 10, CLOTH, null)       // shoulders
                .ellipsoid(V.of(3.4, -11.5, 0.1), 1.8f, 1.5f, 1.8f, 8, 10, CLOTH, null)
                .tube(V.of(0, -13.0, 0), V.of(0, -12.0, 0), 1.4f, 2.6f, CLOTH, 14, 3, 0f, 1f, 0.8f, V.of(1, 0, 0), false, false); // collar
        rb.rough(0f, 1f);
        for (int i = 0; i < 30; i++) {                                                 // tatters
            double a = 2 * Math.PI * i / 30 + rnd.nextDouble() * 0.15;
            V top = V.of(Math.cos(a) * 6.0, 20.2, Math.sin(a) * 6.0 * 0.8);
            float len = 0.8f + rnd.nextFloat() * 1.3f;
            V[] strip = new V[4];
            for (int k = 0; k < 4; k++) {
                float t = k / 3f;
                strip[k] = top.add(V.of(Math.cos(a) * 0.35 * t, len * 1.6f * t, Math.sin(a) * 0.35 * t))
                        .add(V.of(Math.sin(a) * 0.25 * Math.sin(t * 3 + i), 0, -Math.cos(a) * 0.25 * Math.sin(t * 3 + i)));
            }
            float sh = 0.75f + 0.25f * rnd.nextFloat();
            rb.ribbon(strip, 1.3f + rnd.nextFloat() * 0.5f, 0.15f, V.of(0, top.y(), 0), CLOTH, 0.1f, 0.9f, sh, sh * 0.7f);   // torn cloth strips
        }
        // robe and chest torn open: a black hollow where its insides should be, ribs across it
        V hc = V.of(0, -7.2, -2.35);
        rb.blood(hc, V.of(2.6, 3.4, 1.6), true, 0.9f).blood(V.of(0, -3.0, -2.3), V.of(1.4, 3.6, 1.2), false, 0.8f)
          .tint(0.06f).rough(0f, 1f).ellipsoid(V.of(0, -7.2, -1.4), 1.75f, 2.7f, 1.1f, 12, 14, FLESH, null)
          .tint(0.6f).rough(0.08f, 2.5f).ao(1.8f);
        for (int k = 0; k < 6; k++) {                                                   // shreds of dried meat on the bone
            rb.ellipsoid(V.of((rnd.nextFloat() - 0.5f) * 2.6f, -9.4f + rnd.nextFloat() * 4.4f, -1.9f), 0.25f, 0.4f, 0.2f, 5, 6, FLESH, null);
        }
        rb.tint(1f).rough(0.03f, 2f).ao(1.4f);
        for (int k = 0; k < 5; k++) {                                                   // ribs bowing out over the hole
            float y = -9.2f + k * 0.95f, half = 1.75f - Math.abs(k - 2) * 0.12f;
            rb.loft(new V[]{V.of(-half, y + 0.2f, -1.85), V.of(0, y - 0.25f, -2.6), V.of(half, y + 0.2f, -1.85)}, 0.17f, 0.15f, 6, BONE);
        }
        rb.tube(V.of(0, -9.9, -2.5), V.of(0, -5.0, -2.45), 0.22f, 0.18f, BONE, 6, 5, 0f, 1f, 1f, null, true, true);   // sternum
        rb.tint(0.85f).rough(0.06f, 2f).ao(1.4f)
          .loft(Gore.tornRing(hc, Gore.basis(V.of(0, 0, -1)), V.of(0, 0, -1), 2.0f, 0.2f, 0.08f, rnd, 22), 0.24f, 0.24f, 6, CLOTH)
          .tint(ROBE_T).rough(0f, 1f);
        Gore.drip(rb, V.of(0.5, -5.0, -2.5), V.of(0, 1, 0), 3.5f, 0.12f, rnd);
        Gore.drip(rb, V.of(-0.9, -5.3, -2.4), V.of(0, 1, 0), 2.4f, 0.1f, rnd);
        body.mesh = rb.build();

        // ---- long pale neck
        neck = body.add(new Bone("neck", 0, -13f, -0.2f, 0.12f, 0, 0));
        neck.mesh = new MeshBuilder().tint(PALE_T).rough(0.07f, 1.2f).ao(1.4f)
                .tube(V.of(0, 0.5, 0), V.of(0, -9, -0.6), 0.95f, 0.75f, PALE, 12, 9, -0.12f, 1f, 1f, V.of(1, 0, 0), false, false)
                .tube(V.of(-0.55, -8, -0.3), V.of(-0.3, 0, -0.6), 0.22f, 0.25f, PALE, 6, 6, 0f, 1f, 1f, null, true, true)
                .tube(V.of(0.55, -8, -0.3), V.of(0.3, 0, -0.6), 0.22f, 0.25f, PALE, 6, 6, 0f, 1f, 1f, null, true, true)
                .build();

        // ---- the blank head: skin stretched tight over a skull, where a face should be.
        // Its eyes and mouth have been sewn shut; blood weeps through the stitches.
        head = neck.add(new Bone("head", 0, -9f, -0.6f, -0.12f, 0, 0));
        MeshBuilder hb = new MeshBuilder().tint(PALE_T).rough(0.04f, 1.4f).ao(2.0f)
                .blood(V.of(0.98, -2.6, -2.55), V.of(0.45, 2.4, 0.6), false, 1f)              // tears of old blood
                .blood(V.of(-0.98, -2.6, -2.55), V.of(0.45, 2.4, 0.6), false, 1f)
                .blood(V.of(1.0, -4.8, -2.6), 0.6f, true, 0.9f)                                // fresh at the sewn eyes
                .blood(V.of(-1.0, -4.8, -2.6), 0.6f, true, 0.9f)
                .blood(V.of(0, -2.1, -2.75), V.of(1.4, 0.55, 0.6), true, 0.85f)               // the sewn mouth seeps
                .blood(V.of(0, -0.6, -2.4), V.of(0.7, 1.8, 0.6), false, 0.8f)                  // ...down the chin
                .ellipsoid(HEAD_C, HEAD_R.x(), HEAD_R.y(), HEAD_R.z(), 34, 44, PALE, FacelessSkeleton::face);
        hb.rough(0f, 1f).ao(0.6f).tint(0.3f);
        stitches(hb, V.of(0, 0.45, -0.89), 1.15f, 7, 0.32f);                                   // mouth
        stitches(hb, V.of(0.4, -0.12, -0.91), 0.55f, 4, 0.22f);                                // eyes
        stitches(hb, V.of(-0.4, -0.12, -0.91), 0.55f, 4, 0.22f);
        hb.tint(PALE_T);
        head.mesh = hb.build();

        // ---- arms: robe sleeves, then very long pale forearms and hands
        for (int s = 0; s < 2; s++) {
            float sx = s == 0 ? -1f : 1f;
            arm[s] = body.add(new Bone("arm" + s, sx * 3.9f, -11.3f, 0.1f, -0.05f, 0, -sx * 0.06f));
            arm[s].mesh = new MeshBuilder().tint(ROBE_T).rough(0.25f, 0.7f).ao(1.3f)
                    .tube(V.of(0, 0, 0), V.of(0, 8.5, 0), 1.4f, 2.1f, CLOTH, 12, 6, 0f, 1f, 1f, null, false, false)
                    .tint(PALE_T).rough(0.05f, 1.2f)
                    .tube(V.of(0, 6.5, 0), V.of(0, 8.8, 0), 0.8f, 0.8f, PALE, 8, 3, 0f, 1f, 1f, null, false, false)
                    .build();
            forearm[s] = arm[s].add(new Bone("forearm" + s, 0, 8.6f, 0, -0.05f, 0, 0));
            forearm[s].mesh = new MeshBuilder().tint(PALE_T).rough(0.06f, 1.1f).ao(1.3f)
                    .blood(V.of(0, 15.5, 0), V.of(1.2, 5.0, 1.2), true, 0.75f)             // bloody to the elbow
                    .tube(V.of(0, 0, 0), V.of(0, 15.5, 0), 0.8f, 0.5f, PALE, 10, 9, 0.06f, 1.2f, 0.85f, V.of(1, 0, 0), false, false)
                    .ellipsoid(V.of(0, 15.5, 0), 0.62f, 0.5f, 0.55f, PALE)
                    .build();
            hand[s] = forearm[s].add(new Bone("hand" + s, 0, 15.5f, 0, 0, 0, 0));
            MeshBuilder hm = new MeshBuilder().tint(PALE_T).ao(1.2f)
                    .blood(V.of(0, 3.2, -0.3), V.of(1.6, 1.6, 1.0), true, 0.7f)
                    .tint(s == 0 ? 0.7f : 1f)
                    .ellipsoid(V.of(0, 1.5, 0), s == 0 ? 0.8f : 0.95f, 1.6f, s == 0 ? 0.3f : 0.38f, 10, 14, s == 0 ? FLESH : PALE, d -> {
                        float m = 1f - 0.12f * bump(d, V.of(0, 0, -1), 0.3f);           // hollow palm
                        return V.of(1f, 1f, m);
                    })
                    .rough(0.03f, 2f);
            for (int f = 0; f < 4; f++) {                                                   // tendons and knuckles
                float x = -0.72f + f * 0.48f;
                hm.tint(1f).tube(V.of(x * 0.45f, 0.2, 0.3), V.of(x, 2.8, 0.3), 0.13f, 0.15f, s == 0 ? BONE : PALE, 6, 4, 0f, 1f, 1f, null, true, true)
                  .ellipsoid(V.of(x, 2.85, 0.15), 0.27f, 0.24f, 0.27f, 6, 8, s == 0 ? BONE : PALE, null);
            }
            hand[s].mesh = hm.build();
            for (int f = 0; f < 4; f++) {
                float x = -0.72f + f * 0.48f;
                float L = new float[]{0.9f, 1.05f, 1.0f, 0.85f}[f];
                finger[s][f] = hand[s].add(new Bone("finger" + s + f, x, 2.9f, 0, -0.1f, 0, (f - 1.5f) * 0.05f));
                finger[s][f].mesh = new MeshBuilder().tint(PALE_T).ao(1.2f)
                        .blood(V.of(0, 4, 0), 4.5f, true, 0.85f)
                        .tube(V.of(0, 0, 0), V.of(0, 5.5f * L, 0), s == 0 ? 0.13f : 0.24f, s == 0 ? 0.1f : 0.16f, s == 0 ? BONE : PALE, 6, 5, 0f, 1f, 1f, null, false, false)
                        .ellipsoid(V.of(0, 2.7f * L, 0), s == 0 ? 0.2f : 0.26f, s == 0 ? 0.24f : 0.3f, s == 0 ? 0.2f : 0.26f, 5, 7, s == 0 ? BONE : PALE, null)
                        .tint(1f).cone(V.of(0, 5.3f * L, 0), V.of(0, 5.3f * L + 1.4f, -0.4f), 0.17f, KERATIN)
                        .build();
            }
        }
    }

    private static final V HEAD_C = V.of(0, -4.4, 0.1), HEAD_R = V.of(2.6, 4.6, 3.0);

    /** Skull under stretched skin: brow ridge, deep eye pits, cheekbones over hollow cheeks, temples, a chin. */
    private static V face(V d) {
        float mx = 1f - 0.25f * Math.max(0f, d.y());
        float r = 1f
                - 0.16f * bump(d, V.of(0.4, -0.12, -0.91).norm(), 0.045f)      // sunken pits where eyes were
                - 0.16f * bump(d, V.of(-0.4, -0.12, -0.91).norm(), 0.045f)
                + 0.07f * bump(d, V.of(0, -0.3, -0.95).norm(), 0.05f)          // heavy brow
                + 0.08f * bump(d, V.of(0.62, 0.12, -0.78).norm(), 0.03f)       // cheekbones...
                + 0.08f * bump(d, V.of(-0.62, 0.12, -0.78).norm(), 0.03f)
                - 0.11f * bump(d, V.of(0.6, 0.38, -0.7).norm(), 0.05f)         // ...over starved, hollow cheeks
                - 0.11f * bump(d, V.of(-0.6, 0.38, -0.7).norm(), 0.05f)
                - 0.07f * bump(d, V.of(0.9, -0.3, -0.3).norm(), 0.06f)         // sunken temples
                - 0.07f * bump(d, V.of(-0.9, -0.3, -0.3).norm(), 0.06f)
                + 0.05f * bump(d, V.of(0, 0.1, -1).norm(), 0.02f)              // the ghost of a nose
                - 0.05f * bump(d, V.of(0, 0.45, -0.89).norm(), 0.008f)         // the mouth seam, pulled in
                + 0.06f * bump(d, V.of(0, 0.68, -0.73).norm(), 0.04f);         // chin
        return V.of(mx * r, r, r);
    }

    private static V onFace(V d, float lift) {
        d = d.norm();
        V m = face(d);
        V p = HEAD_C.add(V.of(d.x() * HEAD_R.x() * m.x(), d.y() * HEAD_R.y() * m.y(), d.z() * HEAD_R.z() * m.z()));
        return p.add(V.of(d.x() / HEAD_R.x(), d.y() / HEAD_R.y(), d.z() / HEAD_R.z()).norm().mul(lift));
    }

    /** A row of crude stitches across a seam centred on direction {@code c}: thick dark thread pulling skin shut. */
    private static void stitches(MeshBuilder mb, V c, float halfWidth, int n, float height) {
        c = c.norm();
        V side = V.of(1, 0, 0), up = V.of(0, 1, 0);
        for (int i = 0; i < n; i++) {
            float x = -halfWidth + 2f * halfWidth * i / (n - 1);
            float tilt = (i % 2 == 0 ? 0.08f : -0.06f);
            V a = c.add(side.mul(x / HEAD_R.x())).add(up.mul((-height + tilt) / HEAD_R.y()));
            V m = c.add(side.mul((x + tilt) / HEAD_R.x()));
            V b = c.add(side.mul((x + 2 * tilt) / HEAD_R.x())).add(up.mul((height + tilt) / HEAD_R.y()));
            mb.loft(new V[]{onFace(a, -0.02f), onFace(m, 0.09f), onFace(b, -0.02f)}, 0.065f, 0.065f, 5, KERATIN);
        }
        // the thread running along the seam between stitches
        V[] seam = new V[n * 2];
        for (int i = 0; i < seam.length; i++) {
            float x = -halfWidth * 1.05f + 2.1f * halfWidth * i / (seam.length - 1);
            seam[i] = onFace(c.add(side.mul(x / HEAD_R.x())), 0.03f);
        }
        mb.loft(seam, 0.035f, 0.035f, 4, KERATIN);
    }

    private static float sin(float a) { return (float) Math.sin(a); }

    public void pose(float limbDistance, float age, float headYawDeg, float headPitchDeg, boolean frozen, int poseId) {
        pose(limbDistance, age, headYawDeg, headPitchDeg, frozen, poseId, 0, new AnimState());
    }

    /**
     * Frozen (someone is looking): absolutely still, not even breathing, in one of six poses.
     * Moving (nobody is looking... unless you catch it from the corner of your eye, or a friend does):
     * it glides, robe swaying, arms reaching out ahead with loose lagging hands, and its blank head
     * jerks around in quick, insect-like twitches, fingers flexing constantly.
     */
    public void pose(float limbDistance, float age, float headYawDeg, float headPitchDeg, boolean frozen, int poseId,
                     int id, AnimState st) {
        st.tick(age);
        root.reset();
        head.yaw += headYawDeg * (PI / 180f);
        head.pitch += headPitchDeg * (PI / 180f) * 0.5f;

        if (!frozen) {
            float sway = sin(age * 0.15f);
            body.roll += sway * 0.04f;
            body.pitch += 0.15f + 0.05f * Math.min(1f, limbDistance) + sin(age * 0.3f) * 0.02f;
            body.oy += sin(age * 0.2f) * 0.3f;
            neck.pitch += 0.35f;
            // insect-like head: new random angle every few ticks, snapped to with a stiff spring
            int b = (int) ((age + id * 7) / 3.5f);
            head.yaw += st.spring(0, (AnimState.hash(b, 1) - 0.5f) * 1.0f, 3.0f, 2.2f);
            head.roll += st.spring(1, 0.3f + (AnimState.hash(b, 2) - 0.5f) * 0.9f, 3.0f, 2.2f);
            head.pitch += st.spring(2, (AnimState.hash(b, 3) - 0.5f) * 0.4f, 3.0f, 2.2f);
            for (int s = 0; s < 2; s++) {
                float reach = -0.9f + sin(age * 0.2f + s) * 0.1f - sway * 0.08f * (s == 0 ? 1 : -1);
                float a = st.spring(3 + s, reach, 0.3f, 0.45f);
                arm[s].pitch += a;
                forearm[s].pitch += st.spring(5 + s, -0.4f + (a - reach) * 1.5f, 0.2f, 0.35f);  // lagging
                hand[s].pitch += st.spring(7 + s, (a - reach) * 1.2f, 0.18f, 0.3f);
                for (int f = 0; f < 4; f++)
                    finger[s][f].pitch -= 0.2f + 0.35f * (0.5f + 0.5f * sin(age * 0.9f + f * 1.7f + s));
            }
            head.roll += 0.3f;
            return;
        }

        switch (Math.floorMod(poseId, 6)) {
            case 0 -> head.roll += 0.55f;                                        // just standing there
            case 1 -> {                                                          // one hand reaching for you
                arm[0].pitch -= 1.5f;
                forearm[0].pitch -= 0.1f;
                head.roll -= 0.35f;
            }
            case 2 -> {                                                          // both arms reaching
                arm[0].pitch -= 1.35f;
                arm[1].pitch -= 1.45f;
                neck.pitch += 0.4f;
            }
            case 3 -> {                                                          // head bent nearly sideways
                head.roll += 1.35f;
                neck.roll += 0.35f;
                arm[1].pitch -= 2.7f;
                forearm[1].pitch -= 1.2f;
            }
            case 4 -> {                                                          // crouched, arms spread
                body.pitch += 0.45f;
                arm[0].roll += 0.9f;
                arm[1].roll -= 0.9f;
                head.pitch -= 0.4f;
            }
            default -> {                                                         // head turned too far
                head.yaw += 1.4f;
                arm[0].pitch -= 0.4f;
            }
        }
        for (int s = 0; s < 2; s++)
            for (int f = 0; f < 4; f++)
                finger[s][f].pitch -= 0.25f + 0.1f * f;                          // fingers half-curled
    }
}
