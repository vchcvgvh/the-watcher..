package com.thewatcher.client.mesh;

import java.util.Random;

import static com.thewatcher.client.mesh.MeshBuilder.Swatch.BONE;
import static com.thewatcher.client.mesh.MeshBuilder.Swatch.FLESH;
import static com.thewatcher.client.mesh.MeshBuilder.Swatch.KERATIN;
import static com.thewatcher.client.mesh.MeshBuilder.Swatch.SKIN;
import static com.thewatcher.client.mesh.MeshBuilder.bump;

/**
 * The Crawler: a starved human body bent backwards into a spider-walk. Its chest faces the sky, its four
 * limbs arch up over it and stab down to the ground, and its head hangs upside down so the torn grin
 * (the same face as the Watcher's) is on top. ~1.4 blocks tall, very wide.
 */
public final class CrawlerSkeleton implements Rig {
    private static final float PI = (float) Math.PI;

    public final Bone root, body, neck, head, jaw, eyeL, eyeR;
    /** limb[i][segment]: 0 right arm, 1 left arm, 2 right leg, 3 left leg. */
    public final Bone[][] limb = new Bone[4][];
    public final Bone[] paw = new Bone[4];

    @Override
    public Bone root() { return root; }

    public CrawlerSkeleton() {
        Random rnd = new Random(0xC4A111);
        root = new Bone("root", 0, 0, 0, 0, 0, 0);

        // ---- body, chest up: ribs on top, spine underneath
        body = root.add(new Bone("body", 0, 11f, 0, 0, 0, 0));
        MeshBuilder b = new MeshBuilder().rough(0.14f, 0.8f).ao(1.5f)
                .blood(V.of(0, -2.6, -3.0), V.of(2.8, 1.8, 4.6), true, 0.85f)        // around the split chest
                .blood(V.of(-2.7, -0.2, -3.0), V.of(1.0, 2.6, 3.2), false, 0.75f)    // running down its sides
                .blood(V.of(2.7, -0.2, -3.0), V.of(1.0, 2.6, 3.2), false, 0.75f)
                .ellipsoid(V.of(0, 0, -3), 4.2f, 3.0f, 5.0f, 26, 32, SKIN, d -> {
                    float r = 1f;
                    if (d.y() < 0.3f && Math.abs(d.z()) < 0.75f) {                 // ribs, facing up
                        float t = (d.z() + 0.75f) / 1.5f;
                        r += 0.1f * (float) Math.pow(Math.max(0, Math.sin(t * PI * 5f)), 3) * Math.min(1f, (0.3f - d.y()));
                    }
                    if (d.y() < 0) r += 0.05f * (float) Math.exp(-(d.x() / 0.08f) * (d.x() / 0.08f)); // sternum
                    return V.of(r, r, r);
                })
                .tube(V.of(0, 0.3, 1.0), V.of(0, 0.4, 7.0), 2.7f, 2.4f, SKIN, 16, 7, -0.28f, 1f, 0.75f, V.of(1, 0, 0), false, false)
                .ellipsoid(V.of(0, 0.2, 8.2), 3.3f, 2.1f, 2.3f, 12, 16, SKIN, d -> {
                    float r = 1f + 0.2f * bump(d, V.of(0.8, -0.5, 0).norm(), 0.05f) + 0.2f * bump(d, V.of(-0.8, -0.5, 0).norm(), 0.05f);
                    return V.of(r, r, r);
                });
        for (int k = 0; k < 11; k++) {                                               // spine, underneath
            float z = -7.5f + k * 1.6f;
            float y = z < 1.5f ? 3.0f * (float) Math.sqrt(Math.max(0.05, 1 - Math.pow((z + 3) / 5.0, 2))) : 1.9f;
            b.ellipsoid(V.of(0, y + 0.15f, z), 0.55f, 0.5f, 0.55f, SKIN);
        }
        b.ellipsoid(V.of(2.6, -1.0, -1.5), 1.0f, 1.6f, 1.0f, 8, 10, FLESH, null);   // sore, split open
        // its chest is split open from collarbone to belly: raw meat, the ribs pulled apart
        b.tint(0.55f).rough(0.28f, 2f).ao(2f)
                .ellipsoid(V.of(0, -2.55, -3.0), 1.35f, 0.7f, 3.6f, 10, 16, FLESH, null)
                .tint(1f).rough(0.05f, 2f).ao(1.2f);
        for (int k = 0; k < 5; k++) {
            float z = -5.6f + k * 1.3f;
            for (int sd = -1; sd <= 1; sd += 2) {                                    // broken ribs, splayed outwards
                b.tube(V.of(sd * 0.4, -2.9, z), V.of(sd * 2.1, -3.0 + 0.4 * Math.abs(k - 2) * 0.2, z + 0.2), 0.22f, 0.18f, BONE, 6, 4, 0f, 1f, 1f, null, true, true);
            }
        }
        for (int k = 0; k < 14; k++) {                                              // torn edges
            float z = -6.4f + k * 0.52f;
            for (int sd = -1; sd <= 1; sd += 2) {
                float w = 1.45f + (float) Math.sin(k * 1.7) * 0.15f;
                b.rough(0.12f, 2.5f).ellipsoid(V.of(sd * w, -2.7, z), 0.32f, 0.3f, 0.34f, 5, 7, FLESH, null);
            }
        }
        Gore.wound(b, V.of(3.95, 0.2, -3.2), V.of(1, 0.1, 0), 1.3f, rnd);                 // its sides are torn open...
        Gore.entrails(b, V.of(4.1, 0.4, -3.2), V.of(1, 0.15, 0), 9f, 0.45f, rnd);          // ...and its guts hang out
        Gore.wound(b, V.of(-3.8, 0.6, -1.6), V.of(-1, 0.1, 0.1), 1.0f, rnd);
        Gore.entrails(b, V.of(-3.95, 0.8, -1.6), V.of(-1, 0.15, 0.1), 6.5f, 0.36f, rnd);
        Gore.flayed(b, V.of(1.1, -2.35, 4.3), V.of(0.35, -1, 0), V.of(0, 0, 1), 1.5f, rnd);  // skinned haunch
        Gore.drip(b, V.of(-2.5, -0.5, -3.5), V.of(0, 1, 0), 2.5f, 0.12f, rnd);
        Gore.drip(b, V.of(2.4, -0.3, -1.8), V.of(0, 1, 0), 3.2f, 0.12f, rnd);
        body.mesh = b.build();

        // ---- neck stretching forward, head hanging upside down
        neck = body.add(new Bone("neck", 0, -1.0f, -7.5f, 0, 0, 0));
        neck.mesh = new MeshBuilder().rough(0.1f, 0.9f).ao(1.4f)
                .tube(V.of(0, 0, 0.5), V.of(0, 1.5, -4.5), 1.3f, 1.05f, SKIN, 12, 6, -0.1f, 1f, 1f, V.of(1, 0, 0), false, false)
                .tube(V.of(-0.8, 0, 0), V.of(-0.35, 1.2, -4.2), 0.3f, 0.3f, SKIN, 6, 5, 0f, 1f, 1f, null, true, true)
                .tube(V.of(0.8, 0, 0), V.of(0.35, 1.2, -4.2), 0.3f, 0.3f, SKIN, 6, 5, 0f, 1f, 1f, null, true, true)
                .build();
        head = neck.add(new Bone("head", 0, 1.5f, -4.5f, -0.35f, 0, PI));
        // head hangs upside down: blood from its mouth runs over its face towards the top of its skull
        MeshBuilder hd = new MeshBuilder()
                .blood(V.of(0, -4.0, -3.0), V.of(1.6, 3.4, 1.2), false, 0.75f);
        WatcherSkeleton.addFace(hd, rnd);
        hd.bloodReset(0);
        for (int k = 0; k < 3; k++) {
            float x = -0.8f + k * 0.8f;
            Gore.drip(hd, V.of(x, -9.2 - k * 0.2, -1.0 + k * 0.3), V.of(0, -1, 0), 2.0f + rnd.nextFloat() * 2.5f, 0.11f, rnd);
        }
        head.mesh = hd.build();
        Bone[] eyes = WatcherSkeleton.addEyes(head);
        eyeL = eyes[0];
        eyeR = eyes[1];
        jaw = WatcherSkeleton.addJaw(head, rnd);

        // ---- four limbs: arch up and out, then stab down to the ground
        for (int i = 0; i < 4; i++) {
            boolean arm = i < 2;
            float sx = (i % 2 == 0) ? -1f : 1f;
            V start = arm ? V.of(sx * 4.0f, 0.2f, -5.6f) : V.of(sx * 3.0f, 0.6f, 8.0f);
            V up = arm ? V.of(sx * 0.6f, -0.75f, -0.28f) : V.of(sx * 0.55f, -0.7f, 0.45f);
            V down = arm ? V.of(sx * 0.15f, 0.98f, -0.14f) : V.of(sx * 0.15f, 0.98f, 0.1f);
            V[] dirs = {up, down};
            float[] lens = arm ? new float[]{11f, 20f} : new float[]{11f, 19f};
            float[] radii = arm ? new float[]{1.25f, 0.9f, 0.55f} : new float[]{1.5f, 1.05f, 0.6f};
            limb[i] = RigMath.chain(body, (arm ? "arm" : "leg") + i, start, dirs, lens, radii, SKIN, 1f, 0.12f);

            // claw-hand / claw-foot: splayed fingers gripping the ground
            Bone end = limb[i][1];
            paw[i] = end.add(new Bone("paw" + i, 0, lens[1], 0, 0, 0, 0));
            // bone stabbing out of each elbow/knee
            Bone upper = limb[i][0];
            upper.mesh = concat(upper.mesh, Gore.boneSpur(V.of(0, lens[0] - 0.6f, 0.4f), V.of(0, 0.35, 1), 1.9f, 0.24f, rnd));
            MeshBuilder pm = new MeshBuilder().rough(0.05f, 1.5f).ao(1.3f)
                    .blood(V.of(0, 2.0, 0), 2.8f, true, 0.65f)
                    .ellipsoid(V.of(0, 0.6, 0), 0.9f, 0.8f, 0.9f, 7, 10, SKIN, null);
            for (int f = 0; f < 4; f++) {
                double ang = -0.9 + f * 0.6;
                V dir = V.of(Math.sin(ang) * 0.8, 0.45, -Math.cos(ang) * 0.8).norm();
                V knuckle = V.of(0, 1.0, 0).add(dir.mul(2.2f));
                pm.tube(V.of(0, 0.8, 0), knuckle, 0.3f, 0.24f, SKIN, 6, 3, 0f, 1f, 1f, null, false, true);
                pm.tint(1f).cone(knuckle, knuckle.add(V.of(dir.x(), 0.9, dir.z()).norm().mul(2.0f)), 0.22f, KERATIN).tint(1f);
            }
            paw[i].mesh = pm.build();
        }
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
        pose(limbAngle, limbDistance, age, headYawDeg, headPitchDeg, attacking, false, false, 0f, id, new AnimState());
    }

