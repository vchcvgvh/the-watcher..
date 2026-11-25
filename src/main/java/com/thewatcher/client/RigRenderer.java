package com.thewatcher.client;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.client.render.entity.feature.EyesFeatureRenderer;
import net.minecraft.client.render.entity.model.EntityModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.util.Identifier;

/** Renderer for every creature in the mod: one shared generated texture atlas, glowing eyes, no shadow. */
@Environment(EnvType.CLIENT)
public class RigRenderer<T extends MobEntity, M extends EntityModel<T>> extends MobEntityRenderer<T, M> {
    private static final RenderLayer GLOW = RenderLayer.getEyes(WatcherTextures.GLOW);

    public RigRenderer(EntityRendererFactory.Context context, M model, float shadow) {
        super(context, model, shadow);
        this.addFeature(new EyesFeatureRenderer<T, M>(this) {
            @Override
            public RenderLayer getEyesTexture() {
                return GLOW;
            }
        });
    }

    @Override
    public void render(T entity, float yaw, float tickDelta, MatrixStack matrices,
                       VertexConsumerProvider vertexConsumers, int light) {
        WatcherTextures.ensure();
        super.render(entity, yaw, tickDelta, matrices, vertexConsumers, light);
    }

    @Override
    public Identifier getTexture(T entity) {
        return WatcherTextures.SKIN;
    }
}
