package com.thewatcher.client.mesh;

import java.util.Random;

import static com.thewatcher.client.mesh.MeshBuilder.Swatch.BONE;
import static com.thewatcher.client.mesh.MeshBuilder.Swatch.EYE;
import static com.thewatcher.client.mesh.MeshBuilder.Swatch.FLESH;
import static com.thewatcher.client.mesh.MeshBuilder.Swatch.HAIR;
import static com.thewatcher.client.mesh.MeshBuilder.Swatch.HIDE;
import static com.thewatcher.client.mesh.MeshBuilder.Swatch.SKIN;
import static com.thewatcher.client.mesh.MeshBuilder.Swatch.KERATIN;
import static com.thewatcher.client.mesh.MeshBuilder.bump;

/**
 * The Hollow Hound: a starved, mangy wolf-thing, the size of a large dog.
 *
 *  - deep, narrow chest with every rib showing; a waist tucked up to almost nothing; bony shoulder
 *    blades and hip bones; heavy, ropy hindquarters
 *  - real canine legs: shoulder, elbow, long forearm, pastern; thigh, hock, long rear cannon bone;
 *    paws with knuckled toes, black claws and dewclaws
 *  - wolf skull with a long muzzle and wet nose, torn ears (one bitten half off), small glowing eyes
 *    sunk in deep sockets; lips peeled back in a permanent snarl over raw gums, big canines,
 *    and a row of flat, human front teeth; strings of drool
 *  - patches of matted, mangy fur; hackles bristling along the neck and spine; open sores
 *
 * Units: pixels (16 = 1 block), y = 24 is the ground, +y is down, -z is the front.
 */
public final class HoundSkeleton implements Rig {
    private static final float PI = (float) Math.PI;

    public final Bone root, body, neck, head, jaw, tail;
    /** leg[i][segment]: 0 front-right, 1 front-left, 2 back-right, 3 back-left. */
    public final Bone[][] leg = new Bone[4][];
    public final Bone[] earL = new Bone[1], earR = new Bone[1];

    @Override
    public Bone root() { return root; }

    // ---- body proportions
    private static final V CHEST_C = V.of(0, 1.1, -3.0);
    private static final V CHEST_R = V.of(2.35, 3.7, 4.7);

    /** Approximate height of the top of the back at body-length z (for spine knobs and fur). */
    private static float backTop(float z) {
        if (z < 1.2f) {
            float t = (z - CHEST_C.z()) / CHEST_R.z();
            return CHEST_C.y() - CHEST_R.y() * (float) Math.sqrt(Math.max(0.12, 1 - t * t));
        }
        if (z < 6.4f) return -1.5f + (z - 1.2f) * 0.02f;
        return -1.6f - (float) Math.sin((z - 6.4f) / 3.0f * PI) * 0.7f;   // the rise over the hips
    }

