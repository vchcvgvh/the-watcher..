package com.thewatcher.client.mesh;

import java.util.Random;

import static com.thewatcher.client.mesh.MeshBuilder.Swatch.BONE;
import static com.thewatcher.client.mesh.MeshBuilder.Swatch.EYE;
import static com.thewatcher.client.mesh.MeshBuilder.Swatch.FLESH;
import static com.thewatcher.client.mesh.MeshBuilder.Swatch.KERATIN;
import static com.thewatcher.client.mesh.MeshBuilder.Swatch.HAIR;
import static com.thewatcher.client.mesh.MeshBuilder.Swatch.SCALP;
import static com.thewatcher.client.mesh.MeshBuilder.Swatch.SKIN;
import static com.thewatcher.client.mesh.MeshBuilder.bump;

/**
 * The Watcher's body: an emaciated, ~3.4-block-tall humanoid.
 *
 *  - elongated skull with mismatched, pitch-black eye sockets and small glowing eyes, sharp cheekbones,
 *    hollow cheeks, two nose slits, and a grin torn open across both cheeks lined with two rows of
 *    needle teeth; an underbite jaw that unhinges when it screams; thin wet hair hanging over its face
 *  - lumpy, uneven skin with baked shadows in every crease; a twisted spine and a drooping left shoulder
 *  - starved torso: ribs pushing through the skin, sunken belly, visible spine and shoulder blades,
 *    a torn open wound on its left side showing raw flesh and bare ribs
 *  - arms that reach past its knees: wasted muscles, bony elbows, long jointed fingers with claws
 *  - thin legs with knobbly knees, shin ridges and long clawed feet
 *
 * Units: pixels (16 = 1 block). y = 24 is the ground, +y is down, -z is the front,
 * -x is the creature's right side.
 */
public final class WatcherSkeleton implements Rig {
    @Override public Bone root() { return root; }

    private static final float DEG = (float) (Math.PI / 180.0);
    private static final float PI = (float) Math.PI;

    public final Bone root, hips, abdomen, chest, neck, head, jaw, eyeL, eyeR;
    // index 0 = right side (-x), 1 = left side (+x)
    public final Bone[] arm = new Bone[2], forearm = new Bone[2], hand = new Bone[2];
    public final Bone[] thigh = new Bone[2], shin = new Bone[2], foot = new Bone[2];
    public final Bone[][] finger = new Bone[2][5], fingerTip = new Bone[2][5];

    private static final V HEAD_C = V.of(0, -5.0, 0.3);
    private static final float HEAD_RX = 3.3f, HEAD_RY = 5.3f, HEAD_RZ = 3.9f;
    private static final V HEAD_R = V.of(HEAD_RX, HEAD_RY, HEAD_RZ);
    // Mismatched eyes: the right one sits lower, deeper and bigger.
    private static final V EYE_L = V.of(0.36, -0.15, -0.92).norm();
    private static final V EYE_R = V.of(-0.38, -0.07, -0.92).norm();
    private static final V JAW_C = V.of(0, 0.9, -1.3);
    private static final V JAW_R = V.of(2.7, 1.05, 2.0);

    /** Point on the mouth line, a in [-1, 1]: a wide grin that splits the cheeks and curls up towards the ears. */
    private static V mouthDir(float a) {
        float y = 0.62f - 0.30f * a * a;
        float k = (float) Math.sqrt(1 - y * y);
        return V.of(Math.sin(a * 1.65) * k, y, -Math.cos(a * 1.65) * k).norm();
    }

    /** Surface point of the skull in direction d, pushed in/out by {@code scale}. */
    private static V skullPoint(V d, float scale) {
        return HEAD_C.add(d.mul(HEAD_R).mul(headShape(d)).mul(scale));
    }

