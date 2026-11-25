package com.thewatcher.client;

import com.thewatcher.entity.ModEntities;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;

@Environment(EnvType.CLIENT)
public class TheWatcherClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        WatcherConfig.load();
        WatcherTextures.startPainting();
        net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.registerGlobalReceiver(
                com.thewatcher.net.ScarePayload.ID, (payload, context) -> context.client().execute(() -> ScareOverlay.start(payload)));
        net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback.EVENT.register(Dread::render);
        net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback.EVENT.register(ScareOverlay::render);
        net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents.END_CLIENT_TICK.register(Dread::tick);
        net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents.END_CLIENT_TICK.register(ScareOverlay::tick);
        EntityRendererRegistry.register(ModEntities.WATCHER, ctx -> new RigRenderer<>(ctx, new WatcherModel(), 0f));
        EntityRendererRegistry.register(ModEntities.CRAWLER, ctx -> new RigRenderer<>(ctx, new CrawlerModel(), 0.6f));
        EntityRendererRegistry.register(ModEntities.FACELESS, ctx -> new RigRenderer<>(ctx, new FacelessModel(), 0f));
        EntityRendererRegistry.register(ModEntities.HOUND, ctx -> new RigRenderer<>(ctx, new HoundModel(), 0.5f));
    }
}