    public HoundSkeleton() {
        Random rnd = new Random(0x40C0D2);
        root = new Bone("root", 0, 0, 0, 0, 0, 0);
        body = root.add(new Bone("body", 0, 8f, 0, 0, 0, 0));

        // ================================================================ torso
        MeshBuilder b = new MeshBuilder().rough(0.12f, 0.9f).ao(1.7f)
                .blood(V.of(2.2, 0.9, -2.0), 2.4f, true, 0.85f)                       // around the torn flank
                .blood(V.of(2.2, 3.4, -2.0), V.of(1.2, 2.6, 2.0), false, 0.8f)       // ...dripping down its belly
                .blood(V.of(0, -3.4, -1.0), V.of(1.0, 1.0, 4.5), false, 0.55f)       // along the spine where bone shows
                // deep, narrow ribcage with a keel underneath and every rib showing
                .ellipsoid(CHEST_C, CHEST_R.x(), CHEST_R.y(), CHEST_R.z(), 30, 34, HIDE, d -> {
                    float r = 1f;
                    if (d.y() > -0.6f && d.z() > -0.75f && d.z() < 0.85f) {
                        float t = (d.z() + 0.75f) / 1.6f;
                        float rib = (float) Math.pow(Math.max(0, Math.sin(t * PI * 7.5f)), 3);
                        r += 0.1f * rib * Math.min(1f, (d.y() + 0.6f) * 1.6f);
                    }
                    if (d.y() < -0.2f) r -= 0.06f * (float) Math.exp(-(d.x() / 0.12f) * (d.x() / 0.12f)); // spine groove
                    float mx = 1f - 0.25f * Math.max(0f, d.y());                                   // narrow keel
                    float my = 1f + 0.08f * Math.max(0f, d.y());
                    return V.of(mx * r, my * r, r);
                })
                // tucked waist, up under the spine
                .tube(V.of(0, -0.2, 1.0), V.of(0, -0.9, 6.6), 1.85f, 1.75f, HIDE, 16, 8, -0.38f, 0.85f, 1.15f, V.of(1, 0, 0), false, false)
                // pelvis and heavy hindquarters
                .ellipsoid(V.of(0, -0.6, 7.4), 2.1f, 1.9f, 2.2f, 14, 18, HIDE, d -> {
                    float r = 1f + 0.25f * bump(d, V.of(0.7, -0.65, -0.3).norm(), 0.05f)      // jutting hip bones
                            + 0.25f * bump(d, V.of(-0.7, -0.65, -0.3).norm(), 0.05f)
                            + 0.12f * bump(d, V.of(0, -0.4, 0.9).norm(), 0.06f);          // tail root
                    return V.of(r, r, r);
                })
                .rough(0.16f, 0.8f)
                .ellipsoid(V.of(-1.75, 0.4, 7.9), 1.25f, 2.6f, 2.3f, 12, 14, HIDE, null)       // haunches
                .ellipsoid(V.of(1.75, 0.4, 7.9), 1.25f, 2.6f, 2.3f, 12, 14, HIDE, null)
                .rough(0.1f, 1f)
                .ellipsoid(V.of(-1.55, -1.4, -5.0), 0.75f, 2.4f, 1.6f, 10, 12, HIDE, null)     // shoulder blades
                .ellipsoid(V.of(1.55, -1.4, -5.0), 0.75f, 2.4f, 1.6f, 10, 12, HIDE, null)
                .ellipsoid(V.of(-1.9, 1.3, -5.6), 1.0f, 1.6f, 1.3f, 8, 10, HIDE, null)         // shoulder muscle
                .ellipsoid(V.of(1.9, 1.3, -5.6), 1.0f, 1.6f, 1.3f, 8, 10, HIDE, null);
        for (int k = 0; k < 17; k++) {                                                    // spine ridge
            float z = -7.2f + k * 1.0f;
            b.ellipsoid(V.of(0, backTop(z) - 0.05f, z), 0.42f, 0.5f, 0.38f, 5, 8, HIDE, null);
        }
        // open sores
        b.rough(0.25f, 1.8f).ao(2f)
                .ellipsoid(V.of(-2.05, 1.6, -1.2), 0.32f, 1.1f, 1.3f, 8, 12, FLESH, null)
                .rough(0f, 1f).ao(1f);
        // a great torn hole in its flank: you can see the ribs
        Gore.wound(b, V.of(2.25, 0.9, -2.0), V.of(1, 0.1, 0), 1.5f, rnd);
        for (int k = 0; k < 3; k++) {
            float z = -3.1f + k * 1.05f;
            b.tint(1f).rough(0.04f, 2f).tube(V.of(2.35, -0.4, z), V.of(2.65, 2.2, z + 0.25f), 0.22f, 0.18f, BONE, 6, 5, 0.1f, 1f, 1f, null, true, true);
        }
        Gore.drip(b, V.of(2.4, 2.3, -2.2), V.of(0, 1, 0), 3.0f, 0.12f, rnd);
        Gore.drip(b, V.of(2.3, 2.1, -1.4), V.of(0, 1, 0), 2.0f, 0.1f, rnd);
        // spine breaking through the skin
        for (int k = 0; k < 3; k++) {
            float z = -4.2f + k * 2.0f;
            Gore.exposedBone(b, V.of(0, backTop(z) - 0.25f, z), V.of(0, -1, 0), 0.45f, rnd);
        }
        // mangy fur: matted patches on the shoulders, flanks and haunches; hackles up along the spine
        for (int i = 0; i < 90; i++) {
            float z = -7.4f + rnd.nextFloat() * 16.5f;
            boolean spine = rnd.nextFloat() < 0.55f;
            float side = rnd.nextBoolean() ? 1f : -1f;
            if (!spine && rnd.nextFloat() < 0.35f) continue;            // bald patches
            V root;
            V out;
            if (spine) {
                root = V.of((rnd.nextFloat() - 0.5f) * 0.9f, backTop(z) + 0.1f, z);
                out = V.of((rnd.nextFloat() - 0.5f) * 0.5f, -1f, 0.35f + rnd.nextFloat() * 0.3f).norm(); // bristling
            } else {
                float y = backTop(z) + 0.8f + rnd.nextFloat() * 1.6f;
                float x = side * (z < 1.2f ? 2.1f : z < 6.4f ? 1.45f : 2.4f);
                root = V.of(x, y, z);
                out = V.of(side * 0.6f, 0.45f, 0.65f).norm();
            }
            float len = spine ? 1.4f + rnd.nextFloat() * 1.8f : 1.0f + rnd.nextFloat() * 1.2f;
            V[] path = new V[4];
            V dir = out;
            path[0] = root;
            for (int k = 1; k < 4; k++) {
                dir = dir.add(V.of(0, 0.18f, 0.12f)).norm();             // droop backwards
                path[k] = path[k - 1].add(dir.mul(len / 3));
            }
            float u0 = rnd.nextFloat() * 0.6f;
            b.tint(0.85f).ribbon(path, 0.5f + rnd.nextFloat() * 0.4f, 0.12f, root.sub(out.mul(2f)), HAIR, u0, u0 + 0.35f, 0.7f, 1f).tint(1f);
        }
        Gore.wound(b, V.of(0.5, 4.45, -1.8), V.of(0.2, 1, 0), 1.15f, rnd);               // belly ripped open...
        Gore.entrails(b, V.of(0.55, 4.3, -1.7), V.of(0.25, 1, 0.1), 8f, 0.42f, rnd);       // ...guts dragging under it
        body.mesh = b.build();

        // ================================================================ neck (thick, low, hackles up)
        neck = body.add(new Bone("neck", 0, -0.9f, -6.9f, 0, 0, 0));
        MeshBuilder nk = new MeshBuilder().rough(0.12f, 0.9f).ao(1.5f)
                .tube(V.of(0, 0.6, 1.6), V.of(0, -1.7, -3.4), 2.0f, 1.35f, HIDE, 14, 7, 0.06f, 0.9f, 1.1f, V.of(1, 0, 0), false, false)
                .tube(V.of(0, 1.8, 0.8), V.of(0, 0.2, -3.0), 0.55f, 0.5f, HIDE, 7, 6, 0f, 1f, 1f, null, true, true)     // windpipe
                .tube(V.of(-1.0, -0.4, 0.6), V.of(-0.6, -1.0, -3.2), 0.4f, 0.35f, HIDE, 6, 6, 0f, 1f, 1f, null, true, true) // neck sinews
                .tube(V.of(1.0, -0.4, 0.6), V.of(0.6, -1.0, -3.2), 0.4f, 0.35f, HIDE, 6, 6, 0f, 1f, 1f, null, true, true);
        for (int i = 0; i < 34; i++) {                                                   // hackles
            float t = rnd.nextFloat();
            V root = V.lerp(V.of(0, -1.2, 1.4), V.of(0, -2.8, -3.0), t).add(V.of((rnd.nextFloat() - 0.5f) * 1.6f, 0, 0));
            V out = V.of(root.x() * 0.5f, -1f, 0.5f).norm();
            V[] path = {root, root.add(out.mul(0.8f)), root.add(out.mul(1.5f)).add(V.of(0, 0.15, 0.4)), root.add(out.mul(2.1f)).add(V.of(0, 0.4, 0.9))};
            float u0 = rnd.nextFloat() * 0.6f;
            nk.tint(0.85f).ribbon(path, 0.55f, 0.12f, root.add(V.of(0, 2, 0)), HAIR, u0, u0 + 0.35f, 0.7f, 1f).tint(1f);
        }
        neck.mesh = nk.build();

        // ================================================================ head: wolf skull, snarling
        head = neck.add(new Bone("head", 0, -1.7f, -3.4f, 0.12f, 0, 0));
        V cr = V.of(0, -0.7, -1.4);
        V eyeL = V.of(0.95, -0.85, -3.0), eyeR = V.of(-0.95, -0.85, -3.0);
        MeshBuilder hd = new MeshBuilder().rough(0.07f, 1.2f).ao(2.0f)
                .blood(V.of(0, 0.5, -5.0), V.of(1.8, 1.4, 3.2), true, 0.85f)       // muzzle soaked
                .blood(V.of(0.4, -1.8, -1.3), 1.8f, true, 0.6f)                      // around the bare skull
                .blood(V.of(1.3, 0.2, -1.6), V.of(0.6, 1.8, 0.8), false, 0.7f)       // run down the cheek
                // cranium with sagittal crest, deep sockets, cheek arches
                .ellipsoid(cr, 1.95f, 1.75f, 2.4f, 26, 32, HIDE, d -> {
                    float r = 1f
                            - 0.3f * bump(d, V.of(0.55, -0.15, -0.82).norm(), 0.05f)       // eye sockets
                            - 0.3f * bump(d, V.of(-0.55, -0.15, -0.82).norm(), 0.05f)
                            + 0.12f * (float) Math.exp(-(d.x() / 0.1f) * (d.x() / 0.1f)) * Math.max(0f, -d.y()) // crest
                            + 0.1f * bump(d, V.of(0.85, 0.25, -0.45).norm(), 0.05f)        // zygomatic arches
                            + 0.1f * bump(d, V.of(-0.85, 0.25, -0.45).norm(), 0.05f)
                            - 0.08f * bump(d, V.of(0.9, -0.35, 0.2).norm(), 0.06f)         // temples
                            - 0.08f * bump(d, V.of(-0.9, -0.35, 0.2).norm(), 0.06f);
                    float mz = 1f + 0.12f * Math.max(0f, d.z()) * Math.max(0f, -d.y());   // occiput
                    return V.of(r, r, mz * r);
                })
                // muzzle: tapers to the nose, flat on top
                .ellipsoid(V.of(0, -0.05, -4.9), 1.2f, 0.95f, 2.75f, 20, 26, HIDE, d -> {
                    float mx = 1f - 0.38f * Math.max(0f, -d.z());                 // narrows to the nose
                    float my = 1f - 0.3f * Math.max(0f, -d.z()) * Math.max(0f, -d.y()); // flat bridge
                    float r = 1f - 0.05f * bump(d, V.of(0.75, 0.2, -0.6).norm(), 0.06f)
                            - 0.05f * bump(d, V.of(-0.75, 0.2, -0.6).norm(), 0.06f); // hollow under the eyes
                    return V.of(mx * r, my * r, r);
                })
                .tint(0.35f).rough(0.02f, 3f)
                .ellipsoid(V.of(0, -0.25, -7.55), 0.62f, 0.48f, 0.42f, 10, 12, SKIN, d -> {      // wet black nose
                    float r = 1f - 0.18f * bump(d, V.of(0.45, 0.1, -0.88).norm(), 0.04f) - 0.18f * bump(d, V.of(-0.45, 0.1, -0.88).norm(), 0.04f);
                    return V.of(r, r, r);
                })
                .tint(1f).rough(0.14f, 2f).ao(1.6f)
                .rough(0f, 1f).ao(0.7f);
        // lips peeled back: scalloped raw gums along both sides, the lip curled up and wrinkled above them
        for (int s = -1; s <= 1; s += 2) {
            lipLine(hd, V.of(s * 0.98, 0.5, -2.7), V.of(s * 0.5, 0.55, -7.15), 11, 0.36f, 0.26f, FLESH, 1f, 0.12f);
            lipLine(hd, V.of(s * 1.12, 0.12, -2.5), V.of(s * 0.66, 0.1, -6.7), 10, 0.24f, 0.17f, HIDE, 0.85f, 0.08f);
            for (int w = 0; w < 4; w++) {                                           // snarl wrinkles on the muzzle
                float z = -3.6f - w * 0.75f;
                hd.rough(0.03f, 2f).ao(1.6f).tube(V.of(s * 0.95, -0.55, z), V.of(s * 0.75, -0.95, z - 0.4f), 0.12f, 0.08f, HIDE, 5, 3, 0f, 1f, 1f, null, true, true);
            }
        }
        hd.rough(0f, 1f).ao(0.7f);
        // eyes: small, glowing, deep in the sockets
        hd.ellipsoid(eyeL, 0.36f, 0.36f, 0.36f, 12, 16, EYE, null);
        hd.ellipsoid(eyeR, 0.36f, 0.36f, 0.36f, 12, 16, EYE, null);
        // teeth: big canines, side teeth, and flat human incisors at the front
        for (int s = -1; s <= 1; s += 2) {
            hd.cone(V.of(s * 0.72, 0.7, -6.2), V.of(s * 0.62, 2.35, -6.0), 0.27f, BONE);           // canines
            for (int i = 0; i < 6; i++) {
                float t = i / 5f;
                float z = -2.9f - t * 2.9f, x = s * (1.0f - t * 0.25f);
                float len = 0.55f + rnd.nextFloat() * 0.45f + (i == 0 ? 0.3f : 0f);
                hd.cone(V.of(x, 0.7, z), V.of(x * 0.97f, 0.7f + len, z - 0.15f), 0.2f, BONE);
            }
        }
        for (int i = 0; i < 6; i++) {
            float x = -0.6f + i * 0.24f;
            hd.ellipsoid(V.of(x, 0.95, -7.05 + Math.abs(x) * 0.6), 0.12f, 0.36f, 0.1f, 5, 7, BONE, null);
        }
        Gore.exposedBone(hd, V.of(0.35, -2.25, -1.3), V.of(0.15, -1, 0), 0.85f, rnd);   // scalp torn off: bare skull
        // half its muzzle skinned: bare muscle, and the teeth showing right down to the roots
        Gore.flayed(hd, V.of(1.12, 0.05, -4.6), V.of(1, 0.15, 0), V.of(0, 0, 1), 0.95f, rnd);
        hd.rough(0.02f, 2f).ao(1.2f);
        for (int k = 0; k < 5; k++) {
            float z = -3.7f - 0.55f * k;
            hd.tint(0.7f).ellipsoid(V.of(1.0, 0.55, z), 0.2f, 0.16f, 0.22f, 5, 7, FLESH, null)        // raw gum
              .tint(1f).cone(V.of(1.02, 0.35, z), V.of(1.08, 1.05, z - 0.05f), 0.13f, BONE);           // root to tip
        }
        hd.rough(0f, 1f);
        head.mesh = hd.build();

        // ears: torn, one bitten half off
        for (int s = 0; s < 2; s++) {
            float sx = s == 0 ? -1f : 1f;
            Bone ear = head.add(new Bone(s == 0 ? "earR" : "earL", sx * 1.15f, -2.05f, -0.9f, 0.35f, 0, -sx * 0.35f));
            float len = s == 0 ? 1.3f : 2.4f;
            MeshBuilder em = new MeshBuilder().rough(0.08f, 2f).ao(1.6f)
                    .tube(V.of(0, 0.4, 0), V.of(0, -len, 0.2), 0.78f, s == 0 ? 0.42f : 0.06f, HIDE, 8, 5, 0f, 0.35f, 1f, V.of(1, 0, 0), true, true);
            if (s == 0) em.ellipsoid(V.of(0, -len, 0.2), 0.26f, 0.2f, 0.42f, 6, 8, FLESH, null);   // the bitten edge
            ear.mesh = em.build();
            if (s == 0) earR[0] = ear; else earL[0] = ear;
        }

        // ================================================================ lower jaw
        jaw = head.add(new Bone("jaw", 0, 0.75f, -1.6f, 0.1f, 0, 0));
        MeshBuilder jw = new MeshBuilder().rough(0.08f, 1.3f).ao(1.5f)
                .blood(V.of(0, 0.4, -3.2), V.of(1.6, 1.3, 3.4), true, 0.8f)
                .ellipsoid(V.of(0, 0.72, -3.0), 1.0f, 0.6f, 2.95f, 16, 22, HIDE, d -> {
                    float mx = 1f - 0.42f * Math.max(0f, -d.z());
                    float r = 1f + 0.08f * bump(d, V.of(0, 0.6, -0.8).norm(), 0.05f);   // chin
                    return V.of(mx * r, r, r);
                })
                .rough(0.14f, 2f);
        for (int s = -1; s <= 1; s += 2) lipLine(jw, V.of(s * 0.72, 0.18, -1.0), V.of(s * 0.38, 0.28, -5.6), 10, 0.3f, 0.2f, FLESH, 1f, 0.1f);
        jw.ellipsoid(V.of(0, 0.2, -3.0), 0.75f, 0.28f, 2.6f, 6, 12, FLESH, null)                  // tongue
                .rough(0f, 1f).ao(0.7f);
        for (int s = -1; s <= 1; s += 2) {
            jw.cone(V.of(s * 0.55, 0.1, -4.7), V.of(s * 0.5, -1.35, -4.85), 0.24f, BONE);             // lower canines
            for (int i = 0; i < 5; i++) {
                float t = i / 4f;
                float z = -1.2f - t * 3.0f, x = s * (0.78f - t * 0.25f);
                jw.cone(V.of(x, 0.15, z), V.of(x, 0.15f - 0.5f - rnd.nextFloat() * 0.4f, z), 0.17f, BONE);
            }
        }
        for (int i = 0; i < 6; i++) {
            float x = -0.55f + i * 0.22f;
            jw.ellipsoid(V.of(x, -0.15, -5.55 + Math.abs(x) * 0.6), 0.11f, 0.32f, 0.09f, 5, 7, BONE, null);
        }
        // strings of drool
        for (int k = 0; k < 3; k++) {
            float x = (k - 1) * 0.55f + (rnd.nextFloat() - 0.5f) * 0.2f;
            float zz = -2.2f - k * 1.3f;
            float len = 1.4f + rnd.nextFloat() * 2.2f;
            V[] path = new V[8];
            for (int i = 0; i < 8; i++) {
                float t = i / 7f;
                path[i] = V.of(x + (float) Math.sin(t * 3) * 0.12f, 1.15f + t * len, zz + t * 0.25f + (float) Math.sin(t * PI) * 0.2f);
            }
            jw.tint(0.62f).ao(0.3f).loft(path, 0.065f, 0.04f, 5, FLESH)                   // bloody drool
                    .ellipsoid(path[7], 0.13f, 0.17f, 0.13f, 5, 6, FLESH, null).tint(1f);
        }
        jaw.mesh = jw.build();

        // ================================================================ legs
        for (int i = 0; i < 4; i++) {
            boolean front = i < 2;
            float sx = (i % 2 == 0) ? -1f : 1f;
            V start;
            V[] dirs;
            float[] lens, radii, bulges;
            if (front) {
                start = V.of(sx * 1.85f, 1.6f, -5.2f);
                dirs = new V[]{V.of(sx * 0.04, 1, 0.32), V.of(0, 1, -0.1), V.of(0, 0.88, -0.46)};
                lens = new float[]{5.0f, 6.2f, 2.3f};
                radii = new float[]{1.35f, 0.72f, 0.44f, 0.4f};
                bulges = new float[]{0.18f, 0.16f, 0.0f};
            } else {
                start = V.of(sx * 1.75f, 1.2f, 7.6f);
                dirs = new V[]{V.of(sx * 0.04, 0.8, -0.6), V.of(0, 0.72, 0.69), V.of(0, 1, -0.08)};
                lens = new float[]{5.4f, 6.0f, 4.4f};
                radii = new float[]{1.6f, 0.75f, 0.42f, 0.38f};
                bulges = new float[]{0.2f, 0.18f, 0.02f};
            }
            leg[i] = RigMath.chain(body, (front ? "fleg" : "bleg") + i, start, dirs, lens, radii, bulges, 1.18f, HIDE, 1f, 0.14f);

            // its front left leg is broken: bone through the skin
            if (i == 1) leg[i][1].mesh = concat(leg[i][1].mesh, Gore.boneSpur(V.of(0.3f, 2.6f, -0.2f), V.of(0.5, 0.4, -1), 1.6f, 0.2f, rnd));
            // tendons down the back of the lower legs
            Bone lower = leg[i][1];
            lower.mesh = concat(lower.mesh, new MeshBuilder().rough(0.05f, 2f).ao(1.2f)
                    .tube(V.of(0, 0.6, 0.45), V.of(0, lens[1] - 0.4f, 0.32), 0.22f, 0.2f, HIDE, 6, 6, 0f, 1f, 1f, null, true, true)
                    .build());

            // paw: pad, four knuckled toes with black claws (+ dewclaw on the front legs), flat on the ground
            float[] m = RigMath.chainRotation(dirs);
            V fwd = RigMath.invApply(m, V.of(0, 0, -1)), dn = RigMath.invApply(m, V.of(0, 1, 0)), lx = RigMath.invApply(m, V.of(1, 0, 0));
            Bone last = leg[i][dirs.length - 1];
            V base = V.of(0, lens[lens.length - 1], 0);
            MeshBuilder pm = new MeshBuilder().rough(0.05f, 1.8f).ao(1.5f)
                    .ellipsoid(base.add(dn.mul(0.35f)).add(fwd.mul(0.35f)), 0.72f, 0.55f, 0.9f, 9, 12, HIDE, null);
            for (int t = 0; t < 4; t++) {
                float off = (t - 1.5f) * 0.42f;
                float reach = (t == 1 || t == 2) ? 0.25f : 0f;
                V knuckle = base.add(lx.mul(off)).add(dn.mul(0.35f)).add(fwd.mul(1.0f + reach));
                V toe = knuckle.add(fwd.mul(0.75f)).add(dn.mul(0.3f));
                pm.ellipsoid(knuckle, 0.3f, 0.3f, 0.3f, 6, 8, HIDE, null);
                pm.tube(knuckle, toe, 0.27f, 0.22f, HIDE, 6, 3, 0f, 1f, 1f, null, false, true);
                pm.cone(toe, toe.add(fwd.mul(0.85f)).add(dn.mul(0.45f)), 0.17f, KERATIN);
            }
            if (front) {
                V dew = base.add(lx.mul(-sx * 0.55f)).add(dn.mul(-0.7f)).add(fwd.mul(0.2f));
                pm.cone(dew, dew.add(fwd.mul(0.5f)).add(dn.mul(0.35f)), 0.14f, KERATIN);
            }
            last.mesh = concat(last.mesh, pm.build());
        }

        // ================================================================ tail: bony, half bald, hanging
        tail = body.add(new Bone("tail", 0, -1.4f, 9.4f, 0, 0, 0));
        V[] path = new V[11];
        for (int k = 0; k < 11; k++) {
            float t = k / 10f;
            path[k] = V.of(Math.sin(t * 2.2) * 0.4, -0.2 + t * t * 8.0, t * 4.6);
        }
        MeshBuilder tm = new MeshBuilder().rough(0.06f, 1.6f).ao(1.4f).loft(path, 0.6f, 0.14f, 8, HIDE);
        for (int k = 0; k < 10; k++) {
            V r0 = path[2 + rnd.nextInt(8)];
            V out = V.of((rnd.nextFloat() - 0.5f), 0.4f, 0.6f).norm();
            V[] fp = {r0, r0.add(out.mul(0.7f)), r0.add(out.mul(1.3f)).add(V.of(0, 0.4, 0))};
            float u0 = rnd.nextFloat() * 0.6f;
            tm.tint(0.85f).ribbon(fp, 0.45f, 0.1f, r0.sub(out), HAIR, u0, u0 + 0.35f, 0.7f, 1f).tint(1f);
        }
        tail.mesh = tm.build();
    }