    public WatcherSkeleton() {
        Random rnd = new Random(0xBADC0DE);
        root = new Bone("root", 0, 0, 0, 0, 0, 0);

        // ---------------------------------------------------------------- pelvis
        hips = root.add(new Bone("hips", 0, 1.2f, 0, 0, 0, 0));
        hips.mesh = new MeshBuilder().rough(0.14f, 0.75f)
                .ellipsoid(V.of(0, 0.2, 0), 3.8f, 2.3f, 2.5f, 14, 20, SKIN, d -> {
                    float r = 1f
                            + 0.2f * bump(d, V.of(0.75, -0.45, -0.4).norm(), 0.05f)    // jutting hip bones
                            + 0.2f * bump(d, V.of(-0.75, -0.45, -0.4).norm(), 0.05f)
                            + 0.08f * bump(d, V.of(0, -0.3, 0.95).norm(), 0.08f);
                    return V.of(r, r, r);
                })
                .build();

        // ---------------------------------------------------------------- belly (sunken, pinched waist)
        abdomen = hips.add(new Bone("abdomen", 0, -0.6f, 0, 0.28f, 0, 0));
        MeshBuilder ab = new MeshBuilder().rough(0.16f, 0.7f)
                .blood(V.of(3.0, -6.0, -1.4), V.of(2.0, 6.0, 1.8), false, 0.9f)       // blood run from the chest wound
                .blood(V.of(-3.6, -8.5, -0.6), V.of(1.0, 3.2, 1.0), false, 0.6f)
                .tube(V.of(0, 1.2, 0), V.of(0, -11.0, 0), 3.1f, 2.7f, SKIN, 20, 10, -0.22f, 1f, 0.6f, V.of(1, 0, 0), true, true);
        for (int k = 0; k < 6; k++) {                                              // lumbar vertebrae
            float y = -0.5f - 1.6f * k;
            float t = (1.2f - y) / 12.2f;
            float r = (3.1f - 0.4f * t) * (1f - 0.22f * (float) Math.sin(PI * t));
            ab.ellipsoid(V.of(0, y, r * 0.6f - 0.05f), 0.62f, 0.5f, 0.42f, 8, 10, SKIN, RigMath::vertebra);
        }
        Gore.wound(ab, V.of(-1.0, -4.3, -2.25), V.of(-0.4, 0.05, -1), 1.55f, rnd);        // belly torn open...
        Gore.entrails(ab, V.of(-1.0, -3.9, -2.5), V.of(-0.25, 0, -1), 12f, 0.55f, rnd);    // ...and its guts hanging out
        abdomen.mesh = ab.build();

        // ---------------------------------------------------------------- ribcage (twisted spine)
        chest = abdomen.add(new Bone("chest", 0, -9.5f, 0, 0.22f, 0.06f, 0.07f));
        MeshBuilder ch = new MeshBuilder().rough(0.12f, 0.8f).ao(1.5f)
                .blood(V.of(4.25, -3.6, -1.5), 2.8f, true, 1f)                         // around the side wound
                .blood(V.of(4.3, -0.4, -1.6), V.of(2.0, 4.2, 2.0), false, 0.95f)          // ...running down
                .blood(V.of(-4.7, -4.0, -1.0), 2.0f, true, 1f)                        // around the broken rib
                .blood(V.of(-4.5, -1.2, -1.2), V.of(1.6, 4.0, 1.6), false, 0.9f)
                .blood(V.of(0, -2.6, 3.5), V.of(1.8, 5.5, 1.2), false, 0.9f)             // down the back from the spine
                .ellipsoid(V.of(0, -4.7, 0.2), 5.3f, 6.4f, 3.4f, 34, 40, SKIN, WatcherSkeleton::chestShape);
        for (int sd = -1; sd <= 1; sd += 2) {                                       // collarbones
            ch.tube(V.of(sd * 0.7, -10.9, -2.0), V.of(sd * 4.9, -10.6, -0.6), 0.52f, 0.42f, SKIN, 8, 5, 0f, 1f, 1f, null, true, true);
        }
        // the wound: torn skin, raw flesh, bare ribs (creature's left side)
        ch.rough(0.32f, 1.5f).ao(2f)
                .rough(0.05f, 2f).ao(1.2f)
                .tube(V.of(3.35, -5.4, -2.35), V.of(5.35, -4.7, -0.3), 0.32f, 0.28f, BONE, 7, 5, 0f, 1f, 1f, null, true, true)
                .tube(V.of(3.40, -3.8, -2.45), V.of(5.40, -3.1, -0.4), 0.32f, 0.28f, BONE, 7, 5, 0f, 1f, 1f, null, true, true)
                .tube(V.of(3.50, -2.1, -2.25), V.of(5.25, -1.6, -0.4), 0.30f, 0.26f, BONE, 7, 5, 0f, 1f, 1f, null, true, true)
                .rough(0.12f, 0.8f).ao(1.2f);
        for (int k = 0; k < 7; k++) {                                              // thoracic vertebrae
            float y = -1.0f - 1.5f * k;
            float dy = (y + 4.7f) / 6.4f;
            float zs = 0.2f + 3.4f * (float) Math.sqrt(Math.max(0, 1 - dy * dy));
            if (k >= 3 && k <= 5) {
                Gore.exposedBone(ch, V.of(0, y, zs + 0.1f), V.of(0, 0, 1), 0.55f, rnd);   // vertebrae through the skin
            } else {
                ch.ellipsoid(V.of(0, y, zs - 0.15f), 0.62f, 0.5f, 0.45f, 8, 10, SKIN, RigMath::vertebra);
            }
        }
        Gore.wound(ch, V.of(4.3, -3.7, -1.45), V.of(0.8, 0, -0.6), 1.55f, rnd);               // the torn side, ribs showing
        Gore.boneSpur(ch, V.of(-4.55, -4.1, -1.25), V.of(-0.85, 0.15, -0.55), 3.4f, 0.38f, rnd);   // a rib, snapped outwards
        Gore.drip(ch, V.of(-5.0, -3.4, -1.6), V.of(0, 1, 0), 2.6f, 0.12f, rnd);
        Gore.drip(ch, V.of(4.6, -2.2, -1.9), V.of(0, 1, 0), 3.2f, 0.12f, rnd);
        chest.mesh = ch.build();

        // ---------------------------------------------------------------- neck (straining tendons, windpipe)
        neck = chest.add(new Bone("neck", 0, -10.8f, -0.6f, 0.15f, 0, 0));
        MeshBuilder nk = new MeshBuilder().rough(0.1f, 0.9f).ao(1.4f)
                .blood(V.of(0, -2.5, -1.3), V.of(1.6, 3.5, 1.0), false, 0.9f)          // down the throat
                .tube(V.of(0, 0.5, 0), V.of(0, -5, -0.3), 1.35f, 1.0f, SKIN, 14, 6, -0.1f, 1f, 0.9f, V.of(1, 0, 0), false, false)
                .tube(V.of(0, -4.2, -0.9), V.of(0, 0.2, -1.2), 0.45f, 0.5f, SKIN, 8, 6, 0f, 1f, 1f, null, true, true) // windpipe
                .ellipsoid(V.of(0, -2.4, -1.35), 0.42f, 0.5f, 0.38f, SKIN)
                .ellipsoid(V.of(0, -1.0, 1.15), 0.48f, 0.42f, 0.48f, SKIN)
                .ellipsoid(V.of(0, -2.6, 1.05), 0.45f, 0.42f, 0.45f, SKIN);
        for (int sd = -1; sd <= 1; sd += 2) {
            nk.tube(V.of(sd * 0.85, -4.4, -0.2), V.of(sd * 0.35, 0.0, -1.2), 0.34f, 0.38f, SKIN, 7, 5, 0f, 1f, 1f, null, true, true);
            nk.tube(V.of(sd * 1.0, -4.6, 0.5), V.of(sd * 1.1, 0.3, 0.2), 0.28f, 0.3f, SKIN, 6, 5, 0f, 1f, 1f, null, true, true);
        }
        neck.mesh = nk.build();

        // ---------------------------------------------------------------- skull
        head = neck.add(new Bone("head", 0, -5f, -0.3f, -0.65f, 0, 0));
        MeshBuilder hd = new MeshBuilder();
        addFace(hd, rnd);
        // hair, part 1: the scalp under it (dark, dense roots). It sinks inside the skull below the hairline.
        hd.tint(1f).rough(0.02f, 2f).ao(0.8f)
                .ellipsoid(HEAD_C, HEAD_RX, HEAD_RY, HEAD_RZ, 34, 44, SCALP, d -> {
                    float h = hairness(d);
                    return headShape(d).mul(0.55f + 0.475f * h);
                });
        // hair, part 2: ~170 layered locks of long, thin, wet, stringy hair. Inner layers lie flat on the
        // skull, outer layers hang loose. Parted off-centre so it falls over the right side of its face.
        hd.rough(0f, 1f);
        int locks = 0;
        while (locks < 170) {
            V d;
            float dy = -1f + rnd.nextFloat() * 1.6f;
            double ph = rnd.nextDouble() * Math.PI * 2;
            float rr = (float) Math.sqrt(Math.max(0, 1 - dy * dy));
            d = V.of(rr * Math.cos(ph), dy, rr * Math.sin(ph));
            if (hairness(d) < 0.7f) continue;
            boolean front = d.z() < -0.25f;
            // Only a few locks fall over the face, all on its right side: the left eye stays clear.
            boolean face = front && d.x() < 0.15f && rnd.nextFloat() < 0.45f;
            if (front && !face && rnd.nextFloat() < 0.5f) continue;   // thinner at the front
            locks++;
            int layer = locks % 3;                                    // 0 inner, 1 middle, 2 outer
            float hug = layer == 0 ? 1.035f : layer == 1 ? 1.07f : 1.11f;
            if (face) hug += 0.05f;
            if (front && !face) hug -= 0.01f;
            float len = (layer == 0 ? 7f : layer == 1 ? 11f : 14f) + rnd.nextFloat() * 7f;
            if (face) len = 12f + rnd.nextFloat() * 5f;
            float w = (face ? 0.6f : 0.9f) + rnd.nextFloat() * (face ? 0.5f : 0.8f);
            V start = skullPoint(d, 1.0f);
            V down = V.of(0, 1, 0);
            // Face locks hang straight down over the right eye and cheek; the rest of the front hair is
            // swept back and to the sides, framing the face.
            V comb = face ? V.of(-0.2, 0, 0)
                    : front ? V.of(Math.signum(d.x()) * 0.9f, 0, 0.8f)
                    : V.of(0, 0, 0);
            V dir = down.add(comb).sub(d.mul(down.add(comb).dot(d)));
            if (dir.len() < 0.2f) dir = V.of(d.x(), 0, d.z() + 0.3f);
            dir = dir.norm();
            int segs = 14;
            V[] path = new V[segs + 1];
            path[0] = start;
            float curl = (rnd.nextFloat() - 0.5f) * 0.25f;
            for (int i = 1; i <= segs; i++) {
                V jitter = V.of(rnd.nextFloat() - 0.5f, 0, rnd.nextFloat() - 0.5f).mul(0.08f);
                V twist = V.of(curl * (float) Math.sin(i * 0.9f), 0, curl * (float) Math.cos(i * 0.9f));
                dir = dir.mul(0.6f).add(down.mul(0.4f)).add(jitter).add(twist).norm();
                V q = path[i - 1].add(dir.mul(len / segs));
                q = pushOut(q, HEAD_C, HEAD_R, hug);
                q = pushOut(q, JAW_C, JAW_R, 1.35f);                // keep clear of the jaw and teeth
                path[i] = q;
            }
            float u0 = rnd.nextFloat() * 0.6f;
            float shade = layer == 0 ? 0.55f : layer == 1 ? 0.8f : 1f;
            hd.ribbon(path, w, w * 0.35f, HEAD_C, HAIR, u0, u0 + 0.4f, shade * 0.7f, shade);
        }
        hd.tint(1f);
        head.mesh = hd.build();

        // eyeballs: separate joints so they can move on their own. The right one is lazy, turned out and down.
        Bone[] eyes = addEyes(head);
        eyeL = eyes[0];
        eyeR = eyes[1];

        // ---------------------------------------------------------------- lower jaw: underbite of needles
        jaw = addJaw(head, rnd);


        // ---------------------------------------------------------------- arms (the left shoulder droops, its arm is longer)
        for (int s = 0; s < 2; s++) {
            float sx = s == 0 ? -1f : 1f;   // which side
            float m = -sx;                  // "inward" (towards the body) for thumbs
            float foreLen = s == 0 ? 13.5f : 14.6f;

            arm[s] = chest.add(new Bone("arm" + s, sx * 5.3f, s == 0 ? -10.2f : -9.3f, 0.6f, -0.55f, 0, -sx * (s == 0 ? 0.10f : 0.16f)));
            MeshBuilder am = new MeshBuilder().rough(0.13f, 0.8f).ao(1.3f)
                    .ellipsoid(V.of(0, 0, 0), 1.6f, 1.5f, 1.6f, 12, 14, SKIN, RigMath::condyle)
                    .tube(V.of(0, 0, 0), V.of(0, 13, 0), 1.4f, 0.9f, SKIN, 14, 9, 0.1f, 1f, 1f, V.of(1, 0, 0), false, false)
                    .tube(V.of(0, 1.0, 1.0), V.of(0, 12.0, 0.75), 0.4f, 0.32f, SKIN, 6, 5, 0f, 1f, 1f, null, true, true) // triceps tendon
                    .ellipsoid(V.of(0, 13, 0.35), 1.05f, 1.15f, 1.25f, 12, 14, SKIN, RigMath::condyle);
            if (s == 1) Gore.flayed(am, V.of(sx * 1.05f, 6.2f, -0.5f), V.of(sx * 0.85f, 0, -0.5f), V.of(0, 1, 0), 1.05f, rnd);   // skin torn off the arm
            arm[s].mesh = am.build();

            forearm[s] = arm[s].add(new Bone("forearm" + s, 0, 13f, 0, -0.28f, 0, 0));
            MeshBuilder fb = new MeshBuilder().rough(0.12f, 0.9f).ao(1.3f)
                    .blood(V.of(0, foreLen - 1.5f, 0), V.of(1.3, 3.5, 1.3), false, 0.65f);   // bloody to the wrist
            if (s == 1) fb.blood(V.of(0.9, 6.4, 0), 2.2f, true, 1f).blood(V.of(0.7, 10.0, 0.1), V.of(1.5, 4.4, 1.5), false, 0.95f);
            fb
                    .tube(V.of(0, 0, 0), V.of(0, foreLen, 0), 1.0f, 0.6f, SKIN, 12, 9, 0.12f, 1.25f, 0.8f, V.of(1, 0, 0), false, false)
                    .tube(V.of(0, 0.8, 0.8), V.of(0, foreLen - 1, 0.45), 0.34f, 0.3f, SKIN, 6, 6, 0f, 1f, 1f, null, true, true)
                    .tube(V.of(-0.45, 3.0, -0.75), V.of(-0.35, foreLen - 0.5, -0.5), 0.2f, 0.18f, SKIN, 5, 6, 0f, 1f, 1f, null, true, true)
                    .tube(V.of(0.45, 3.0, -0.75), V.of(0.35, foreLen - 0.5, -0.5), 0.2f, 0.18f, SKIN, 5, 6, 0f, 1f, 1f, null, true, true)
                    .ellipsoid(V.of(0, foreLen, 0), 0.78f, 0.6f, 0.68f, SKIN);
            if (s == 1) {                                                               // compound fracture
                Gore.boneSpur(fb, V.of(0.75, 6.3, -0.1), V.of(1, -0.55, -0.25), 3.2f, 0.36f, rnd);
                Gore.drip(fb, V.of(1.2, 6.9, -0.2), V.of(0, 1, 0), 3.5f, 0.11f, rnd);
            }
            forearm[s].mesh = fb.build();

            hand[s] = forearm[s].add(new Bone("hand" + s, 0, foreLen, 0, -0.12f, 0, 0));
            MeshBuilder hb = new MeshBuilder().rough(0.06f, 1.5f).ao(1.3f)
                    .blood(V.of(0, 2.2, 0), 2.8f, false, 0.6f)
                    .ellipsoid(V.of(0, 1.8, -0.05), 1.35f, 1.95f, 0.6f, 10, 14, SKIN, null);
            for (int i = 0; i < 4; i++) {
                float x = -0.95f + 0.63f * i;
                hb.tube(V.of(x * 0.6f, 0.6, 0.42), V.of(x, 3.4, 0.38), 0.17f, 0.17f, SKIN, 5, 3, 0f, 1f, 1f, null, true, true);
            }
            hand[s].mesh = hb.build();

            float[] lengths = {0.85f, 1.05f, 1.0f, 0.8f};
            for (int i = 0; i < 4; i++) {
                float L = lengths[i];
                float x = -0.98f + 0.65f * i;
                finger[s][i] = hand[s].add(new Bone("finger" + s + i, x, 3.5f, -0.05f, -0.3f, 0, (i - 1.5f) * 0.07f));
                finger[s][i].mesh = new MeshBuilder().ao(1.2f)
                        .ellipsoid(V.of(0, 0, 0), 0.42f, 0.4f, 0.42f, 6, 8, SKIN, null)
                        .tube(V.of(0, 0, 0), V.of(0, 4.4f * L, 0), 0.31f, 0.24f, SKIN, 7, 4, 0f, 1f, 1f, null, false, false)
                        .build();
                fingerTip[s][i] = finger[s][i].add(new Bone("tip" + s + i, 0, 4.4f * L, 0, -0.4f, 0, 0));
                fingerTip[s][i].mesh = new MeshBuilder().ao(1.2f)
                        .blood(V.of(0, 3.4f * L + 1.5f, -0.5f), 3.2f, true, 0.7f)                  // claws wet with it
                        .ellipsoid(V.of(0, 0, 0), 0.32f, 0.32f, 0.32f, 6, 8, SKIN, null)
                        .tube(V.of(0, 0, 0), V.of(0, 3.6f * L, 0), 0.24f, 0.18f, SKIN, 7, 4, 0f, 1f, 1f, null, false, false)
                        .tint(1f).cone(V.of(0, 3.4f * L, 0), V.of(0, 3.4f * L + 3.4f, -1.3f), 0.24f, KERATIN)  // dirty hooked claw
                        .build();
            }
            finger[s][4] = hand[s].add(new Bone("thumb" + s, m * 1.15f, 1.3f, -0.45f, -0.5f, 0, -m * 0.6f));
            finger[s][4].mesh = new MeshBuilder().ao(1.2f)
                    .ellipsoid(V.of(0, 0, 0), 0.45f, 0.45f, 0.45f, 6, 8, SKIN, null)
                    .tube(V.of(0, 0, 0), V.of(0, 3.0, 0), 0.36f, 0.28f, SKIN, 7, 4, 0f, 1f, 1f, null, false, false)
                    .build();
            fingerTip[s][4] = finger[s][4].add(new Bone("thumbtip" + s, 0, 3.0f, 0, -0.3f, 0, 0));
            fingerTip[s][4].mesh = new MeshBuilder().ao(1.2f)
                    .blood(V.of(0, 3.0, -0.4), 2.8f, true, 0.7f)
                    .ellipsoid(V.of(0, 0, 0), 0.32f, 0.32f, 0.32f, 6, 8, SKIN, null)
                    .tube(V.of(0, 0, 0), V.of(0, 2.4, 0), 0.28f, 0.21f, SKIN, 7, 3, 0f, 1f, 1f, null, false, false)
                    .tint(1f).cone(V.of(0, 2.2, 0), V.of(0, 4.6, -0.9), 0.25f, KERATIN)
                    .build();
        }

        // ---------------------------------------------------------------- legs
        for (int s = 0; s < 2; s++) {
            float sx = s == 0 ? -1f : 1f;
            thigh[s] = hips.add(new Bone("thigh" + s, sx * 2.3f, 0.9f, 0, -0.18f, 0, -sx * 0.04f));
            thigh[s].mesh = new MeshBuilder().rough(0.14f, 0.75f).ao(1.3f)
                    .ellipsoid(V.of(0, 0.3, 0), 1.9f, 1.7f, 1.9f, 10, 12, SKIN, null)
                    .tube(V.of(0, 0, 0), V.of(0, 11, 0), 1.8f, 1.0f, SKIN, 14, 8, 0.08f, 1f, 1.05f, V.of(1, 0, 0), false, false)
                    .ellipsoid(V.of(0, 11, -0.65), 1.0f, 1.25f, 0.8f, 8, 10, SKIN, null)
                    .ellipsoid(V.of(0, 11, 0), 1.15f, 1.1f, 1.2f, 12, 14, SKIN, RigMath::condyle)
                    .build();

            shin[s] = thigh[s].add(new Bone("shin" + s, 0, 11f, 0, 0.36f, 0, 0));
            shin[s].mesh = new MeshBuilder().rough(0.12f, 0.85f).ao(1.3f)
                    .tube(V.of(0, 0, 0), V.of(0, 9.6, 0), 1.1f, 0.6f, SKIN, 12, 8, 0.08f, 0.9f, 1.1f, V.of(1, 0, 0), false, false)
                    .ellipsoid(V.of(0, 3.0, 0.45), 0.85f, 2.6f, 0.8f, 8, 10, SKIN, null)
                    .tube(V.of(0, 0.6, -0.9), V.of(0, 9.0, -0.55), 0.33f, 0.28f, SKIN, 6, 6, 0f, 1f, 1f, null, true, true)
                    .ellipsoid(V.of(0, 9.6, 0), 0.95f, 0.7f, 0.85f, SKIN)
                    .build();

            foot[s] = shin[s].add(new Bone("foot" + s, 0, 9.6f, 0, -0.18f, 0, 0));
            MeshBuilder fb = new MeshBuilder().rough(0.08f, 1.2f).ao(1.3f)
                    .ellipsoid(V.of(0, 0.6, 0.55), 0.8f, 0.75f, 0.9f, SKIN)
                    .tube(V.of(0, 0.3, 0.3), V.of(0, 0.95, -4.3), 0.9f, 0.62f, SKIN, 10, 6, 0f, 1.25f, 0.55f, V.of(1, 0, 0), true, false);
            for (int i = 0; i < 4; i++) {
                float x = -0.78f + 0.52f * i;
                float reach = (i == 1 || i == 2) ? -0.3f : 0f;
                fb.tint(1f).tube(V.of(x, 0.95, -4.0), V.of(x, 1.25, -5.6 + reach), 0.28f, 0.21f, SKIN, 6, 3, 0f, 1f, 1f, null, false, true);
                fb.tint(1f).cone(V.of(x, 1.25, -5.5 + reach), V.of(x, 1.75, -7.0 + reach), 0.22f, KERATIN);
            }
            foot[s].mesh = fb.build();
        }
    }

