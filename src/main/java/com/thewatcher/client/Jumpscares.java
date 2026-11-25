package com.thewatcher.client;

import com.thewatcher.ModSounds;
import com.thewatcher.entity.FacelessEntity;
import com.thewatcher.entity.HoundEntity;
import com.thewatcher.entity.WatcherEntity;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.math.RotationAxis;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Four different jumpscares, one per creature, drawn with the creature's real 3D model:
 * <ul>
 *   <li>WATCHER, "turn around": it fades out of the dark, breathing... then snaps into your face, jaw unhinged,
 *       screaming; blood spatters the screen and runs down it.</li>
 *   <li>FACELESS, "don't blink": strobe cuts - every time the screen flickers back it's somewhere else, frozen in
 *       a different pose, closer. Then its blank face fills the screen and its neck snaps sideways.</li>
 *   <li>CRAWLER, "from above": dust falls, then it drops from the top of the screen, its upside-down head first,
 *       bouncing on its limbs, and scuttles at you in jerky stop-motion bursts.</li>
 *   <li>HOUND, "the charge": it gallops at you out of the dark, getting huge, and bites: the screen jolts,
 *       claw gashes rip across it and blood sprays.</li>
 * </ul>
 */
@Environment(EnvType.CLIENT)
final class Jumpscares {
    private Jumpscares() {}

    /**
     * Where to aim on the model (blocks up from its middle) so its face is centred - measured from the real
     * meshes. The Watcher drops into a crouch when it screams and the Hound lowers its head to bite.
     */
    private static final float WATCHER_STAND = 1.55f, WATCHER_SCREAM = 0.45f, FACELESS = 1.25f, CRAWLER = 0.36f,
            HOUND_RUN = 0.58f, HOUND_BITE = 0.48f;
    private static final int[] DURATION = {48, 56, 44, 50};

    private static final List<float[]> drops = new ArrayList<>();   // x, y, r (fractions of screen h), spawn tick, run speed, max run
    private static final List<float[]> slashes = new ArrayList<>();
    private static final List<float[]> dust = new ArrayList<>();

    static int duration(int v) { return DURATION[v]; }

    static void begin(int v, LivingEntity e, Random r) {
        drops.clear(); slashes.clear(); dust.clear();
        if (e instanceof WatcherEntity w) w.setScreamingLocal(false);
        if (e instanceof FacelessEntity f) f.setFrozenLocal(true, 0);
        if (e instanceof HoundEntity h) h.setAttacking(false);
        if (v == 2) for (int i = 0; i < 60; i++) dust.add(new float[]{r.nextFloat(), -r.nextFloat() * 0.6f, 0.002f + r.nextFloat() * 0.006f, 0.6f + r.nextFloat()});
    }

    // ------------------------------------------------------------------------------------------ sound & state cues

