package com.thewatcher;

import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;

/** The mod's own sounds: all synthesized from scratch (assets/thewatcher/sounds, listed in sounds.json). */
public final class ModSounds {
    private ModSounds() {}

    public static final SoundEvent JUMPSCARE = reg("jumpscare");
    public static final SoundEvent WATCHER_SCREAM = reg("watcher.scream");
    public static final SoundEvent DISTANT_SCREAM = reg("distant_scream");
    public static final SoundEvent WHISPER = reg("whisper");
    public static final SoundEvent BREATHING = reg("breathing");
    public static final SoundEvent HEARTBEAT = reg("heartbeat");
    public static final SoundEvent STATIC = reg("static");
    public static final SoundEvent GLITCH = reg("glitch");
    public static final SoundEvent TICK = reg("tick");
    public static final SoundEvent KNOCK = reg("knock");
    public static final SoundEvent CRAWLER_SKITTER = reg("crawler.skitter");
    public static final SoundEvent CRAWLER_CHITTER = reg("crawler.chitter");
    public static final SoundEvent HOUND_GROWL = reg("hound.growl");
    public static final SoundEvent HOUND_HOWL = reg("hound.howl");
    public static final SoundEvent FACELESS_HUM = reg("faceless.hum");
    public static final SoundEvent DRONE = reg("drone");
    public static final SoundEvent BONE_CRACK = reg("bone_crack");

    private static SoundEvent reg(String name) {
        Identifier id = TheWatcher.id(name);
        return Registry.register(Registries.SOUND_EVENT, id, SoundEvent.of(id));
    }

    /** Called from onInitialize so the fields above load (and register) at the right time. */
    public static void register() {}
}