    // ------------------------------------------------------------------ shared face (also used by the Crawler)

    /** The skull, eyelids, torn grin and both rows of teeth, in head-bone space (no hair, no eyeballs). */
    public static void addFace(MeshBuilder hd, Random rnd) {
        int mark = hd.bloodMark();
        hd.blood(V.of(0, -1.0, -3.0), V.of(3.4, 2.2, 2.0), true, 1f)                  // the grin
                .blood(V.of(0, 1.2, -2.4), V.of(2.4, 3.0, 2.0), false, 0.9f)          // chin and throat
                .blood(eyeCenter(false).add(V.of(0.05, 2.0, -0.25)), V.of(0.45, 2.1, 0.45), false, 0.85f) // tear of blood
                .blood(eyeCenter(true).add(V.of(0, 1.2, -0.25)), V.of(0.3, 1.1, 0.35), false, 0.6f);
        hd.rough(0.06f, 1.1f).ao(1.8f)
                .ellipsoid(HEAD_C, HEAD_RX, HEAD_RY, HEAD_RZ, 44, 60, SKIN, WatcherSkeleton::headShape)
                .rough(0f, 1f).ao(0.6f);
        // eyelids pulled back from the eyeballs in a fixed, lidless stare; the lower lids sag and show red
        V eyeCL = eyeCenter(true), eyeCR = eyeCenter(false);
        hd.rough(0.03f, 2f).ao(1.2f);
        eyelids(hd, eyeCL, EYE_L, 0.62f);
        eyelids(hd, eyeCR, EYE_R, 0.72f);
        // raw flesh lining the split grin
        hd.rough(0.12f, 2f).ao(1.5f);
        for (int i = 0; i <= 30; i++) {
            float a = -1f + 2f * i / 30f;
            float wide = Math.abs(a) < 0.45f ? 1f : 0.6f;
            hd.ellipsoid(skullPoint(mouthDir(a), 0.9f), 0.42f, 0.34f * wide, 0.42f, 5, 8, FLESH, null);
        }
        hd.ellipsoid(V.of(0, -0.9, -0.8), 2.2f, 1.2f, 2.0f, 8, 12, FLESH, null);   // throat cavity
        hd.rough(0f, 1f).ao(0.8f);
        // upper teeth: needles set into the grin, running back into the torn cheeks
        for (int i = 0; i <= 26; i++) {
            float a = -0.95f + 1.9f * i / 26f;
            V m = mouthDir(a);
            V base = skullPoint(m, 0.985f);
            V out = V.of(m.x(), 0, m.z()).norm();
            boolean front = Math.abs(a) < 0.45f;
            float len = front ? 1.4f + rnd.nextFloat() * 1.0f : 0.6f + rnd.nextFloat() * 0.5f;
            if (rnd.nextInt(9) == 0) len *= 0.45f;                                 // broken tooth
            V tip = base.add(V.of((rnd.nextFloat() - 0.5f) * 0.5f, len, 0)).add(out.mul(0.25f + rnd.nextFloat() * 0.2f));
            hd.cone(base.sub(V.of(0, 0.3, 0)), tip, front ? 0.2f : 0.15f, BONE);
        }
        // second, inner row (shark-like)
        for (int i = 0; i <= 16; i++) {
            float a = -0.6f + 1.2f * i / 16f;
            V m = mouthDir(a);
            V base = skullPoint(m, 0.9f);
            float len = 0.8f + rnd.nextFloat() * 0.6f;
            hd.cone(base.sub(V.of(0, 0.2, 0)), base.add(V.of((rnd.nextFloat() - 0.5f) * 0.3f, len, 0)), 0.17f, BONE);
        }
        hd.bloodReset(mark);
    }

