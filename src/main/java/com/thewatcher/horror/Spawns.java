package com.thewatcher.horror;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.Difficulty;

/** Placing the mod's creatures around a player. */
public final class Spawns {
    private Spawns() {}

    public static <T extends MobEntity> int countNear(ServerWorld world, EntityType<T> type, ServerPlayerEntity p, double radius) {
        return world.getEntitiesByType(type, p.getBoundingBox().expand(radius), e -> true).size();
    }

    /**
     * Spawns {@code count} of {@code type} {@code min..max} blocks from the player (behind them if {@code behind}),
     * standing on solid ground near height {@code y}, already hunting the player. Returns how many spawned.
     */
    public static <T extends MobEntity> int spawnAround(ServerWorld world, EntityType<T> type, ServerPlayerEntity p,
                                                        int min, int max, boolean behind, int count, int y, int searchHeight) {
        if (world.getDifficulty() == Difficulty.PEACEFUL) return 0;
        var r = p.getRandom();
        for (int attempt = 0; attempt < 12; attempt++) {
            double angle = behind ? 180 + (r.nextDouble() - 0.5) * 120 : r.nextDouble() * 360;
            Vec3d dir = Fx.rotateY(Fx.flatLook(p), angle);
            Vec3d at = p.getPos().add(dir.multiply(min + r.nextDouble() * (max - min)));
            BlockPos spot = Fx.findStandingSpot(world, at.x, at.z, y, searchHeight);
            if (spot == null) continue;
            int made = 0;
            for (int i = 0; i < count; i++) {
                T mob = type.create(world);
                if (mob == null) break;
                double ox = i == 0 ? 0 : (r.nextDouble() - 0.5) * 3;
                double oz = i == 0 ? 0 : (r.nextDouble() - 0.5) * 3;
                mob.refreshPositionAndAngles(spot.getX() + 0.5 + ox, spot.getY(), spot.getZ() + 0.5 + oz, r.nextFloat() * 360f, 0f);
                mob.setTarget(p);
                if (world.spawnEntity(mob)) made++;
            }
            return made;
        }
        return 0;
    }
}