    static void tick(MinecraftClient mc, int v, int t, LivingEntity e, Random r) {
        switch (v) {
            case 0 -> {
                if (t == 1) play(mc, ModSounds.BREATHING, 0.85f, 0.9f);
                if (t == 12) {
                    if (e instanceof WatcherEntity w) w.setScreamingLocal(true);
                    play(mc, ModSounds.JUMPSCARE, 1f, 1f); play(mc, ModSounds.WATCHER_SCREAM, 1f, 1f); play(mc, ModSounds.STATIC, 1f, 0.5f);
                    splatter(r, 12, 9);
                }
            }
            case 1 -> {
                if (t == 1) play(mc, ModSounds.FACELESS_HUM, 0.8f, 0.8f);
                int[][] cuts = {{8, 2}, {18, 4}, {27, 5}};
                for (int[] c : cuts) if (t == c[0]) {
                    if (e instanceof FacelessEntity f) f.setFrozenLocal(true, c[1]);
                    play(mc, ModSounds.GLITCH, 1.3f, 0.45f); play(mc, ModSounds.TICK, 0.6f, 0.8f);
                }
                if (t == 34) {
                    if (e instanceof FacelessEntity f) f.setFrozenLocal(false, 0);   // it moves now: the head twitches
                    play(mc, ModSounds.JUMPSCARE, 0.9f, 1f); play(mc, ModSounds.FACELESS_HUM, 1.6f, 1f); play(mc, ModSounds.STATIC, 0.8f, 0.6f);
                }
                if (t == 37) play(mc, ModSounds.BONE_CRACK, 0.8f, 1f);
                if (t == 39) play(mc, ModSounds.BONE_CRACK, 0.6f, 1f);
            }
            case 2 -> {
                if (t == 1) play(mc, ModSounds.CRAWLER_SKITTER, 0.8f, 0.8f);
                if (t == 7) { play(mc, ModSounds.JUMPSCARE, 1.15f, 1f); play(mc, ModSounds.CRAWLER_CHITTER, 1f, 1f); }
                if (t >= 15 && (t - 15) % 4 == 0) play(mc, ModSounds.CRAWLER_SKITTER, 1.3f + r.nextFloat() * 0.2f, 0.55f);
                if (t == 23) play(mc, ModSounds.CRAWLER_CHITTER, 1.35f, 1f);
            }
            case 3 -> {
                if (t < 24) e.limbAnimator.updateLimbs(1.3f, 0.4f);                    // galloping
                if (t == 1) play(mc, ModSounds.HOUND_GROWL, 0.8f, 0.8f);
                if (t < 24 && t % 5 == 0) play(mc, ModSounds.HEARTBEAT, 1.7f, 0.35f + 0.65f * t / 24f);   // paws hitting the ground
                if (t == 24) {
                    if (e instanceof HoundEntity h) h.setAttacking(true);              // jaws open
                    play(mc, ModSounds.JUMPSCARE, 1f, 1f); play(mc, ModSounds.HOUND_GROWL, 1.35f, 1f); play(mc, ModSounds.BONE_CRACK, 1f, 1f);
                    splatter(r, 24, 8);
                }
                if (t == 26) gashes(r, 26);
            }
            default -> {}
        }
    }

    private static void play(MinecraftClient mc, SoundEvent s, float pitch, float vol) { ScareOverlay.play(mc, s, pitch, vol); }

    /** Blood hitting the screen: an irregular splat, streaks of spray flying out of it, and a drip or two running down. */
    private static void splatter(Random r, int tick, int n) {
        for (int i = 0; i < n; i++) {
            float x = r.nextFloat() * 1.7f, y = r.nextFloat() * 0.75f, rad = 0.014f + r.nextFloat() * 0.04f;
            drops.add(new float[]{x, y + rad * 0.3f, rad * 0.55f, tick, 0.004f + r.nextFloat() * 0.01f, 0.12f + r.nextFloat() * 0.5f});
            for (int k = 0; k < 6; k++) {                                         // ragged blob
                drops.add(new float[]{x + (r.nextFloat() - 0.5f) * rad, y + (r.nextFloat() - 0.5f) * rad, rad * (0.45f + r.nextFloat() * 0.4f), tick, 0f, 0f});
            }
            int rays = 3 + r.nextInt(4);
            for (int k = 0; k < rays; k++) {                                      // spray streaks
                double ang = r.nextDouble() * Math.PI * 2;
                float len = rad * (2f + r.nextFloat() * 2.5f);
                for (int j = 0; j < 6; j++) {
                    float u = (j + 1) / 6f;
                    drops.add(new float[]{x + (float) Math.cos(ang) * len * u, y + (float) Math.sin(ang) * len * u,
                            rad * (0.32f - 0.26f * u) * (0.8f + r.nextFloat() * 0.4f), tick, 0f, 0f});
                }
            }
        }
    }

    private static void gashes(Random r, int tick) {
        float x = 0.12f + r.nextFloat() * 0.2f, y = 0.04f, dx = 0.32f + r.nextFloat() * 0.14f, dy = 0.85f;
        for (int i = 0; i < 3; i++) slashes.add(new float[]{x + i * 0.11f, y + i * 0.02f, x + i * 0.11f + dx, y + i * 0.02f + dy, tick, 0.022f + r.nextFloat() * 0.01f});
    }

    // ------------------------------------------------------------------------------------------ drawing