    public static V eyeCenter(boolean left) {
        return left ? skullPoint(EYE_L, 1f).add(EYE_L.mul(0.12f)) : skullPoint(EYE_R, 1f).add(EYE_R.mul(0.14f));
    }

    /** Two eyeball joints; the right one is lazy (turned out and down). */
    public static Bone[] addEyes(Bone head) {
        V l = eyeCenter(true), r = eyeCenter(false);
        Bone eL = head.add(new Bone("eyeL", l.x(), l.y(), l.z(), 0f, 0f, 0f));
        eL.mesh = new MeshBuilder().ao(0.3f).ellipsoid(V.of(0, 0, 0), 0.62f, 0.62f, 0.62f, 14, 20, EYE, null).build();
        Bone eR = head.add(new Bone("eyeR", r.x(), r.y(), r.z(), 0.12f, 0.24f, 0f));
        eR.mesh = new MeshBuilder().ao(0.3f).ellipsoid(V.of(0, 0, 0), 0.72f, 0.72f, 0.72f, 14, 20, EYE, null).build();
        return new Bone[]{eL, eR};
    }

    /** The hinged lower jaw with its underbite of needles. */
    public static Bone addJaw(Bone head, Random rnd) {
        V jp = V.of(0, -1.3, 0.6);
        V jc = V.of(0, 0.9, -1.3);
        V jr = V.of(2.7, 1.05, 2.0);
        Bone jaw = head.add(new Bone("jaw", jp.x(), jp.y(), jp.z(), 0.10f, 0, 0));
        MeshBuilder jw = new MeshBuilder().rough(0.08f, 1.2f).ao(1.4f)
                .blood(V.of(0, 0.2, -2.0).sub(jp), V.of(2.2, 1.3, 1.6), true, 0.6f)
                .ellipsoid(jc.sub(jp), jr.x(), jr.y() * 0.9f, jr.z(), 16, 24, SKIN, d -> {
                    // bony V-shaped mandible: narrow pointed chin, sharp jaw angles at the back, hollow underneath
                    float mx = 1f - 0.45f * Math.max(0f, -d.z());
                    float r = 1f + 0.2f * bump(d, V.of(0, 0.35, -0.94).norm(), 0.06f)
                            + 0.16f * bump(d, V.of(0.75, 0.3, 0.6).norm(), 0.12f) + 0.16f * bump(d, V.of(-0.75, 0.3, 0.6).norm(), 0.12f)
                            - 0.12f * bump(d, V.of(0, 1, 0), 0.35f);
                    return V.of(mx * r, r, r);
                })
                .rough(0.12f, 2f)
                .ellipsoid(V.of(0, 0.0, -1.3).sub(jp), 2.2f, 0.5f, 1.75f, 8, 16, FLESH, null)
                .ellipsoid(V.of(0, 0.3, -0.9).sub(jp), 1.9f, 0.7f, 1.8f, 6, 12, FLESH, null)
                .rough(0f, 1f).ao(0.8f);
        for (int i = 0; i <= 20; i++) {
            float a = -0.9f + 1.8f * i / 20f;
            V dj = V.of(Math.sin(a * 1.5), -0.6, -Math.cos(a * 1.5)).norm();
            float mx = 1f - 0.3f * Math.max(0f, -dj.z());
            V base = jc.add(V.of(dj.x() * jr.x() * mx, dj.y() * jr.y(), dj.z() * jr.z()).mul(0.95f));
            float len = Math.abs(a) < 0.4f ? 1.1f + rnd.nextFloat() * 0.9f : 0.6f + rnd.nextFloat() * 0.5f;
            V tip = base.add(V.of((rnd.nextFloat() - 0.5f) * 0.4f, -len, -0.2f - rnd.nextFloat() * 0.2f));
            jw.cone(base.add(V.of(0, 0.3, 0)).sub(jp), tip.sub(jp), 0.19f, BONE);
        }
        for (int k = 0; k < 4; k++) {                                                   // strings of blood off the chin
            float x = -1.2f + k * 0.8f + (rnd.nextFloat() - 0.5f) * 0.3f;
            Gore.drip(jw, V.of(x, 1.6, -2.4 + Math.abs(x) * 0.35).sub(jp), V.of(0, 1, 0.05), 1.6f + rnd.nextFloat() * 2.8f, 0.1f, rnd);
        }
        jaw.mesh = jw.build();
        return jaw;
    }

