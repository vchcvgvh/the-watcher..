package com.thewatcher.client;

import com.thewatcher.client.mesh.Bone;
import com.thewatcher.client.mesh.WatcherSkeleton;
import com.thewatcher.entity.WatcherEntity;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public class WatcherModel extends RigModel<WatcherEntity> {
    private final WatcherSkeleton skeleton = new WatcherSkeleton();

    @Override
    protected Bone root() { return skeleton.root; }

    @Override
    public void setAngles(WatcherEntity entity, float limbAngle, float limbDistance, float animationProgress,
                          float headYaw, float headPitch) {
        skeleton.pose(limbAngle, limbDistance, animationProgress, headYaw, headPitch,
                entity.isHuntingClient(), entity.isScreaming(), entity.getId(), state(entity));
    }
}
