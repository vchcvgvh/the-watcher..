package com.thewatcher.horror;

import com.thewatcher.TheWatcher;
import com.thewatcher.entity.ModEntities;
import com.thewatcher.gen.Realm;
import com.thewatcher.gen.RealmGen;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.EnumMap;
import java.util.Map;

/**
 * The places it takes you (see {@link Realm}): going in, building them around you as you explore
 * ({@link RealmGen}), what lives there, the sound that leads you to the way out, and coming back.
 */
public final class Dimensions {
    private Dimensions() {}

    private static final Map<Realm, RegistryKey<World>> KEYS = new EnumMap<>(Realm.class);
    static {
        for (Realm r : Realm.values()) KEYS.put(r, RegistryKey.of(RegistryKeys.WORLD, TheWatcher.id(r.id)));
    }

    public static final RegistryKey<World> HALLWAYS = KEYS.get(Realm.HALLWAYS);
    public static final RegistryKey<World> HOLLOW = KEYS.get(Realm.HOLLOW);

    public static RegistryKey<World> key(Realm r) { return KEYS.get(r); }

    @Nullable
    public static Realm realmOf(World world) {
        for (Map.Entry<Realm, RegistryKey<World>> e : KEYS.entrySet()) if (e.getValue() == world.getRegistryKey()) return e.getKey();
        return null;
    }

    public static boolean isHorrorDim(World world) { return realmOf(world) != null; }

    // ------------------------------------------------------------------ going in

    public static boolean send(ServerPlayerEntity p, RegistryKey<World> key) {
        for (Map.Entry<Realm, RegistryKey<World>> e : KEYS.entrySet()) if (e.getValue() == key) return send(p, e.getKey());
        return false;
    }

    /** Takes the player away. Returns false if they're already in one, or the dimension is missing. */
    public static boolean send(ServerPlayerEntity p, Realm realm) {
        MinecraftServer server = p.server;
        ServerWorld target = server.getWorld(key(realm));
        if (target == null || isHorrorDim(p.getWorld())) return false;

        Random r = p.getRandom();
        int ax = (r.nextInt(12000) - 6000) * 16 + 8;
        int az = (r.nextInt(12000) - 6000) * 16 + 8;
        double ang = r.nextDouble() * Math.PI * 2;
        int dist = realm.exitMin + r.nextInt(realm.exitRange);
        int ex = ax + (int) (Math.cos(ang) * dist), ez = az + (int) (Math.sin(ang) * dist);

        var sink = RealmBlocks.sink(target, realm);
        for (int dx = -2; dx <= 2; dx++)
            for (int dz = -2; dz <= 2; dz++) ensureDecorated(target, realm, (ax >> 4) + dx, (az >> 4) + dz);
        RealmGen.prepareArrival(realm, sink, ax, realm.standY, az);
        RealmGen.buildExit(realm, sink, ex, ez, ax, az);

        TripState.Trip trip = new TripState.Trip();
        trip.fromDim = p.getWorld().getRegistryKey().getValue().toString();
        trip.fromPos = p.getBlockPos().asLong();
        trip.fromYaw = p.getYaw();
        trip.toDim = key(realm).getValue().toString();
        trip.exitPos = new BlockPos(ex, realm.exitY(), ez).asLong();
        trip.startTime = server.getOverworld().getTime();
        TripState.get(server).put(p.getUuid(), trip);

        if (p.isSleeping()) p.wakeUp(false, true);
        p.stopRiding();
        p.teleport(target, ax + 0.5, realm.standY, az + 0.5, p.getYaw(), 0f);

        Fx.effect(p, StatusEffects.BLINDNESS, 50, 0);
        Fx.effect(p, StatusEffects.DARKNESS, 220, 0);
        Fx.sound(p, SoundEvents.ENTITY_WARDEN_SONIC_CHARGE, p.getPos(), 0.8f, 0.5f);
        Fx.sound(p, com.thewatcher.ModSounds.STATIC, p.getPos(), 1.0f, 0.8f);
        Fx.sound(p, com.thewatcher.ModSounds.DRONE, p.getPos(), 1.0f, 0.8f);
        String line = switch (realm) {
            case HALLWAYS -> "where are you?";
            case HOLLOW -> "follow the lanterns";
            case FLESH -> "it's breathing";
            case STILL_WATER -> "listen for the bell";
        };
        Fx.whisper(p, Text.literal(line).formatted(Formatting.GRAY, Formatting.ITALIC));
        return true;
    }