    /** Upper and lower lids as arcs around an eye looking along {@code e}. */
    private static void eyelids(MeshBuilder mb, V c, V e, float eyeR) {
        V a = e.cross(V.of(0, 1, 0)).norm();
        V b = a.cross(e).norm();
        if (b.y() > 0) b = b.mul(-1f);          // b points up (-y)
        for (int lid = 0; lid < 2; lid++) {
            int n = 10;
            V[] path = new V[n + 1];
            V[] inner = new V[n + 1];
            for (int i = 0; i <= n; i++) {
                double t = Math.PI * (0.08 + 0.84 * i / n) + (lid == 1 ? Math.PI : 0);
                // almond-shaped opening: wide, short; the lower lid sags
                float sag = lid == 1 ? 0.95f : 0.72f;
                V off = a.mul((float) Math.cos(t) * 1.3f).add(b.mul((float) Math.sin(t) * sag));
                // lids wrap onto the eyeball surface instead of floating in front of it
                float bulge = (float) Math.sqrt(Math.max(0.0, 1.0 - Math.pow(Math.sin(t), 2) * 0.5));
                path[i] = c.add(off.mul(eyeR * 1.02f)).add(e.mul(eyeR * 0.45f * bulge));
                inner[i] = c.add(off.mul(eyeR * 0.93f)).add(e.mul(eyeR * 0.4f * bulge));
            }
            mb.loft(path, 0.13f, 0.13f, 6, SKIN);
            if (lid == 1) mb.loft(inner, 0.09f, 0.09f, 6, FLESH);   // raw red rim under the eye
        }
    }

