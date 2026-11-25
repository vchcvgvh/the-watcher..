package com.thewatcher.client;

import com.thewatcher.client.mesh.Bone;
import com.thewatcher.client.mesh.HoundSkeleton;
import com.thewatcher.entity.HoundEntity;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public class HoundModel extends RigModel<HoundEntity> {
    private final HoundSkeleton skeleton = new HoundSkeleton();

    @Override
    protected Bone root() { return skeleton.root; }

    @Override
    public void setAngles(HoundEntity entity, float limbAngle, float limbDistance, float animationProgress,
                          float headYaw, float headPitch) {
        skeleton.pose(limbAngle, limbDistance, animationProgress, headYaw, headPitch, entity.isAttacking(),
                !entity.isOnGround(), entity.bodyYaw, entity.getId(), state(entity));
    }
}
