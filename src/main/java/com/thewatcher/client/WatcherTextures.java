package com.thewatcher.client;

import com.thewatcher.TheWatcher;
import com.thewatcher.client.mesh.TexturePainter;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.client.texture.TextureManager;
import net.minecraft.util.Identifier;

/**
 * The mod ships no image files: {@link TexturePainter} paints the skin, bone, flesh, hair, scalp and eyes in code,
 * and this uploads them to the GPU the first time a Watcher is drawn.
 */
@Environment(EnvType.CLIENT)
public final class WatcherTextures {
    private WatcherTextures() {}

    public static final Identifier SKIN = TheWatcher.id("generated/watcher_skin");
    public static final Identifier GLOW = TheWatcher.id("generated/watcher_glow");

    private static java.util.concurrent.CompletableFuture<int[][]> painting;

    /** Start painting in the background when the game starts (takes a few seconds, once). */
    public static synchronized void startPainting() {
        if (painting == null) {
            painting = java.util.concurrent.CompletableFuture.supplyAsync(
                    () -> new int[][]{TexturePainter.paint(false), TexturePainter.paint(true)});
        }
    }

    /** Makes sure both textures exist (also survives F3+T resource reloads). Call on the render thread. */
    public static void ensure() {
        TextureManager tm = MinecraftClient.getInstance().getTextureManager();
        if (tm.getOrDefault(SKIN, null) != null && tm.getOrDefault(GLOW, null) != null) return;
        startPainting();
        int[][] img = painting.join();
        if (tm.getOrDefault(SKIN, null) == null) tm.registerTexture(SKIN, upload(img[0]));
        if (tm.getOrDefault(GLOW, null) == null) tm.registerTexture(GLOW, upload(img[1]));
    }

    private static NativeImageBackedTexture upload(int[] argb) {
        int w = TexturePainter.WIDTH, h = TexturePainter.HEIGHT;
        NativeImage img = new NativeImage(w, h, true);
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int c = argb[y * w + x];
                int a = c >>> 24, r = (c >> 16) & 0xFF, g = (c >> 8) & 0xFF, b = c & 0xFF;
                img.setColor(x, y, (a << 24) | (b << 16) | (g << 8) | r); // NativeImage wants ABGR
            }
        }
        return new NativeImageBackedTexture(img);
    }
}
