package com.thewatcher.client;

import com.thewatcher.client.mesh.Bone;
import com.thewatcher.client.mesh.FacelessSkeleton;
import com.thewatcher.entity.FacelessEntity;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public class FacelessModel extends RigModel<FacelessEntity> {
    private final FacelessSkeleton skeleton = new FacelessSkeleton();

    @Override
    protected Bone root() { return skeleton.root; }

    @Override
    public void setAngles(FacelessEntity entity, float limbAngle, float limbDistance, float animationProgress,
                          float headYaw, float headPitch) {
        skeleton.pose(limbDistance, animationProgress, headYaw, headPitch, entity.isFrozen(), entity.getFrozenPose(),
                entity.getId(), state(entity));
    }
}
