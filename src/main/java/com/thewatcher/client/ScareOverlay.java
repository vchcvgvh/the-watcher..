package com.thewatcher.client;

import com.thewatcher.entity.ModEntities;
import com.thewatcher.entity.WatcherEntity;
import com.thewatcher.net.ScarePayload;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.entity.LivingEntity;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;

import java.util.Random;

/**
 * Full-screen scares drawn over the game:
 * - JUMPSCARE: the creature's real 3D model lunges into your face, screaming, the screen shaking and
 *   flashing red with static;
 * - GLITCH_TEXT: huge red words that shake, flicker and corrupt in the middle of the screen;
 * - SUBLIMINAL: three frames of black with giant white words - gone before you're sure you saw it;
 * - TYPED: a quiet line typing itself out at the bottom of the screen;
 * - PC_NOTE: hands off to {@link PcNotes}.
 */
@Environment(EnvType.CLIENT)
public final class ScareOverlay {
    private ScareOverlay() {}

    private static int kind = -1, variant, timer, duration;
    private static String text = "";
    private static LivingEntity face;
    private static final Random R = new Random();
    private static final String GLITCH_CHARS = "#%&@$?!*/\\|<>~^";


    public static void start(ScarePayload p) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.world == null || mc.player == null) return;
        R.setSeed(p.seed());
        switch (p.kind()) {
            case ScarePayload.JUMPSCARE -> {
                if (!WatcherConfig.jumpscares) return;
                int v = Math.floorMod(p.variant(), 4);
                LivingEntity e = switch (v) {
                    case 1 -> ModEntities.FACELESS.create(mc.world);
                    case 2 -> ModEntities.CRAWLER.create(mc.world);
                    case 3 -> ModEntities.HOUND.create(mc.world);
                    default -> ModEntities.WATCHER.create(mc.world);
                };
                if (e == null) return;
                e.age = R.nextInt(200);
                face = e;
                variant = v;
                Jumpscares.begin(v, e, R);
                duration = Jumpscares.duration(v);
            }
            case ScarePayload.GLITCH_TEXT -> {
                if (!WatcherConfig.popupText) return;
                text = ScareLines.pick(ScareLines.GLITCH, R);
                duration = 50 + R.nextInt(30);
                play(mc, com.thewatcher.ModSounds.GLITCH, 1f, 0.8f);
                play(mc, com.thewatcher.ModSounds.HEARTBEAT, 0.9f, 1f);
            }
            case ScarePayload.SUBLIMINAL -> {
                if (!WatcherConfig.popupText) return;
                text = ScareLines.pick(ScareLines.SUBLIMINAL, R);
                duration = 3;
                play(mc, com.thewatcher.ModSounds.STATIC, 1.2f, 0.9f);
            }
            case ScarePayload.TYPED -> {
                if (!WatcherConfig.popupText) return;
                text = ScareLines.pick(ScareLines.TYPED, R);
                duration = text.length() * 2 + 80;
                play(mc, com.thewatcher.ModSounds.WHISPER, 1f, 0.6f);
            }
            case ScarePayload.STAGE -> {
                Dread.setStage(p.variant());
                return;
            }
            case ScarePayload.FAKE_DEATH -> {
                if (!WatcherConfig.jumpscares) return;
                duration = 100;
                play(mc, SoundEvents.ENTITY_PLAYER_DEATH, 1f, 1f);
            }
            case ScarePayload.PC_NOTE -> {
                PcNotes.leaveNote(R);
                return;
            }
            default -> { return; }
        }
        kind = p.kind();
        timer = 0;
    }

    static void play(MinecraftClient mc, SoundEvent s, float pitch, float volume) {
        mc.getSoundManager().play(PositionedSoundInstance.master(s, pitch, volume));
    }

    public static void tick(MinecraftClient mc) {
        if (kind < 0) return;
        timer++;
        if (face != null) face.age++;
        if (kind == ScarePayload.JUMPSCARE && face != null) Jumpscares.tick(mc, variant, timer, face, R);
        if (kind == ScarePayload.FAKE_DEATH && timer == 62) {
            play(mc, com.thewatcher.ModSounds.STATIC, 1f, 0.9f);
            play(mc, com.thewatcher.ModSounds.WHISPER, 0.8f, 1f);
        }
        if (kind == ScarePayload.TYPED && timer % 2 == 0 && timer / 2 <= text.length()
                && timer / 2 > 0 && text.charAt(timer / 2 - 1) != ' ') {
            play(mc, com.thewatcher.ModSounds.TICK, 0.8f + R.nextFloat() * 0.4f, 0.35f);
        }
        if (timer >= duration || mc.world == null) {
            kind = -1;
            face = null;
        }
    }

    public static void render(DrawContext ctx, RenderTickCounter tc) {
        if (kind < 0) return;
        MinecraftClient mc = MinecraftClient.getInstance();
        int w = ctx.getScaledWindowWidth(), h = ctx.getScaledWindowHeight();
        float t = Math.min(1f, (timer + tc.getTickDelta(false)) / duration);
        switch (kind) {
            case ScarePayload.JUMPSCARE -> { if (face != null) Jumpscares.render(ctx, variant, timer + tc.getTickDelta(false), duration, w, h, face, R); }
            case ScarePayload.GLITCH_TEXT -> glitch(ctx, mc.textRenderer, w, h, t);
            case ScarePayload.SUBLIMINAL -> subliminal(ctx, mc.textRenderer, w, h);
            case ScarePayload.TYPED -> typed(ctx, mc.textRenderer, w, h);
            case ScarePayload.FAKE_DEATH -> fakeDeath(ctx, mc, w, h);
            default -> {}
        }
    }

    private static String corrupt(String s, float amount) {
        StringBuilder b = new StringBuilder(s);
        for (int i = 0; i < b.length(); i++) {
            if (b.charAt(i) != ' ' && R.nextFloat() < amount) b.setCharAt(i, GLITCH_CHARS.charAt(R.nextInt(GLITCH_CHARS.length())));
        }
        return b.toString();
    }

    private static void glitch(DrawContext ctx, TextRenderer tr, int w, int h, float t) {
        float fadeIn = Math.min(1f, t * 8f), fadeOut = Math.min(1f, (1f - t) * 6f);
        boolean flick = R.nextFloat() < 0.12f;
        if (flick) return;                                   // flickers out for a frame now and then
        float a = fadeIn * fadeOut;
        ctx.fillGradient(0, 0, w, h, ((int) (110 * a) << 24), ((int) (170 * a) << 24));
        String shown = corrupt(text, 0.04f + (R.nextFloat() < 0.15f ? 0.35f : 0f));
        float scale = Math.min(4.5f, (w * 0.85f) / Math.max(1, tr.getWidth(shown)));
        ctx.getMatrices().push();
        ctx.getMatrices().translate(w / 2f, h / 2f - 4 * scale, 400);
        ctx.getMatrices().scale(scale, scale, 1);
        int x = -tr.getWidth(shown) / 2;
        int alpha = Math.max(8, (int) (255 * a)) << 24;
        int j = R.nextFloat() < 0.3f ? 2 : 1;
        ctx.drawText(tr, shown, x - j, R.nextInt(3) - 1, alpha | 0x400000, false);   // smeared copies
        ctx.drawText(tr, shown, x + j, R.nextInt(3) - 1, alpha | 0x30FFFF, false);
        ctx.drawText(tr, shown, x + (R.nextInt(3) - 1), R.nextInt(3) - 1, alpha | 0xD01010, false);
        ctx.getMatrices().pop();
    }

    /** Looks exactly like the real death screen... until it doesn't. */
    private static void fakeDeath(DrawContext ctx, MinecraftClient mc, int w, int h) {
        TextRenderer tr = mc.textRenderer;
        ctx.getMatrices().push();
        ctx.getMatrices().translate(0, 0, 400);
        if (timer >= 92) { ctx.getMatrices().pop(); return; }
        ctx.fillGradient(0, 0, w, h, 0x60500000, 0xA0803030);
        boolean broken = timer >= 62;
        String title = broken ? corrupt("not yet.", 0.08f) : net.minecraft.text.Text.translatable("deathScreen.title").getString();
        ctx.getMatrices().push();
        int j = broken ? R.nextInt(5) - 2 : 0;
        ctx.getMatrices().translate(w / 2f + j, 30 + (broken ? R.nextInt(5) - 2 : 0), 0);
        ctx.getMatrices().scale(2f, 2f, 1f);
        ctx.drawText(tr, title, -tr.getWidth(title) / 2, 0, broken ? 0xFFD01010 : 0xFFFFFFFF, true);
        ctx.getMatrices().pop();
        if (!broken) {
            String score = "Score: \u00a7e" + mc.player.getScore();
            ctx.drawText(tr, score, w / 2 - tr.getWidth(score.replace("\u00a7e", "")) / 2, 100, 0xFFFFFFFF, true);
            String[] labels = {net.minecraft.text.Text.translatable("deathScreen.respawn").getString(),
                    net.minecraft.text.Text.translatable("deathScreen.titleScreen").getString()};
            for (int i = 0; i < 2; i++) {                                     // greyed-out buttons, like for the first second
                int bx = w / 2 - 100, by = h / 4 + 72 + i * 24;
                ctx.fill(bx, by, bx + 200, by + 20, 0xFF2A2A2A);
                ctx.fill(bx + 1, by + 1, bx + 199, by + 19, 0xFF545454);
                ctx.drawText(tr, labels[i], w / 2 - tr.getWidth(labels[i]) / 2, by + 6, 0xFFA0A0A0, true);
            }
        } else {
            String sub = corrupt("it isn't finished with you, " + ScareLines.fill("{player}"), 0.05f);
            ctx.drawText(tr, sub, w / 2 - tr.getWidth(sub) / 2, 100, 0xFF901010, true);
            for (int i = 0; i < 20; i++) {
                int y = R.nextInt(h);
                ctx.fill(0, y, w, y + 1, (R.nextInt(120) << 24) | (R.nextBoolean() ? 0xFFFFFF : 0x000000));
            }
        }
        ctx.getMatrices().pop();
    }

    private static void subliminal(DrawContext ctx, TextRenderer tr, int w, int h) {
        ctx.getMatrices().push();
        ctx.getMatrices().translate(0, 0, 400);
        ctx.fill(0, 0, w, h, 0xFF000000);
        float scale = Math.min(7f, (w * 0.9f) / Math.max(1, tr.getWidth(text)));
        ctx.getMatrices().translate(w / 2f, h / 2f - 4 * scale, 0);
        ctx.getMatrices().scale(scale, scale, 1);
        ctx.drawText(tr, text, -tr.getWidth(text) / 2, 0, 0xFFFFFFFF, false);
        ctx.getMatrices().pop();
    }

    private static void typed(DrawContext ctx, TextRenderer tr, int w, int h) {
        int n = Math.min(text.length(), timer / 2);
        String shown = text.substring(0, n) + (timer / 6 % 2 == 0 && n < text.length() ? "_" : "");
        int fade = Math.min(255, (duration - timer) * 8);
        if (fade <= 8) return;
        ctx.getMatrices().push();
        ctx.getMatrices().translate(w / 2f, h - 70, 400);
        ctx.getMatrices().scale(1.5f, 1.5f, 1);
        ctx.drawText(tr, shown, -tr.getWidth(text) / 2, 0, (fade << 24) | 0xB8B8B8, true);
        ctx.getMatrices().pop();
    }
}