    // ------------------------------------------------------------------ coming back

    public static void returnHome(ServerPlayerEntity p, boolean escaped) {
        MinecraftServer server = p.server;
        TripState ts = TripState.get(server);
        TripState.Trip t = ts.get(p.getUuid());
        ServerWorld dest = null;
        BlockPos pos = null;
        float yaw = 0f;
        if (t != null) {
            Identifier id = Identifier.tryParse(t.fromDim);
            if (id != null) dest = server.getWorld(RegistryKey.of(RegistryKeys.WORLD, id));
            pos = BlockPos.fromLong(t.fromPos);
            yaw = t.fromYaw;
        }
        if (dest == null || pos == null) {
            dest = server.getOverworld();
            pos = dest.getSpawnPos();
        }
        ts.remove(p.getUuid());
        p.teleport(dest, pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, yaw, 0f);
        if (escaped) {
            Fx.whisper(p, Text.literal("You got out. This time.").formatted(Formatting.GRAY, Formatting.ITALIC));
        } else {
            Fx.effect(p, StatusEffects.BLINDNESS, 60, 0);
            Fx.whisper(p, Text.literal("You wake up.").formatted(Formatting.GRAY, Formatting.ITALIC));
        }
        Fx.sound(p, com.thewatcher.ModSounds.HEARTBEAT, p.getPos(), 1f, 0.9f);
    }

    // ------------------------------------------------------------------ every 10 ticks, per player