    /**
     * Skitter: each limb snaps forward through the air in a fast arc, then plants and drags back slowly,
     * diagonal pairs together, so it moves in bursty, insect-like jerks. The body bobs and leans into turns.
     * The upside-down head tracks you in twitchy snaps, its jaw chattering, and now and then it turns all the
     * way around. Idle: fingers drum, the body sways, a sudden shudder. Attacking: it rears up on its back
     * limbs and claws with the front ones. Climbing: body tips up the wall. Pouncing: limbs splay out.
     */
    public void pose(float limbAngle, float limbDistance, float age, float headYawDeg, float headPitchDeg,
                     boolean attacking, boolean climbing, boolean airborne, float bodyYawDeg, int id, AnimState st) {
        st.tick(age);
        st.bodyYaw(bodyYawDeg);
        root.reset();
        float wMove = st.smooth(0, Math.min(1f, limbDistance * 1.5f), 0.3f);
        float wClimb = st.smooth(1, climbing ? 1f : 0f, 0.2f);
        float wAtk = st.smooth(2, attacking ? 1f : 0f, 0.25f);
        float wAir = st.smooth(3, airborne ? 1f : 0f, 0.4f);
        float still = 1f - wMove;
        float turn = Math.max(-1f, Math.min(1f, st.turnRate / 20f));

        // ================================================================ skitter gait
        float ph = limbAngle * 1.05f;
        float[] offs = {0f, PI, PI, 0f};
        for (int i = 0; i < 4; i++) {
            float sx = (i % 2 == 0) ? -1f : 1f;
            float u = (float) (((ph + offs[i]) / (2 * Math.PI)) % 1.0);
            if (u < 0) u += 1f;
            float fwd, lift;
            if (u < 0.32f) {                                  // swing: fast arc through the air
                float t = u / 0.32f;
                fwd = -cos(t * PI);
                lift = sin(t * PI);
            } else {                                          // stance: planted, dragging back
                float t = (u - 0.32f) / 0.68f;
                fwd = 1f - 2f * t;
                lift = 0f;
            }
            float stride = 0.42f * (1f + sx * turn * 0.35f);
            limb[i][0].pitch -= fwd * stride * wMove;
            limb[i][0].roll += -sx * lift * 0.5f * wMove;
            limb[i][1].roll += sx * lift * 0.45f * wMove;
            paw[i].pitch += lift * 0.6f * wMove;
            // idle: fingers drumming, limbs shifting
            float drum = AnimState.pulse((age * 1.3f + i * 11 + id) % 40f, 0f, 3f);
            paw[i].pitch -= drum * 0.35f * still;
            limb[i][1].roll += sx * AnimState.pulse((age + i * 29 + id * 7) % 120f, 0f, 6f) * 0.12f * still;
        }
        body.oy -= Math.abs(sin(ph * 2f)) * 0.55f * wMove;
        body.yaw += sin(ph * 2f) * 0.05f * wMove;
        body.roll += -turn * 0.18f + st.smooth(11, (AnimState.hash((int) ((age + id) / 70f), 4) - 0.5f) * 0.12f * still, 0.05f);
        body.meshScaleY = 1f + 0.04f * sin(age * 0.16f);
        float shudder = AnimState.pulse((age + id * 19) % 160f, 0f, 5f);
        body.roll += sin(age * 4.3f) * 0.06f * shudder;
        body.pitch += sin(age * 3.7f) * 0.04f * shudder;

        // ================================================================ head
        if (age >= st.nextSnap || Math.abs(headYawDeg - st.heldYaw) > 30f || st.dt == 0f) {
            st.heldYaw = headYawDeg;
            st.heldPitch = headPitchDeg;
            st.nextSnap = age + 3f + AnimState.hash((int) age, id) * 8f;
        }
        head.yaw -= st.spring(8, st.heldYaw * (PI / 180f) * 0.7f, 1.2f, 1.1f);   // upside down: mirrored
        head.pitch -= st.spring(9, st.heldPitch * (PI / 180f) * 0.5f, 1.2f, 1.1f);
        head.roll += sin(age * 0.05f) * 0.25f * still;
        // every so often it turns its head all the way around
        float spinT = (age + id * 41) % 240f;
        if (spinT < 14f) {
            float t = spinT / 14f;
            head.roll += (float) (2 * Math.PI) * (t * t * (3 - 2 * t));
        }
        if ((age + id * 13) % 45f < 2f) head.roll += 0.9f;
        jaw.pitch += 0.15f + 0.05f * sin(age * 1.3f) + 0.12f * wMove * Math.abs(sin(age * 2.4f)); // chattering
        eyeL.yaw += sin(age * 0.21f) * 0.25f;
        eyeR.pitch += cos(age * 0.17f) * 0.2f;

        // ================================================================ attack: rear up and claw
        if (wAtk > 0.001f) {
            body.pitch -= 0.45f * wAtk;
            body.oy -= 1.5f * wAtk;
            for (int i = 0; i < 2; i++) {
                float sx = i == 0 ? -1f : 1f;
                float strike = Math.max(0f, sin(age * 0.6f + i * PI));
                limb[i][0].pitch -= (0.6f + 0.3f * strike) * wAtk;
                limb[i][0].roll += -sx * 0.25f * wAtk;
                limb[i][1].pitch -= strike * 0.9f * wAtk;
            }
            neck.oz -= 1.5f * wAtk * Math.abs(sin(age * 0.6f));
            jaw.pitch += 0.7f * wAtk * Math.abs(sin(age * 0.9f));
        }

        // ================================================================ climbing a wall
        if (wClimb > 0.001f) {
            body.pitch = lerp(body.pitch, -1.35f, wClimb);
            head.pitch += 0.8f * wClimb;
            for (int i = 0; i < 4; i++) {
                float sx = (i % 2 == 0) ? -1f : 1f;
                limb[i][0].roll += -sx * 0.25f * wClimb;
            }
        }

        // ================================================================ pounce: splayed in the air
        if (wAir > 0.001f) {
            for (int i = 0; i < 4; i++) {
                float sx = (i % 2 == 0) ? -1f : 1f;
                limb[i][0].pitch += (i < 2 ? -0.8f : 0.6f) * wAir;
                limb[i][0].roll += -sx * 0.35f * wAir;
                limb[i][1].roll -= sx * 0.3f * wAir;
            }
            jaw.pitch += 0.8f * wAir;
        }
    }
}
