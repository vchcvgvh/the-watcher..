package com.thewatcher.client;

import com.thewatcher.client.mesh.AnimState;
import com.thewatcher.client.mesh.Bone;
import com.thewatcher.client.mesh.MeshBuilder;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.entity.model.EntityModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.joml.Vector4f;

import java.util.HashMap;
import java.util.Map;

/**
 * Draws a sculpted skeleton mesh (see client/mesh) instead of vanilla boxes. Each bone is transformed
 * exactly like a vanilla ModelPart; its quads are streamed straight into the vertex consumer with smooth
 * normals and baked shading.
 */
@Environment(EnvType.CLIENT)
public abstract class RigModel<T extends Entity> extends EntityModel<T> {
    private final Quaternionf rot = new Quaternionf();
    private final Vector4f pos = new Vector4f();
    private final Vector3f nrm = new Vector3f();

    private final Map<Integer, AnimState> states = new HashMap<>();
    private long frame;

    protected abstract Bone root();

    /** Animation memory for this particular creature (springs, blends, timers). */
    protected AnimState state(Entity entity) {
        AnimState st = states.computeIfAbsent(entity.getId(), k -> new AnimState());
        st.lastTouched = ++frame;
        if (states.size() > 64) states.values().removeIf(s -> frame - s.lastTouched > 20000);
        return st;
    }

    @Override
    public void render(MatrixStack matrices, VertexConsumer vertices, int light, int overlay, int color) {
        renderBone(root(), matrices, vertices, light, overlay, color);
    }

    private void renderBone(Bone bone, MatrixStack matrices, VertexConsumer vc, int light, int overlay, int color) {
        matrices.push();
        matrices.translate((bone.px + bone.ox) / 16f, (bone.py + bone.oy) / 16f, (bone.pz + bone.oz) / 16f);
        if (bone.pitch != 0f || bone.yaw != 0f || bone.roll != 0f) {
            matrices.multiply(rot.rotationZYX(bone.roll, bone.yaw, bone.pitch));
        }
        if (bone.mesh.length > 0) {
            boolean scaled = bone.meshScaleX != 1f || bone.meshScaleY != 1f || bone.meshScaleZ != 1f;
            if (scaled) {
                matrices.push();
                matrices.scale(bone.meshScaleX, bone.meshScaleY, bone.meshScaleZ);
            }
            emit(bone.mesh, matrices.peek(), vc, light, overlay, color);
            if (scaled) matrices.pop();
        }
        for (Bone child : bone.children) {
            renderBone(child, matrices, vc, light, overlay, color);
        }
        matrices.pop();
    }

    private void emit(float[] mesh, MatrixStack.Entry entry, VertexConsumer vc, int light, int overlay, int color) {
        Matrix4f pm = entry.getPositionMatrix();
        Matrix3f nm = entry.getNormalMatrix();
        int a = color >>> 24, r = (color >> 16) & 0xFF, g = (color >> 8) & 0xFF, b = color & 0xFF;
        for (int i = 0; i < mesh.length; i += MeshBuilder.STRIDE) {
            pm.transform(pos.set(mesh[i] / 16f, mesh[i + 1] / 16f, mesh[i + 2] / 16f, 1f));
            nm.transform(nrm.set(mesh[i + 5], mesh[i + 6], mesh[i + 7])).normalize();
            // baked shadows (sockets, creases), tint (hair) and blood stains, per vertex
            int c = (a << 24)
                    | (Math.min(255, (int) (r * mesh[i + 8])) << 16)
                    | (Math.min(255, (int) (g * mesh[i + 9])) << 8)
                    | Math.min(255, (int) (b * mesh[i + 10]));
            vc.vertex(pos.x(), pos.y(), pos.z(), c, mesh[i + 3], mesh[i + 4], overlay, light,
                    nrm.x(), nrm.y(), nrm.z());
        }
    }
}
