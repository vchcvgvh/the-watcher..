package com.thewatcher.client;

import com.thewatcher.ModSounds;
import com.thewatcher.entity.CrawlerEntity;
import com.thewatcher.entity.FacelessEntity;
import com.thewatcher.entity.HoundEntity;
import com.thewatcher.entity.ModEntities;
import com.thewatcher.entity.WatcherEntity;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;

import java.util.Random;

/**
 * Always-on dread, growing with your stage:
 * - the edges of the screen close in, darker the further gone you are;
 * - something stands at the edge of your vision for a split second, then isn't there;
 * - the picture tears and glitches;
 * - at the last stage you can hear your own heartbeat.
 * And when one of them is actually near you: the edges pulse red, the heartbeat races and your view trembles.
 */
@Environment(EnvType.CLIENT)
public final class Dread {
    private Dread() {}

    private static int stage = 0;
    private static float prox = 0f, proxSmooth = 0f;
    private static int heartbeatIn = 0, apparition = 0, glitch = 0, age = 0;
    private static float appX;
    private static boolean appFaceless;
    private static LivingEntity watcherDummy, facelessDummy;
    private static ClientWorld dummyWorld;
    private static final Random R = new Random();

    public static void setStage(int s) { stage = Math.max(0, Math.min(4, s)); }

    public static void tick(MinecraftClient mc) {
        if (mc.world == null || mc.player == null || mc.isPaused()) return;
        age++;
        // how close is the nearest one of them?
        double best = 40;
        for (Entity e : mc.world.getEntities()) {
            if (e instanceof WatcherEntity || e instanceof FacelessEntity || e instanceof CrawlerEntity || e instanceof HoundEntity) {
                best = Math.min(best, e.distanceTo(mc.player));
            }
        }
        prox = (float) Math.max(0, 1 - best / 32.0);
        proxSmooth += (prox - proxSmooth) * 0.1f;

        // heartbeat: at the last stage always, and racing when something is close
        float urgency = Math.max(proxSmooth, stage >= 4 ? 0.25f : 0f);
        if (urgency > 0.15f && --heartbeatIn <= 0) {
            ScareOverlay.play(mc, ModSounds.HEARTBEAT, 0.9f + urgency * 0.3f, 0.2f + 0.6f * urgency);
            heartbeatIn = (int) (34 - 24 * urgency);
        }
        // your view trembles when it's right there
        if (proxSmooth > 0.55f) {
            float k = (proxSmooth - 0.55f) * 0.9f;
            mc.player.setPitch(mc.player.getPitch() + (R.nextFloat() - 0.5f) * k);
            mc.player.setYaw(mc.player.getYaw() + (R.nextFloat() - 0.5f) * k * 0.6f);
        }
        // something at the edge of your vision
        if (apparition > 0) apparition--;
        else if (stage >= 2 && WatcherConfig.popupText && R.nextFloat() < (stage - 1) / (20f * 110f)) {
            apparition = 3;
            appX = R.nextBoolean() ? 0.07f + R.nextFloat() * 0.05f : 0.88f + R.nextFloat() * 0.05f;
            appFaceless = R.nextInt(3) == 0;
            ScareOverlay.play(mc, ModSounds.TICK, 0.5f, 0.35f);
        }
        // the picture tears
        if (glitch > 0) glitch--;
        else if (stage >= 3 && R.nextFloat() < 1f / (20f * (stage >= 4 ? 25f : 50f))) {
            glitch = 2 + R.nextInt(3);
            ScareOverlay.play(mc, ModSounds.GLITCH, 0.8f + R.nextFloat() * 0.5f, 0.25f);
        }
    }

    private static LivingEntity dummy(MinecraftClient mc, boolean faceless) {
        if (dummyWorld != mc.world) { watcherDummy = null; facelessDummy = null; dummyWorld = mc.world; }
        if (faceless) {
            if (facelessDummy == null) facelessDummy = ModEntities.FACELESS.create(mc.world);
            return facelessDummy;
        }
        if (watcherDummy == null) watcherDummy = ModEntities.WATCHER.create(mc.world);
        return watcherDummy;
    }

    public static void render(DrawContext ctx, RenderTickCounter tc) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.world == null || mc.player == null || mc.options.hudHidden) return;
        int w = ctx.getScaledWindowWidth(), h = ctx.getScaledWindowHeight();
        float time = age + tc.getTickDelta(false);

        // a figure standing at the edge of the screen, for three frames
        if (apparition > 0) {
            LivingEntity e = dummy(mc, appFaceless);
            if (e != null) {
                int x = Math.round(w * appX), y = Math.round(h * 0.55f);
                InventoryScreen.drawEntity(ctx, x - 60, y - h, x + 60, y + h, Math.round(h * 0.22f), 0f, x, y, e);
            }
        }

        ctx.getMatrices().push();
        ctx.getMatrices().translate(0, 0, 200);
        // the edges close in
        float v = new float[]{0f, 0.18f, 0.32f, 0.46f, 0.6f}[stage] + 0.3f * proxSmooth;
        if (v > 0.01f) edges(ctx, w, h, Math.min(0.92f, v), 0x000000);
        // red pulse when one of them is near
        if (proxSmooth > 0.1f) {
            float beat = (float) Math.pow(Math.abs(Math.sin(time * (0.12f + 0.25f * proxSmooth))), 6);
            edges(ctx, w, h, Math.min(0.7f, proxSmooth * (0.25f + 0.45f * beat)), 0x8A0000);
        }
        // screen tear
        if (glitch > 0) {
            for (int i = 0; i < 4; i++) {
                int y = R.nextInt(h), hh = 1 + R.nextInt(6);
                ctx.fill(0, y, w, y + hh, ((60 + R.nextInt(120)) << 24) | (R.nextBoolean() ? 0xFFFFFF : 0x000000));
                int off = R.nextInt(w);
                ctx.fill(off, y + hh, Math.min(w, off + 40 + R.nextInt(120)), y + hh + 2, 0xA0FF0000 | R.nextInt(0xFF));
            }
        }
        ctx.getMatrices().pop();
    }

    /** Darkens towards the edges of the screen (strength 0..1). */
    private static void edges(DrawContext ctx, int w, int h, float strength, int rgb) {
        int a = (int) (255 * strength);
        ctx.fillGradient(0, 0, w, h / 3, (a << 24) | rgb, rgb);
        ctx.fillGradient(0, h - h / 3, w, h, rgb, (a << 24) | rgb);
        int strips = 20, side = w / 4;
        for (int i = 0; i < strips; i++) {
            int alpha = (int) (a * Math.pow(1 - i / (float) strips, 2));
            int x0 = side * i / strips, x1 = side * (i + 1) / strips;
            ctx.fill(x0, 0, x1, h, (alpha << 24) | rgb);
            ctx.fill(w - x1, 0, w - x0, h, (alpha << 24) | rgb);
        }
    }
}