    private static V pushOut(V q, V c, V r, float scale) {
        V rel = q.sub(c);
        V e = V.of(rel.x() / r.x(), rel.y() / r.y(), rel.z() / r.z());
        if (e.len() >= scale) return q;
        return c.add(e.norm().mul(scale).mul(r));
    }

    private static float smoothstep(float e0, float e1, float x) {
        float t = Math.max(0f, Math.min(1f, (x - e0) / (e1 - e0)));
        return t * t * (3 - 2 * t);
    }

    /** 1 where hair grows (crown, sides, back, nape), 0 on the face. Hairline sits high on the forehead. */
    public static float hairness(V d) {
        float top = smoothstep(-0.42f, -0.6f, d.y());
        float sideBack = smoothstep(-0.5f, -0.25f, d.z()) * smoothstep(0.55f, 0.3f, d.y());
        return Math.max(top, sideBack);
    }

    // ------------------------------------------------------------------ sculpting

    /** Elongated skull: mismatched deep sockets, brow, cheekbones, hollow cheeks, nose slits, torn grin. */
    public static V headShape(V d) {
        float mx = (1f - 0.32f * Math.max(0f, d.y())) * (1f + 0.08f * Math.max(0f, -d.y()) * Math.max(0f, d.z()));
        float my = 1f + 0.12f * Math.max(0f, -d.y()) * Math.max(0f, d.z());
        float mz = (1f + 0.25f * Math.max(0f, d.z()) * Math.max(0f, -d.y()))
                * (1f - 0.10f * Math.max(0f, -d.z()) * Math.max(0f, d.y() + 0.2f));          // flatter face plane
        // continuous brow ridge and zygomatic arches running back towards the ears
        float brow = (float) Math.exp(-Math.pow((d.y() + 0.33f) / 0.07f, 2)) * Math.max(0f, Math.min(1f, (-d.z() - 0.55f) * 3f));
        float arch = (float) Math.exp(-Math.pow((d.y() - 0.1f) / 0.07f, 2))
                * Math.max(0f, Math.min(1f, (Math.abs(d.x()) - 0.45f) * 3f)) * Math.max(0f, Math.min(1f, (0.3f - d.z()) * 2f));
        float r = 1f + 0.06f * brow + 0.045f * arch + 0.03f * bump(d, V.of(0, -0.6, -0.8).norm(), 0.15f)
                - 0.30f * bump(d, EYE_L, 0.06f)
                - 0.36f * bump(d, EYE_R, 0.07f)
                + 0.10f * bump(d, V.of(0.6, 0.1, -0.78).norm(), 0.04f)       // sharp cheekbones
                + 0.10f * bump(d, V.of(-0.6, 0.1, -0.78).norm(), 0.04f)
                - 0.12f * bump(d, V.of(0.62, 0.36, -0.68).norm(), 0.07f)     // hollow cheeks
                - 0.12f * bump(d, V.of(-0.62, 0.36, -0.68).norm(), 0.07f)
                - 0.04f * bump(d, V.of(0, 0.08, -1).norm(), 0.03f)           // flat, noseless face...
                - 0.13f * bump(d, V.of(0.07, 0.13, -1).norm(), 0.006f)       // ...with two slits
                - 0.13f * bump(d, V.of(-0.07, 0.13, -1).norm(), 0.006f)
                - 0.07f * bump(d, V.of(0.95, -0.3, -0.1).norm(), 0.06f)      // sunken temples
                - 0.07f * bump(d, V.of(-0.95, -0.3, -0.1).norm(), 0.06f);
        // the grin: a wide open mouth that narrows into slits across both cheeks
        float cut = 0f;
        for (int i = 0; i <= 20; i++) {
            float a = -1f + 2f * i / 20f;
            boolean open = Math.abs(a) < 0.45f;
            cut = Math.max(cut, (open ? 0.17f : 0.13f) * bump(d, mouthDir(a), open ? 0.02f : 0.007f));
        }
        r -= cut;
        return V.of(mx * r, my * r, mz * r);
    }

