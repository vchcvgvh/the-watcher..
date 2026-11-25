package com.thewatcher.horror;

import net.minecraft.entity.Entity;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.network.packet.s2c.play.PlaySoundS2CPacket;
import net.minecraft.network.packet.s2c.play.SubtitleS2CPacket;
import net.minecraft.network.packet.s2c.play.TitleFadeS2CPacket;
import net.minecraft.network.packet.s2c.play.TitleS2CPacket;
import net.minecraft.registry.Registries;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

/** Small helpers for "only this player experiences it" effects. */
public final class Fx {
    private Fx() {}

    /** Plays a sound that only {@code player} can hear, as if it came from {@code pos}. */
    public static void sound(ServerPlayerEntity player, SoundEvent sound, Vec3d pos, float volume, float pitch) {
        sound(player, Registries.SOUND_EVENT.getEntry(sound), pos, volume, pitch);
    }

    public static void sound(ServerPlayerEntity player, RegistryEntry<SoundEvent> sound, Vec3d pos, float volume, float pitch) {
        player.networkHandler.sendPacket(new PlaySoundS2CPacket(
                sound, SoundCategory.HOSTILE, pos.x, pos.y, pos.z, volume, pitch, player.getRandom().nextLong()));
    }

    /** A small grey line in the middle of the screen. */
    public static void whisper(ServerPlayerEntity player, Text text) {
        player.networkHandler.sendPacket(new TitleFadeS2CPacket(20, 50, 30));
        player.networkHandler.sendPacket(new SubtitleS2CPacket(text));
        player.networkHandler.sendPacket(new TitleS2CPacket(Text.empty()));
    }

    public static void effect(ServerPlayerEntity player, RegistryEntry<StatusEffect> effect, int ticks, int amplifier) {
        player.addStatusEffect(new StatusEffectInstance(effect, ticks, amplifier, false, false, false));
    }

    /** Horizontal unit vector the player is facing. */
    public static Vec3d flatLook(Entity e) {
        Vec3d look = e.getRotationVector();
        Vec3d flat = new Vec3d(look.x, 0, look.z);
        return flat.lengthSquared() < 1.0E-4 ? new Vec3d(0, 0, 1) : flat.normalize();
    }

    /** Rotate a horizontal vector around the Y axis by {@code degrees}. */
    public static Vec3d rotateY(Vec3d v, double degrees) {
        double r = Math.toRadians(degrees);
        double cos = Math.cos(r), sin = Math.sin(r);
        return new Vec3d(v.x * cos - v.z * sin, 0, v.x * sin + v.z * cos);
    }

    /** True when the player is looking roughly at {@code target} (within {@code minDegrees}, wider when close). */
    public static boolean isLookingAt(ServerPlayerEntity player, Vec3d target, double minDegrees) {
        Vec3d eye = player.getEyePos();
        Vec3d to = target.subtract(eye);
        double dist = to.length();
        if (dist < 1.0E-3) return true;
        double allowed = Math.max(Math.toRadians(minDegrees), Math.atan2(1.4, dist));
        double dot = player.getRotationVec(1.0f).normalize().dotProduct(to.normalize());
        return dot > Math.cos(allowed);
    }

    /** True when {@code target} is well outside the player's field of view. */
    public static boolean isBehind(ServerPlayerEntity player, Vec3d target) {
        Vec3d to = target.subtract(player.getEyePos()).normalize();
        return player.getRotationVec(1.0f).normalize().dotProduct(to) < 0.1;
    }

    /**
     * Finds a block position near (x, z) around height {@code aroundY} where a 3-block-tall
     * creature can stand on solid ground. Returns null if there's nowhere sensible.
     */
    @Nullable
    public static BlockPos findStandingSpot(ServerWorld world, double x, double z, int aroundY, int searchHeight) {
        BlockPos.Mutable pos = new BlockPos.Mutable();
        for (int dy = searchHeight; dy >= -searchHeight; dy--) {
            pos.set(x, aroundY + dy, z);
            if (!world.isChunkLoaded(pos)) return null;
            if (isFree(world, pos) && isFree(world, pos.up()) && isFree(world, pos.up(2))) {
                BlockPos below = pos.down();
                if (world.getBlockState(below).isSolidBlock(world, below)) {
                    return pos.toImmutable();
                }
            }
        }
        return null;
    }

    private static boolean isFree(World world, BlockPos pos) {
        return world.getBlockState(pos).getCollisionShape(world, pos).isEmpty();
    }
}