    /** A scalloped, organic ridge (gums, lips) from a to b: overlapping flattened lumps with a slight wave. */
    private static void lipLine(MeshBuilder mb, V a, V b, int n, float r0, float r1, MeshBuilder.Swatch sw, float tint, float rough) {
        mb.tint(tint).rough(rough, 2.2f).ao(1.5f);
        for (int i = 0; i <= n; i++) {
            float t = (float) i / n;
            float r = r0 + (r1 - r0) * t;
            V c = V.lerp(a, b, t).add(V.of(0, (float) Math.sin(t * 9f) * 0.05f, 0));
            mb.ellipsoid(c, r, r * 0.8f, r * 1.6f, 6, 9, sw, null);
        }
        mb.tint(1f).rough(0f, 1f);
    }

    private static float[] concat(float[] a, float[] b) {
        float[] r = new float[a.length + b.length];
        System.arraycopy(a, 0, r, 0, a.length);
        System.arraycopy(b, 0, r, a.length, b.length);
        return r;
    }

    private static float sin(float a) { return (float) Math.sin(a); }
    private static float cos(float a) { return (float) Math.cos(a); }
    private static float lerp(float a, float b, float t) { return a + (b - a) * t; }

    /** Pose without memory (previews). */
    public void pose(float limbAngle, float limbDistance, float age, float headYawDeg, float headPitchDeg,
                     boolean attacking, int id) {
        pose(limbAngle, limbDistance, age, headYawDeg, headPitchDeg, attacking, false, 0f, id, new AnimState());
    }