    static void render(DrawContext ctx, int v, float time, int dur, int w, int h, LivingEntity e, Random r) {
        float t = time / dur;
        ctx.getMatrices().push();
        ctx.getMatrices().translate(0, 0, 200);
        ctx.fill(0, 0, w, h, 0xFF000000);
        ctx.getMatrices().pop();
        switch (v) {
            case 0 -> watcher(ctx, time, w, h, e, r);
            case 1 -> faceless(ctx, time, w, h, e, r);
            case 2 -> crawler(ctx, time, w, h, e, r);
            case 3 -> hound(ctx, time, w, h, e, r);
            default -> {}
        }
        ctx.getMatrices().push();
        ctx.getMatrices().translate(0, 0, 2600);
        drawDrops(ctx, time, w, h);
        drawGashes(ctx, time, w, h);
        grain(ctx, w, h, r, 260);
        vignette(ctx, w, h, 0xD0);
        if (t > 0.86f) ctx.fill(0, 0, w, h, (int) (255 * Math.min(1f, (t - 0.86f) / 0.14f)) << 24);
        ctx.getMatrices().pop();
    }

    /** The real model, its face at (cx, cy), {@code zoom} = screen heights per block, rolled by {@code roll} degrees. */
    private static void creature(DrawContext ctx, LivingEntity e, float faceY, float cx, float cy, float zoom, float roll, int h) {
        MatrixStack m = ctx.getMatrices();
        m.push();
        m.translate(cx, cy, 300);
        m.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(roll));
        m.translate(-cx, -cy, 0);
        int x = Math.round(cx), y = Math.round(cy), big = 4 * h;
        InventoryScreen.drawEntity(ctx, x - big, y - big, x + big, y + big, Math.max(1, Math.round(h * zoom)), faceY, x, y, e);
        m.pop();
    }

    private static void tint(DrawContext ctx, int w, int h, int argb) {
        ctx.getMatrices().push();
        ctx.getMatrices().translate(0, 0, 2400);
        ctx.fill(0, 0, w, h, argb);
        ctx.getMatrices().pop();
    }

    private static int a(float alpha) { return Math.max(0, Math.min(255, (int) (alpha * 255))) << 24; }

    private static float shake(Random r, float amount) { return (r.nextFloat() * 2 - 1) * amount; }

    // --- WATCHER: turn around
    private static void watcher(DrawContext ctx, float time, int w, int h, LivingEntity e, Random r) {
        if (time < 12) {                                                          // it's there in the dark, breathing
            float k = time / 12f;
            creature(ctx, e, WATCHER_STAND, w / 2f + shake(r, 0.6f), h * 0.55f, 0.5f + 0.15f * k, -4f + 3f * k, h);
            tint(ctx, w, h, a(0.93f - 0.45f * k));
            return;
        }
        float s = time - 12;
        float zoom = 2.2f + 0.5f * Math.min(1f, s / 30f);
        float sh = 2f + 18f * (float) Math.exp(-s / 6f);
        boolean twitch = ((int) s) % 7 == 5;                                      // sudden jerks
        float tx = twitch ? shake(r, w * 0.04f) : 0;
        creature(ctx, e, WATCHER_SCREAM, w / 2f + shake(r, sh) + tx, h * 0.5f + shake(r, sh), zoom, shake(r, 3f) + (twitch ? 8f : 0f), h);
        tint(ctx, w, h, a(0.12f + 0.15f * (float) Math.abs(Math.sin(s * 1.7f))) | 0x700000);
        if (s < 1.5f) tint(ctx, w, h, a(0.85f * (1f - s / 1.5f)) | 0xFFFFFF);       // the snap: a white frame
        scanlines(ctx, w, h, r, 14);
    }

    // --- FACELESS: don't blink
    private static void faceless(DrawContext ctx, float time, int w, int h, LivingEntity e, Random r) {
        float[][] cuts = {{8, 11, 0.55f, -0.18f, 0f}, {18, 21, 0.95f, 0.12f, 22f}, {27, 30, 1.5f, -0.05f, -35f}};
        for (float[] c : cuts) {
            if (time >= c[0] && time < c[1]) {
                creature(ctx, e, FACELESS, w * (0.5f + c[3]), h * 0.5f, c[2], c[4], h);
                tint(ctx, w, h, a(0.35f) | 0x0A1428);                              // cold
                if (time - c[0] < 1) tint(ctx, w, h, a(0.45f) | 0xFFFFFF);         // flashbulb
                scanlines(ctx, w, h, r, 10);
                return;
            }
        }
        if (time < 34) { scanlines(ctx, w, h, r, 6); return; }                    // darkness between flickers
        float s = time - 34;
        float roll = s < 3 ? 0 : 78f * Math.min(1f, (s - 3) / 2.2f);              // the neck snaps sideways
        float sh = 3f + 14f * (float) Math.exp(-s / 5f) + (s > 3 && s < 6 ? 16f : 0f);
        creature(ctx, e, FACELESS, w / 2f + shake(r, sh), h * 0.5f + shake(r, sh), 1.9f + 0.3f * Math.min(1, s / 20f), roll + shake(r, 2f), h);
        tint(ctx, w, h, a(0.3f) | 0x0A1428);
        if (s < 1.5f) tint(ctx, w, h, a(0.9f * (1f - s / 1.5f)) | 0xFFFFFF);
        scanlines(ctx, w, h, r, 18);
    }

    // --- CRAWLER: from above
    private static void crawler(DrawContext ctx, float time, int w, int h, LivingEntity e, Random r) {
        drawDust(ctx, time, w, h);
        if (time < 7) return;
        float x, y, zoom, roll;
        if (time < 15) {                                                          // drops from the ceiling, bouncing on its legs
            float s = time - 7;
            float target = h * 0.45f, start = -h * 0.7f;
            y = target - (target - start) * (float) (Math.exp(-s * 0.55f) * Math.cos(s * 1.1f));
            x = w / 2f; zoom = 1.05f; roll = 6f * (float) Math.sin(s * 2);
        } else {                                                                  // stop-motion scuttle towards you
            int step = (int) ((time - 15) / 4);
            Random sr = new Random(step * 7919L + 13);
            x = w * (0.5f + (sr.nextFloat() - 0.5f) * 0.3f);
            y = h * (0.47f + (sr.nextFloat() - 0.5f) * 0.18f);
            zoom = 1.35f + 0.4f * step;
            roll = (sr.nextFloat() - 0.5f) * 50f;
        }
        creature(ctx, e, CRAWLER, x + shake(r, 3), y + shake(r, 3), zoom, roll, h);
        tint(ctx, w, h, a(0.3f) | 0x1C2408);                                      // sickly
        if (time >= 7 && time < 8) tint(ctx, w, h, a(0.6f) | 0xFFFFFF);
        scanlines(ctx, w, h, r, 10);
    }

    // --- HOUND: the charge
    private static void hound(DrawContext ctx, float time, int w, int h, LivingEntity e, Random r) {
        if (time < 24) {
            float k = time / 24f;
            float zoom = 0.12f + 1.0f * (float) Math.pow(k, 2.2);
            float bob = (float) Math.sin(time * 1.3f) * h * 0.035f * zoom;
            creature(ctx, e, HOUND_RUN, w / 2f + (float) Math.sin(time * 0.4f) * w * 0.03f, h * 0.55f + bob, zoom, (float) Math.sin(time * 0.65f) * 4f, h);
            tint(ctx, w, h, a(0.55f - 0.3f * k) | 0x1A0603);                         // red-brown murk
            scanlines(ctx, w, h, r, 6);
            return;
        }
        float s = time - 24;
        float jolt = h * 0.08f * (float) Math.exp(-s / 3f);
        float sh = 3f + 16f * (float) Math.exp(-s / 5f);
        creature(ctx, e, HOUND_BITE, w / 2f + shake(r, sh), h * 0.5f + jolt + shake(r, sh), 2.5f + 0.3f * Math.min(1, s / 20f), shake(r, 4f), h);
        tint(ctx, w, h, a(0.18f + 0.18f * (float) Math.abs(Math.sin(s * 2.1f))) | 0x6A0000);
        if (s < 1.2f) tint(ctx, w, h, a(0.65f * (1 - s / 1.2f)) | 0xB00000);
        scanlines(ctx, w, h, r, 16);
    }

    // ------------------------------------------------------------------------------------------ screen effects

    private static void scanlines(DrawContext ctx, int w, int h, Random r, int n) {
        ctx.getMatrices().push();
        ctx.getMatrices().translate(0, 0, 2500);
        for (int i = 0; i < n; i++) {
            int y = r.nextInt(h);
            ctx.fill(0, y, w, y + 1, (r.nextInt(70) << 24) | (r.nextBoolean() ? 0xFFFFFF : 0x000000));
        }
        ctx.getMatrices().pop();
    }

    private static void grain(DrawContext ctx, int w, int h, Random r, int n) {
        for (int i = 0; i < n; i++) {
            int x = r.nextInt(w), y = r.nextInt(h);
            ctx.fill(x, y, x + 1, y + 1, ((20 + r.nextInt(50)) << 24) | (r.nextBoolean() ? 0xFFFFFF : 0x000000));
        }
    }

    private static void vignette(DrawContext ctx, int w, int h, int strength) {
        ctx.fillGradient(0, 0, w, h / 3, strength << 24, 0);
        ctx.fillGradient(0, h - h / 3, w, h, 0, strength << 24);
        int strips = 24, side = w / 4;
        for (int i = 0; i < strips; i++) {
            int alpha = (int) (strength * Math.pow(1 - i / (float) strips, 2));
            int x0 = side * i / strips, x1 = side * (i + 1) / strips;
            ctx.fill(x0, 0, x1, h, alpha << 24);
            ctx.fill(w - x1, 0, w - x0, h, alpha << 24);
        }
    }

    private static void disc(DrawContext ctx, float cx, float cy, float rad, int argb) {
        int r = Math.max(1, Math.round(rad));
        for (int dy = -r; dy <= r; dy++) {
            int half = (int) Math.round(Math.sqrt(r * r - dy * dy));
            ctx.fill(Math.round(cx) - half, Math.round(cy) + dy, Math.round(cx) + half + 1, Math.round(cy) + dy + 1, argb);
        }
    }

    /** Blood on the screen: splats with droplets around them, running down in drips. */
    private static void drawDrops(DrawContext ctx, float time, int w, int h) {
        for (float[] d : drops) {
            float age = time - d[3];
            if (age < 0) continue;
            float x = d[0] * h - (1.7f * h - w) / 2f, y = d[1] * h, rad = d[2] * h * Math.min(1f, 0.4f + age * 0.6f);
            float run = Math.min(d[5], d[4] * age) * h;
            if (run > 1) {
                float rw = Math.max(1f, rad * 0.35f);
                ctx.fill(Math.round(x - rw), Math.round(y), Math.round(x + rw), Math.round(y + run), 0xE8500000);
                disc(ctx, x, y + run, rw * 1.3f, 0xEE5A0202);
            }
            disc(ctx, x, y, rad, 0xE0400000);
            disc(ctx, x, y, rad * 0.72f, 0xF0700808);
            if (rad > 4) disc(ctx, x - rad * 0.3f, y - rad * 0.35f, Math.max(1, rad * 0.15f), 0x70FF9090);   // wet highlight
        }
    }

    /** Claw gashes ripping across the screen. */
    private static void drawGashes(DrawContext ctx, float time, int w, int h) {
        for (float[] g : slashes) {
            float grow = Math.min(1f, (time - g[4]) / 3f);
            if (grow <= 0) continue;
            float x0 = g[0] * w, y0 = g[1] * h, x1 = g[2] * w, y1 = g[3] * h;
            float len = (float) Math.hypot(x1 - x0, y1 - y0);
            int steps = (int) (len * grow);
            for (int pass = 0; pass < 3; pass++) {               // dark torn edges, then raw red, then the wet core on top
                float k = pass == 0 ? 1f : pass == 1 ? 0.6f : 0.22f;
                int col = pass == 0 ? 0xF0200000 : pass == 1 ? 0xFF9A1010 : 0xFFD03A30;
                for (int i = 0; i < steps; i++) {
                    float u = i / Math.max(1f, len);
                    float px = x0 + (x1 - x0) * u, py = y0 + (y1 - y0) * u;
                    float th = (g[5] * h * (float) Math.sin(Math.PI * Math.min(1f, u)) + 1) * k;
                    int q = Math.max(1, Math.round(th));
                    ctx.fill(Math.round(px - q), Math.round(py - q * 0.5f), Math.round(px + q), Math.round(py + q * 0.5f), col);
                }
            }
        }
    }

    private static void drawDust(DrawContext ctx, float time, int w, int h) {
        ctx.getMatrices().push();
        ctx.getMatrices().translate(0, 0, 2500);
        for (float[] d : dust) {
            float y = (d[1] + time * d[2] * d[3] * 2.2f) * h;
            if (y < 0 || y > h) continue;
            int x = Math.round(d[0] * w + (float) Math.sin(time * 0.3f + d[0] * 20) * 3);
            ctx.fill(x, Math.round(y), x + 1, Math.round(y) + 2, 0x90A09888);
        }
        ctx.getMatrices().pop();
    }
}
