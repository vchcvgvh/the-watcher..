package com.thewatcher.horror;

import com.thewatcher.net.ScarePayload;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.random.Random;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Screen scares, sent to the player's client (see client/ScareOverlay). */
public final class Scares {
    private Scares() {}

    private static final Map<UUID, Integer> lastScreamer = new HashMap<>();
    private static final Map<UUID, Integer> lastNote = new HashMap<>();

    public static boolean send(ServerPlayerEntity p, int kind, int variant) {
        if (!ServerPlayNetworking.canSend(p, ScarePayload.ID)) return false;
        ServerPlayNetworking.send(p, new ScarePayload(kind, variant, p.getRandom().nextInt()));
        return true;
    }

    private static boolean cooled(Map<UUID, Integer> map, ServerPlayerEntity p, int ticks) {
        int now = p.getServer().getTicks();
        Integer last = map.get(p.getUuid());
        if (last != null && now - last < ticks) return false;
        map.put(p.getUuid(), now);
        return true;
    }

    /** A face fills the screen, screaming. Which creature depends on how far gone you are. */
    public static boolean screamer(ServerPlayerEntity p, int stage, Random r) {
        if (!cooled(lastScreamer, p, 20 * 75)) return false;   // at most one every 75 seconds
        int creature = r.nextInt(Math.min(4, stage + 1));     // watcher, faceless, crawler, hound
        Fx.effect(p, net.minecraft.entity.effect.StatusEffects.DARKNESS, 60, 0);
        return send(p, ScarePayload.JUMPSCARE, creature);
    }

    public static boolean glitchText(ServerPlayerEntity p, Random r) { return send(p, ScarePayload.GLITCH_TEXT, -1); }

    public static boolean subliminal(ServerPlayerEntity p, Random r) { return send(p, ScarePayload.SUBLIMINAL, -1); }

    public static boolean typed(ServerPlayerEntity p, Random r) { return send(p, ScarePayload.TYPED, -1); }

    /** A note in a text file on the player's computer. The client also rate-limits these. */
    public static boolean pcNote(ServerPlayerEntity p, Random r) {
        if (!cooled(lastNote, p, 20 * 60 * 8)) return false;    // at most one every 8 minutes
        return send(p, ScarePayload.PC_NOTE, -1);
    }
}
