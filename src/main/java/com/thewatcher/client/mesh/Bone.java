package com.thewatcher.client.mesh;

import java.util.ArrayList;
import java.util.List;

/**
 * One joint of the skeleton. Works like a vanilla ModelPart: translate to pivot (+offset),
 * rotate Z*Y*X (roll, yaw, pitch), draw the mesh, then the children.
 */
public final class Bone {
    public final String name;
    public final float px, py, pz;
    public final float basePitch, baseYaw, baseRoll;

    public float pitch, yaw, roll;
    public float ox, oy, oz;
    /** Scales only this bone's own mesh (breathing), not its children. */
    public float meshScaleX = 1f, meshScaleY = 1f, meshScaleZ = 1f;

    public float[] mesh = new float[0];
    public final List<Bone> children = new ArrayList<>();

    public Bone(String name, float px, float py, float pz, float basePitch, float baseYaw, float baseRoll) {
        this.name = name;
        this.px = px;
        this.py = py;
        this.pz = pz;
        this.basePitch = basePitch;
        this.baseYaw = baseYaw;
        this.baseRoll = baseRoll;
        reset();
    }

    public Bone add(Bone child) {
        children.add(child);
        return child;
    }

    /** Back to the rest pose (recursively). */
    public void reset() {
        pitch = basePitch;
        yaw = baseYaw;
        roll = baseRoll;
        ox = oy = oz = 0f;
        meshScaleX = meshScaleY = meshScaleZ = 1f;
        for (Bone c : children) c.reset();
    }
}