    public static void tickPlayer(ServerPlayerEntity p) {
        MinecraftServer server = p.server;
        TripState ts = TripState.get(server);
        TripState.Trip trip = ts.get(p.getUuid());
        Realm realm = realmOf(p.getWorld());
        if (realm == null) {
            if (trip != null) ts.remove(p.getUuid());       // died or left some other way
            return;
        }
        ServerWorld world = p.getServerWorld();

        // Build the world around them as they explore.
        ChunkPos c = p.getChunkPos();
        int built = 0;
        DecoratedChunks dc = DecoratedChunks.get(world);
        for (int ring = 0; ring <= 5 && built < 3; ring++) {
            for (int dx = -ring; dx <= ring && built < 3; dx++) {
                for (int dz = -ring; dz <= ring && built < 3; dz++) {
                    if (Math.max(Math.abs(dx), Math.abs(dz)) != ring) continue;
                    if (!dc.isDone(ChunkPos.toLong(c.x + dx, c.z + dz))) {
                        ensureDecorated(world, realm, c.x + dx, c.z + dz);
                        built++;
                    }
                }
            }
        }

        long now = server.getOverworld().getTime();
        if (trip != null) {
            BlockPos exit = BlockPos.fromLong(trip.exitPos);
            Vec3d exitV = Vec3d.ofBottomCenter(exit);
            if (p.getPos().squaredDistanceTo(exitV) < 1.6 * 1.6) {
                returnHome(p, true);
                return;
            }
            if (now - trip.startTime > realm.minutes * 60L * 20L) {
                returnHome(p, false);
                return;
            }
            // A sound from the direction of the way out, every 8 seconds.
            if (now % 160 < 10 && p.getPos().squaredDistanceTo(exitV) > 20 * 20) {
                SoundEvent hint = switch (realm) {
                    case HALLWAYS -> com.thewatcher.ModSounds.KNOCK;
                    case HOLLOW -> com.thewatcher.ModSounds.WHISPER;
                    case FLESH -> com.thewatcher.ModSounds.HEARTBEAT;
                    case STILL_WATER -> SoundEvents.BLOCK_BELL_RESONATE;
                };
                Vec3d dir = exitV.subtract(p.getPos()).multiply(1, 0, 1).normalize();
                Fx.sound(p, hint, p.getPos().add(dir.multiply(12)), realm == Realm.STILL_WATER ? 2.0f : 1.2f,
                        realm == Realm.STILL_WATER ? 0.6f : 0.5f);
            }
        }

        if (p.isCreative() || p.isSpectator()) return;
        Random r = p.getRandom();
        int y = realm.standY;
        switch (realm) {
            case HALLWAYS -> {
                if (r.nextInt(14) == 0) flicker(world, p, r);
                if (r.nextInt(36) == 0 && Spawns.countNear(world, ModEntities.CRAWLER, p, 48) < 3)
                    Spawns.spawnAround(world, ModEntities.CRAWLER, p, 18, 30, false, 1, y, 0);
            }
            case HOLLOW -> {
                if (r.nextInt(30) == 0 && Spawns.countNear(world, ModEntities.HOUND, p, 64) < 4
                        && Spawns.spawnAround(world, ModEntities.HOUND, p, 28, 38, false, 2, y, 3) > 0)
                    Fx.sound(p, com.thewatcher.ModSounds.HOUND_HOWL, p.getPos().add(Fx.flatLook(p).multiply(-20)), 2.5f, 0.85f);
                if (r.nextInt(45) == 0 && Spawns.countNear(world, ModEntities.FACELESS, p, 64) < 3)
                    Spawns.spawnAround(world, ModEntities.FACELESS, p, 18, 28, true, 1, y, 3);
            }
            case FLESH -> {
                if (r.nextInt(26) == 0 && Spawns.countNear(world, ModEntities.CRAWLER, p, 48) < 4)
                    Spawns.spawnAround(world, ModEntities.CRAWLER, p, 14, 26, false, 1, y, 2);
                if (r.nextInt(70) == 0 && Spawns.countNear(world, ModEntities.HOUND, p, 48) < 2)
                    Spawns.spawnAround(world, ModEntities.HOUND, p, 20, 30, true, 1, y, 2);
            }
            case STILL_WATER -> {
                if (r.nextInt(32) == 0 && Spawns.countNear(world, ModEntities.FACELESS, p, 64) < 4)
                    Spawns.spawnAround(world, ModEntities.FACELESS, p, 16, 30, r.nextBoolean(), 1, y, 1);
            }
        }
    }

    /** A light above you dies for a moment. */
    private static void flicker(ServerWorld world, ServerPlayerEntity p, Random r) {
        BlockPos base = p.getBlockPos();
        for (int i = 0; i < 20; i++) {
            BlockPos pos = new BlockPos(base.getX() + r.nextInt(17) - 8, Realm.HALLWAYS.standY + 4, base.getZ() + r.nextInt(17) - 8);
            if (world.getBlockState(pos).isOf(Blocks.SEA_LANTERN)) {
                world.setBlockState(pos, Blocks.REDSTONE_LAMP.getDefaultState(), Block.NOTIFY_LISTENERS);
                Fx.sound(p, SoundEvents.BLOCK_LEVER_CLICK, Vec3d.ofCenter(pos), 0.4f, 0.5f);
                int back = 4 + r.nextInt(30);
                HorrorDirector.later(back, () -> {
                    if (world.getBlockState(pos).isOf(Blocks.REDSTONE_LAMP)) {
                        world.setBlockState(pos, Blocks.SEA_LANTERN.getDefaultState(), Block.NOTIFY_LISTENERS);
                    }
                });
                return;
            }
        }
    }

    /** Decorates a chunk of a realm once (maze walls, trees, caves...). */
    public static void ensureDecorated(ServerWorld world, Realm realm, int cx, int cz) {
        DecoratedChunks dc = DecoratedChunks.get(world);
        long key = ChunkPos.toLong(cx, cz);
        if (dc.isDone(key)) return;
        dc.mark(key);                         // mark first: decoration can't re-enter itself
        world.getChunk(cx, cz);
        RealmGen.decorate(realm, RealmBlocks.sink(world, realm), cx, cz);
    }
}
