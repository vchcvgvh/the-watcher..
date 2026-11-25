package com.thewatcher.net;

import com.thewatcher.TheWatcher;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;

/**
 * Server -> client: "scare this player now". The client decides what it looks like (all text and
 * images live client-side, so a server can only pick from the mod's own scares).
 */
public record ScarePayload(int kind, int variant, int seed) implements CustomPayload {
    public static final int JUMPSCARE = 0;   // variant: 0 watcher, 1 faceless, 2 crawler, 3 hound
    public static final int GLITCH_TEXT = 1; // big shaking red text in the middle of the screen
    public static final int SUBLIMINAL = 2;  // a few frames of black with huge white words
    public static final int TYPED = 3;       // a quiet line typing itself out at the bottom of the screen
    public static final int PC_NOTE = 4;     // a note left in a text file on your computer, opened in Notepad
    public static final int STAGE = 5;       // variant = your dread stage: drives the always-on dread effects
    public static final int FAKE_DEATH = 6;  // a "You Died!" screen that isn't real... yet

    public static final CustomPayload.Id<ScarePayload> ID = new CustomPayload.Id<>(TheWatcher.id("scare"));
    public static final PacketCodec<RegistryByteBuf, ScarePayload> CODEC = PacketCodec.tuple(
            PacketCodecs.VAR_INT, ScarePayload::kind,
            PacketCodecs.VAR_INT, ScarePayload::variant,
            PacketCodecs.INTEGER, ScarePayload::seed,
            ScarePayload::new);

    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() { return ID; }
}
