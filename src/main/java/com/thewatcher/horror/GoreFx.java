package com.thewatcher.horror;

import com.thewatcher.TheWatcher;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.particle.BlockStateParticleEffect;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import org.joml.Vector3f;

/** Blood particles: drips falling off the creatures, and sprays when they hit or are hit. /gamerule watcherGore */
public final class GoreFx {
    private GoreFx() {}

    private static final BlockStateParticleEffect DRIP = new BlockStateParticleEffect(ParticleTypes.FALLING_DUST, Blocks.RED_CONCRETE.getDefaultState());
    private static final BlockStateParticleEffect CHUNKS = new BlockStateParticleEffect(ParticleTypes.BLOCK, Blocks.RED_CONCRETE.getDefaultState());
    private static final DustParticleEffect MIST = new DustParticleEffect(new Vector3f(0.45f, 0.0f, 0.02f), 1.4f);

    public static boolean enabled(ServerWorld world) {
        return world.getGameRules().getBoolean(TheWatcher.GORE);
    }

    /** A drop of blood falling from the creature at {@code heightFraction} of its height (0 = feet, 1 = top). */
    public static void drip(ServerWorld world, Entity e, double heightFraction, double forward) {
        if (!enabled(world)) return;
        double yaw = Math.toRadians(e.getBodyYaw());
        double fx = -Math.sin(yaw) * forward, fz = Math.cos(yaw) * forward;
        world.spawnParticles(DRIP, e.getX() + fx, e.getY() + e.getHeight() * heightFraction, e.getZ() + fz, 1, 0.12, 0.04, 0.12, 0.0);
    }

    /** A spray of blood from {@code e} (when something is hit). */
    public static void spray(ServerWorld world, Entity e) {
        if (!enabled(world)) return;
        double x = e.getX(), y = e.getY() + e.getHeight() * 0.6, z = e.getZ();
        world.playSound(null, x, y, z, com.thewatcher.ModSounds.BONE_CRACK, net.minecraft.sound.SoundCategory.HOSTILE, 0.7f, 0.85f + world.random.nextFloat() * 0.3f);
        world.spawnParticles(CHUNKS, x, y, z, 28, 0.25, 0.3, 0.25, 0.15);
        world.spawnParticles(MIST, x, y, z, 18, 0.3, 0.3, 0.3, 0.02);
        world.spawnParticles(DRIP, x, y, z, 10, 0.3, 0.2, 0.3, 0.0);
    }
}