    /** Starved ribcage: deep rib ridges, sternum, shoulder blades, spine groove, hollow under the ribs. */
    public static V chestShape(V d) {
        float r = 1f;
        if (d.y() > -0.65f && d.y() < 0.8f) {
            float t = (d.y() + 0.65f) / 1.45f;
            float ridge = (float) Math.pow(Math.max(0, Math.sin(t * PI * 6f)), 3);
            r += 0.09f * ridge * (1f - Math.max(0f, d.z()));
        }
        if (d.z() < 0 && d.y() < 0.5f) r += 0.05f * (float) Math.exp(-(d.x() / 0.07f) * (d.x() / 0.07f)) * -d.z();
        r -= 0.14f * Math.max(0f, d.y() - 0.6f) * Math.max(0f, -d.z());
        r += 0.12f * bump(d, V.of(0.5, -0.4, 0.77).norm(), 0.05f);
        r += 0.12f * bump(d, V.of(-0.5, -0.4, 0.77).norm(), 0.05f);
        if (d.z() > 0) r -= 0.07f * (float) Math.exp(-(d.x() / 0.09f) * (d.x() / 0.09f));
        float mx = (1f + 0.06f * Math.max(0f, d.y())) * (1f + 0.1f * Math.max(0f, -d.y()));
        return V.of(mx * r, r, r);
    }

    // ------------------------------------------------------------------ animation

    private static float sin(float a) { return (float) Math.sin(a); }
    private static float cos(float a) { return (float) Math.cos(a); }
    private static float lerp(float a, float b, float t) { return a + (b - a) * t; }
    private static float clamp01(float v) { return Math.max(0f, Math.min(1f, v)); }

    /** Pose without memory (previews / first frame). */
    public void pose(float limbAngle, float limbDistance, float age, float headYawDeg, float headPitchDeg,
                     boolean hunting, boolean screaming, int id) {
        pose(limbAngle, limbDistance, age, headYawDeg, headPitchDeg, hunting, screaming, id, new AnimState());
    }

