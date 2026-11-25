package com.thewatcher.client;

import com.thewatcher.client.mesh.Bone;
import com.thewatcher.client.mesh.CrawlerSkeleton;
import com.thewatcher.entity.CrawlerEntity;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public class CrawlerModel extends RigModel<CrawlerEntity> {
    private final CrawlerSkeleton skeleton = new CrawlerSkeleton();

    @Override
    protected Bone root() { return skeleton.root; }

    @Override
    public void setAngles(CrawlerEntity entity, float limbAngle, float limbDistance, float animationProgress,
                          float headYaw, float headPitch) {
        skeleton.pose(limbAngle, limbDistance, animationProgress, headYaw, headPitch, entity.isAttacking(),
                entity.isClimbing(), !entity.isOnGround(), entity.bodyYaw, entity.getId(), state(entity));
    }
}