    /**
     * Gait blends from a trot (diagonal pairs) into a full rotary gallop as it speeds up: the spine
     * flexes and stretches with each bound, haunches gather, head held level, ears pinned, tail streaming
     * out behind on a spring. It leans and looks into turns; the tail swings out the other way.
     * Idle: a low stalking crouch, panting, ears flicking independently, and now and then it drops its
     * head to sniff the ground, nose twitching. Attacking: lunging, snapping, shaking its head like it's
     * tearing at something. Pouncing: stretched out in the air, legs reaching.
     */
    public void pose(float limbAngle, float limbDistance, float age, float headYawDeg, float headPitchDeg,
                     boolean attacking, boolean airborne, float bodyYawDeg, int id, AnimState st) {
        st.tick(age);
        st.bodyYaw(bodyYawDeg);
        root.reset();
        float ld = Math.min(1f, limbDistance);
        float wMove = st.smooth(0, Math.min(1f, ld * 1.5f), 0.25f);
        float wGal = st.smooth(1, Math.max(0f, Math.min(1f, (ld - 0.45f) / 0.35f)), 0.12f);
        float wAtk = st.smooth(2, attacking ? 1f : 0f, 0.3f);
        float wAir = st.smooth(3, airborne ? 1f : 0f, 0.35f);
        float still = 1f - wMove;
        float turn = Math.max(-1f, Math.min(1f, st.turnRate / 15f));

        // ================================================================ trot -> gallop
        float ph = limbAngle * (0.7f + 0.15f * wGal);
        float[] trot = {0f, PI, PI, 0f};
        float[] gallop = {0f, 0.35f, PI, PI + 0.35f};
        for (int i = 0; i < 4; i++) {
            boolean front = i < 2;
            float phase = ph + lerp(trot[i], gallop[i], wGal);
            float swing = cos(phase), lift = Math.max(0f, sin(phase));
            float a = wMove * (0.5f + 0.22f * wGal);
            if (front) {
                leg[i][0].pitch -= swing * a;
                leg[i][1].pitch -= lift * (0.35f + 0.25f * wGal) * wMove;
                leg[i][2].pitch -= lift * 0.8f * wMove;
            } else {
                leg[i][0].pitch -= swing * a * 0.95f;
                leg[i][1].pitch += lift * (0.55f + 0.2f * wGal) * wMove;
                leg[i][2].pitch -= lift * 0.6f * wMove;
            }
        }
        // spine: gather and stretch with each bound when galloping
        float flex = sin(ph + PI * 0.5f);
        body.pitch += flex * 0.08f * wGal + sin(ph * 2f) * 0.035f * (1f - wGal) * wMove;
        body.oy -= (sin(ph) * 0.5f + 0.5f) * 1.2f * wGal + Math.abs(sin(ph)) * 0.6f * (1f - wGal) * wMove;
        body.roll += -turn * 0.12f;
        neck.pitch -= flex * 0.1f * wGal;

        // ================================================================ idle: stalking crouch, panting
        for (int i = 0; i < 4; i++) {
            if (i < 2) { leg[i][0].pitch -= 0.18f * still; leg[i][1].pitch += 0.32f * still; }
            else { leg[i][0].pitch -= 0.15f * still; leg[i][1].pitch += 0.2f * still; }
        }
        body.oy += 0.8f * still;
        body.pitch += 0.05f * still;
        body.meshScaleX = 1f + 0.05f * sin(age * 0.24f);

        // ================================================================ head and neck
        float sniffT = (age + id * 29) % 160f;
        float sniff = st.smooth(4, (sniffT < 50f && wMove < 0.3f && wAtk < 0.3f) ? 1f : 0f, 0.15f);
        neck.pitch += 0.28f + 0.1f * wMove + sniff * 0.5f;
        neck.pitch -= (0.5f * 0.12f * wGal + 0.035f * wMove) * flex * 0.6f;          // keep the head level
        head.yaw += headYawDeg * (PI / 180f) * (1f - sniff) + turn * 0.25f;          // looks into the turn
        head.pitch += headPitchDeg * (PI / 180f) * 0.6f * (1f - sniff) - 0.25f - 0.15f * wMove + sniff * 0.45f;
        head.pitch += sniff * sin(age * 3.1f) * 0.035f;                             // nose twitching
        head.yaw += sniff * sin(age * 0.4f) * 0.25f;
        if ((age + id * 23) % 90f < 3f) head.roll += 0.4f;
        // ears: flick independently, pinned back when running or attacking
        float pin = Math.max(wMove, wAtk);
        earL[0].pitch += 0.5f * pin + st.spring(9, AnimState.pulse((age + id * 3) % 37f, 0f, 3f) * 0.5f, 1.2f, 0.5f);
        earR[0].pitch += 0.5f * pin + st.spring(10, AnimState.pulse((age + id * 5) % 43f, 0f, 3f) * 0.5f, 1.2f, 0.5f);
        // jaw: panting when idle, snarling quiver
        jaw.pitch += 0.12f + 0.1f * (0.5f + 0.5f * sin(age * 0.9f)) * still + 0.03f * sin(age * 2.1f);

        // ================================================================ tail on a spring
        float tailPitch = lerp(0.15f, -0.55f, wGal);
        tail.pitch += st.spring(6, tailPitch + sin(age * 0.07f) * 0.05f, 0.15f, 0.3f);
        tail.yaw += st.spring(5, -turn * 0.6f + sin(age * 0.05f) * 0.12f * still, 0.12f, 0.25f);

        // ================================================================ attack: lunge, snap, shake
        if (wAtk > 0.001f) {
            jaw.pitch += 0.65f * Math.abs(sin(age * 1.1f)) * wAtk;
            head.yaw += sin(age * 1.5f) * 0.35f * wAtk;
            neck.pitch -= 0.25f * wAtk;
            neck.oz -= Math.abs(sin(age * 0.55f)) * 1.2f * wAtk;
            body.pitch += 0.08f * wAtk;
            leg[0][0].pitch -= 0.2f * wAtk;
            leg[1][0].pitch -= 0.2f * wAtk;
        }

        // ================================================================ pounce
        if (wAir > 0.001f) {
            for (int i = 0; i < 4; i++) {
                leg[i][0].pitch += (i < 2 ? -0.95f : 0.8f) * wAir;
                leg[i][1].pitch += (i < 2 ? 0.2f : -0.3f) * wAir;
            }
            body.pitch -= 0.1f * wAir;
            jaw.pitch += 0.7f * wAir;
            tail.pitch -= 0.5f * wAir;
        }
    }
}