    /**
     * One frame of animation.
     *
     * Walking: a limping gait with heel-strike and toe-off, hips that bob, dip on the bad leg, sway and
     * rotate, a spine that counter-twists, and arms that drag and swing loosely behind the body (springs),
     * the forearms and hands lagging further still.
     * Its head is stabilised against all of that: the stare stays locked on you. It tracks you in
     * sudden snaps, not smoothly.
     * Standing: shallow breathing that lifts the shoulders, slow weight shifts onto one leg, fingers
     * curling, the slow sideways head tilt, and every few seconds a "glitch": a limb snaps to a wrong
     * angle for a split second and back.
     * Hunting: blends into a lunging sprint, arms raking at you, fingers grasping, lurches forward.
     * Screaming: whips down into your face with overshoot, jaw unhinging, arms flung out and wobbling.
     */
    public void pose(float limbAngle, float limbDistance, float age, float headYawDeg, float headPitchDeg,
                     boolean hunting, boolean screaming, int id, AnimState st) {
        st.tick(age);
        root.reset();
        float wHunt = st.smooth(0, hunting ? 1f : 0f, 0.12f);
        float wScream = st.smooth(1, screaming ? 1f : 0f, 0.6f);
        float wMove = st.smooth(2, Math.min(1f, limbDistance * 1.6f), 0.25f);
        float still = 1f - wMove;

        // ================================================================ gait
        float ph = limbAngle * (0.42f + 0.08f * wHunt);
        float p = ph + 0.3f * sin(ph * 2f);                          // the limp: uneven timing
        float amp = wMove * (1f + 0.55f * wHunt);
        for (int s = 0; s < 2; s++) {
            float ps = p + (s == 0 ? 0f : PI);
            float swing = cos(ps);                                   // +1 = leg forward
            float forwardMotion = Math.max(0f, -sin(ps));            // leg travelling forward (lifted)
            thigh[s].pitch -= swing * 0.5f * amp;
            shin[s].pitch += forwardMotion * 0.78f * amp + 0.12f * amp;
            foot[s].pitch += swing * 0.22f * amp - Math.max(0f, sin(ps)) * 0.38f * amp;  // heel strike, toe-off
        }
        float sw = sin(p);
        hips.oy -= (0.5f + 0.5f * cos(2f * p)) * 0.8f * amp;
        hips.oy += Math.max(0f, sw) * 0.55f * amp;                   // dips onto the bad leg
        hips.roll += sw * 0.07f * amp;
        hips.yaw += sw * 0.12f * amp;
        abdomen.yaw -= sw * 0.07f * amp;
        chest.yaw -= sw * 0.09f * amp;
        chest.roll -= sw * 0.03f * amp;
        abdomen.pitch += 0.06f * amp;

        // ================================================================ breathing, weight shift
        float breath = sin(age * 0.11f);
        chest.meshScaleX = 1f + 0.025f * breath;
        chest.meshScaleZ = 1f + 0.03f * breath;
        chest.pitch += 0.015f * breath;
        int wsBucket = (int) ((age + id * 13) / 110f);
        float shift = (AnimState.hash(wsBucket, 3) - 0.5f) * 2f;
        float sh = st.smooth(11, shift * still, 0.04f);
        hips.roll += sh * 0.06f;
        hips.oy += Math.abs(sh) * 0.35f;
        shin[sh > 0 ? 0 : 1].pitch += Math.abs(sh) * 0.18f;
        thigh[sh > 0 ? 0 : 1].pitch -= Math.abs(sh) * 0.08f;

        // ================================================================ arms: loose, dragging, lagging
        for (int s = 0; s < 2; s++) {
            float sg = s == 0 ? 1f : -1f;
            float target = -sg * cos(p) * 0.3f * amp + sin(age * 0.045f + s * 1.7f) * 0.05f;
            float armP = st.spring(3 + s, target, 0.32f, 0.5f);
            arm[s].pitch += armP;
            arm[s].roll += sg * sin(age * 0.031f + s) * 0.03f + sg * 0.04f * wMove;
            arm[s].oy -= breath * 0.25f * still;                     // shoulders rise with each breath
            float fore = st.spring(5 + s, armP * 0.55f, 0.22f, 0.42f);
            forearm[s].pitch += fore - 0.05f;
            float hnd = st.spring(7 + s, fore * 0.6f, 0.18f, 0.38f);
            hand[s].pitch += hnd;
        }

        // ================================================================ fingers
        for (int s = 0; s < 2; s++) {
            for (int i = 0; i < 5; i++) {
                float k = 0.5f + 0.5f * sin(age * 0.06f + i * 1.31f + s * 2.1f);
                finger[s][i].pitch -= 0.25f * k;
                fingerTip[s][i].pitch -= 0.35f * k;
                if ((age * 0.5f + i * 13 + s * 29 + id) % 90f < 1.2f) fingerTip[s][i].pitch -= 0.6f;
            }
        }

        // ================================================================ head: snapping, stabilised stare
        if (age >= st.nextSnap || Math.abs(headYawDeg - st.heldYaw) > 28f || dtJump(st)) {
            st.heldYaw = headYawDeg;
            st.heldPitch = headPitchDeg;
            st.nextSnap = age + 6f + AnimState.hash((int) age, id) * 18f;
        }
        float hy = st.spring(9, st.heldYaw * DEG, 0.9f, 1.05f);
        float hp = st.spring(10, st.heldPitch * DEG, 0.9f, 1.05f);
        head.yaw += hy;
        head.pitch += hp;
        float bodyYaw = (hips.yaw - hips.baseYaw) + (abdomen.yaw - abdomen.baseYaw) + (chest.yaw - chest.baseYaw);
        float bodyRoll = (hips.roll - hips.baseRoll) + (chest.roll - chest.baseRoll);
        head.yaw -= bodyYaw * 0.9f;
        head.roll -= bodyRoll * 0.8f;
        // cocked at a wrong angle, slowly; sometimes tilting almost sideways
        float tilt = 0.32f + 0.05f * sin(age * 0.027f);
        float cyc = (age + id * 31) % 400f;
        if (cyc < 80f) tilt += sin(cyc / 80f * PI) * 1.05f;
        head.roll += st.spring(15, tilt, 0.25f, 0.6f);
        neck.roll += (cyc < 80f ? sin(cyc / 80f * PI) * 0.25f : 0f);
        float tw = (age + id * 17) % 73f;
        if (tw < 1.5f) { head.roll += 0.5f; head.yaw += 0.25f; } else if (tw < 3f) head.roll -= 0.3f;

        // ================================================================ the glitch
        float gl = (age + id * 37) % 140f;
        int gb = (int) ((age + id * 37) / 140f);
        float gs = 20f + AnimState.hash(gb, 7) * 100f;
        if (wHunt < 0.5f && wScream < 0.5f && gl >= gs && gl < gs + 2.5f) {
            switch ((int) (AnimState.hash(gb, 8) * 5f)) {
                case 0 -> { arm[0].roll += 0.9f; forearm[0].pitch -= 1.2f; }
                case 1 -> head.roll += 1.3f;
                case 2 -> { chest.roll += 0.35f; neck.yaw += 0.5f; }
                case 3 -> { for (int i = 0; i < 5; i++) fingerTip[1][i].pitch += 1.3f; }
                default -> { shin[1].pitch += 0.8f; hips.oy += 1.0f; }
            }
        }

        // ================================================================ jaw and eyes
        jaw.pitch += 0.04f * (1f + sin(age * 0.9f));
        if ((age + id * 7) % 55f < 8f) jaw.pitch += 0.12f * Math.abs(sin(age * 2.6f));
        int bucket = (int) ((age + id * 11) / 37f);
        eyeL.yaw += st.spring(16, (AnimState.hash(bucket, 1) - 0.5f) * 0.35f, 1.4f, 1.6f);
        eyeL.pitch += st.spring(17, (AnimState.hash(bucket, 2) - 0.5f) * 0.2f, 1.4f, 1.6f);
        eyeR.yaw += sin(age * 0.05f) * 0.12f + ((age + id * 5) % 97f < 2f ? 0.4f : 0f);
        eyeR.pitch += cos(age * 0.037f) * 0.08f;

        // ================================================================ hunting: lunging sprint
        if (wHunt > 0.001f) {
            float w = wHunt;
            float lurch = AnimState.pulse((age + id * 3) % 34f, 0f, 5f);
            abdomen.pitch = lerp(abdomen.pitch, 0.75f + sin(2f * p) * 0.06f, w);
            chest.pitch = lerp(chest.pitch, 0.35f + lurch * 0.25f, w);
            neck.pitch = lerp(neck.pitch, 0.05f, w);
            head.pitch = lerp(head.pitch, -1.05f + hp - lurch * 0.2f, w);
            for (int s = 0; s < 2; s++) {
                float sg = s == 0 ? 1f : -1f;
                float rake = sin(p + s * PI);
                arm[s].pitch = lerp(arm[s].pitch, -2.3f + rake * 0.45f * amp, w);
                arm[s].roll = lerp(arm[s].roll, sg * (0.3f + 0.15f * Math.max(0f, rake)), w);
                forearm[s].pitch = lerp(forearm[s].pitch, -0.35f + cos(p + s * PI) * 0.4f * amp, w);
                hand[s].pitch = lerp(hand[s].pitch, -0.3f, w);
                float grasp = Math.max(0f, sin(p * 2f + s));
                for (int i = 0; i < 5; i++) {
                    finger[s][i].pitch = lerp(finger[s][i].pitch, -0.05f - 0.45f * grasp, w);
                    finger[s][i].roll = lerp(finger[s][i].roll, finger[s][i].baseRoll * 3f, w);
                    fingerTip[s][i].pitch = lerp(fingerTip[s][i].pitch, -0.2f - 0.5f * grasp, w);
                }
                thigh[s].pitch -= 0.35f * w;
                shin[s].pitch += 0.55f * w;
                foot[s].pitch -= 0.2f * w;
            }
            hips.oy += 1.6f * w;
            jaw.pitch = lerp(jaw.pitch, 0.5f + 0.3f * Math.abs(sin(age * 0.45f)) + 0.05f * sin(age * 3f), w);
        }

        // ================================================================ scream: whips into your face
        if (wScream > 0.001f || st.value(12) > 0.001f) {
            float L = st.spring(12, screaming ? 1f : 0f, 0.5f, 0.45f);     // overshoots: violent
            float A = st.spring(13, screaming ? 1f : 0f, 0.45f, 0.3f);     // arms wobble
            float Lc = clamp01(L);
            abdomen.pitch = lerp(abdomen.pitch, 0.95f, L);
            chest.pitch = lerp(chest.pitch, 0.45f, L);
            neck.pitch = lerp(neck.pitch, 0.35f, L);
            head.pitch = lerp(head.pitch, -1.55f + sin(age * 2.8f) * 0.1f, Lc);
            head.yaw += (sin(age * 2.3f) * 0.3f + sin(age * 5.1f) * 0.1f) * Lc;
            head.roll = lerp(head.roll, 0.15f + sin(age * 3.1f) * 0.25f, Lc);
            jaw.pitch = lerp(jaw.pitch, 1.4f + 0.12f * sin(age * 4f), Lc);
            for (int s = 0; s < 2; s++) {
                float sg = s == 0 ? 1f : -1f;
                arm[s].pitch = lerp(arm[s].pitch, -2.3f, Lc);
                arm[s].roll = lerp(arm[s].roll, sg * 1.25f, A);
                forearm[s].pitch = lerp(forearm[s].pitch, -0.8f, Lc);
                hand[s].pitch = lerp(hand[s].pitch, -0.4f, Lc);
                for (int i = 0; i < 5; i++) {
                    finger[s][i].pitch = lerp(finger[s][i].pitch, 0.15f, Lc);
                    finger[s][i].roll = lerp(finger[s][i].roll, finger[s][i].baseRoll * 4f, Lc);
                    fingerTip[s][i].pitch = lerp(fingerTip[s][i].pitch, 0.1f, Lc);
                }
                thigh[s].pitch -= 0.25f * Lc;
                shin[s].pitch += 0.4f * Lc;
            }
            hips.oy += 1.0f * Lc;
        }
    }

    private static boolean dtJump(AnimState st) {
        return st.dt == 0f;
    }
}
