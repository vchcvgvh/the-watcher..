package com.thewatcher.client;

import com.thewatcher.entity.WatcherEntity;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.entity.EntityRendererFactory;

/** The Watcher's renderer (kept as its own class for older references; see {@link RigRenderer}). */
@Environment(EnvType.CLIENT)
public class WatcherRenderer extends RigRenderer<WatcherEntity, WatcherModel> {
    public WatcherRenderer(EntityRendererFactory.Context context) {
        super(context, new WatcherModel(), 0f);
    }
}
