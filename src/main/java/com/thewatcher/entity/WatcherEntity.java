package com.thewatcher.entity;

import com.thewatcher.horror.Dimensions;
import com.thewatcher.horror.Fx;
import com.thewatcher.horror.HorrorEvent;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * The Watcher. A tall black figure with white eyes.
 *
 * STALK - stands far away and stares. Vanishes the moment you look straight at it, or come close.
 * SCARE - waits right behind you. Turn around and it screams in your face, then it's gone.
 * HUNT  - stage 4, night only. It stops pretending and comes for you until sunrise.
 */
public class WatcherEntity extends HostileEntity {
    public enum Mode { STALK, SCARE, HUNT }

    private static final int STALK_LIFETIME = 20 * 60;       // 1 minute
    private static final int SCARE_LIFETIME = 20 * 25;
    private static final int HUNT_LIFETIME = 20 * 60 * 4;    // 4 minutes, or sunrise

    // Synced to clients so the 3D model can switch poses.
    private static final TrackedData<Boolean> HUNTING = DataTracker.registerData(WatcherEntity.class, TrackedDataHandlerRegistry.BOOLEAN);
    private static final TrackedData<Boolean> SCREAMING = DataTracker.registerData(WatcherEntity.class, TrackedDataHandlerRegistry.BOOLEAN);

    private Mode mode = Mode.STALK;
    @Nullable private UUID targetId;
    private int life;
    private int lookedAtTicks;
    private int scareCountdown = -1;
    private boolean noticed;
    /** Dread stage of its target when it was sent. 3+ = it doesn't just vanish when you look at it. */
    private int boldness;
    private int blinks;
    private boolean waitingForLookAway;
    private Vec3d anchor = Vec3d.ZERO;

    public WatcherEntity(EntityType<? extends HostileEntity> type, World world) {
        super(type, world);
        this.experiencePoints = 0;
    }

    public static DefaultAttributeContainer.Builder createWatcherAttributes() {
        return HostileEntity.createHostileAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 80.0)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.34)
                .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 9.0)
                .add(EntityAttributes.GENERIC_FOLLOW_RANGE, 96.0)
                .add(EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE, 1.0)
                .add(EntityAttributes.GENERIC_STEP_HEIGHT, 1.5);
    }

    @Override
    protected void initDataTracker(DataTracker.Builder builder) {
        super.initDataTracker(builder);
        builder.add(HUNTING, false);
        builder.add(SCREAMING, false);
    }

    @Override
    protected void initGoals() {
        this.goalSelector.add(1, new MeleeAttackGoal(this, 1.1, true) {
            @Override public boolean canStart() { return isHunting() && super.canStart(); }
            @Override public boolean shouldContinue() { return isHunting() && super.shouldContinue(); }
        });
    }

    public void setup(Mode mode, ServerPlayerEntity target) {
        setup(mode, target, 1);
    }

    public void setup(Mode mode, ServerPlayerEntity target, int boldness) {
        this.boldness = boldness;
        this.mode = mode;
        this.targetId = target.getUuid();
        this.anchor = target.getPos();
        this.dataTracker.set(HUNTING, mode == Mode.HUNT);
        if (mode == Mode.HUNT) this.setTarget(target);
    }

    public Mode getMode() { return mode; }
    public boolean isHunting() { return mode == Mode.HUNT; }
    /** Client-safe versions (read from synced data). */
    public boolean isHuntingClient() { return this.dataTracker.get(HUNTING); }
    public boolean isScreaming() { return this.dataTracker.get(SCREAMING); }

    /** Client-side only: for the full-screen jumpscare dummy. */
    public void setScreamingLocal(boolean screaming) { this.dataTracker.set(SCREAMING, screaming); }
    @Nullable public UUID getTargetId() { return targetId; }

    @Override
    public void tick() {
        super.tick();
        if (!(this.getWorld() instanceof ServerWorld world)) return;

        // Spawned from an egg (or reloaded from disk): latch on to whoever is nearest.
        if (targetId == null) {
            PlayerEntity nearest = world.getClosestPlayer(this, 64);
            if (nearest instanceof ServerPlayerEntity sp && !sp.isSpectator()) {
                setup(Mode.STALK, sp);
            } else {
                vanish(false);
                return;
            }
        }

        PlayerEntity p = world.getPlayerByUuid(targetId);
        if (!(p instanceof ServerPlayerEntity player) || !player.isAlive() || player.isSpectator()) {
            vanish(false);
            return;
        }

        life++;
        if (life % 14 == 0) com.thewatcher.horror.GoreFx.drip(world, this, 0.72, 0.25);
        switch (mode) {
            case STALK -> tickStalk(player);
            case SCARE -> tickScare(world, player);
            case HUNT -> tickHunt(world, player);
        }
    }

    private void tickStalk(ServerPlayerEntity player) {
        this.getNavigation().stop();
        this.getLookControl().lookAt(player, 360f, 360f);

        double dist = this.distanceTo(player);
        boolean seen = player.canSee(this) && Fx.isLookingAt(player, this.getEyePos(), 4.0);

        if (seen && !waitingForLookAway) {
            if (!noticed) {
                noticed = true;
                // A single, quiet heartbeat the moment you notice it.
                Fx.sound(player, com.thewatcher.ModSounds.HEARTBEAT, player.getPos(), 1.0f, 0.85f);
                Fx.sound(player, com.thewatcher.ModSounds.BREATHING, this.getEyePos(), 0.6f, 0.9f);
            }
            // It lets you look... for a moment.
            if (++lookedAtTicks > 18) {
                if (boldness < 3) {
                    vanish(true);
                    return;
                }
                waitingForLookAway = true;   // bold: it waits for you to look away...
            }
        }

        // ...and then it's closer. Every time.
        if (waitingForLookAway && !seen && Fx.isBehind(player, this.getPos()) && this.getWorld() instanceof ServerWorld world) {
            waitingForLookAway = false;
            lookedAtTicks = 0;
            blinks++;
            Vec3d toward = this.getPos().lerp(player.getPos(), 0.45);
            BlockPos spot = Fx.findStandingSpot(world, toward.x, toward.z, (int) toward.y, 6);
            if (spot != null && blinks <= 3) {
                this.refreshPositionAndAngles(toward.x, spot.getY(), toward.z, this.getYaw(), 0f);
                Fx.sound(player, SoundEvents.ENTITY_ENDERMAN_TELEPORT, this.getPos(), 0.3f, 0.4f);
                dist = this.distanceTo(player);
            } else if (boldness >= 4 && !world.isDay()) {
                // Done playing. It comes for you.
                this.mode = Mode.HUNT;
                this.life = 0;
                this.dataTracker.set(HUNTING, true);
                this.setTarget(player);
                Fx.sound(player, com.thewatcher.ModSounds.WATCHER_SCREAM, this.getEyePos(), 2.5f, 0.9f);
                return;
            } else {
                vanish(true);
                Fx.effect(player, StatusEffects.DARKNESS, 80, 0);
                return;
            }
        }

        if (dist < 9.0 || life > STALK_LIFETIME) {
            vanish(dist < 9.0);
        }
    }

    private void tickScare(ServerWorld world, ServerPlayerEntity player) {
        this.getNavigation().stop();
        this.getLookControl().lookAt(player, 360f, 360f);

        if (scareCountdown >= 0) {
            if (--scareCountdown <= 0) {
                Fx.effect(player, StatusEffects.BLINDNESS, 30, 0);
                vanish(true);
            }
            return;
        }

        // If the player wandered off, quietly reposition right behind them again (only while unseen).
        if (player.getPos().squaredDistanceTo(anchor) > 9.0 && Fx.isBehind(player, this.getPos())) {
            Vec3d behind = player.getPos().subtract(Fx.flatLook(player).multiply(1.6));
            BlockPos spot = Fx.findStandingSpot(world, behind.x, behind.z, player.getBlockY(), 1);
            if (spot != null) {
                this.refreshPositionAndAngles(behind.x, spot.getY(), behind.z, player.getYaw(), 0f);
                anchor = player.getPos();
            }
        }

        if (player.canSee(this) && Fx.isLookingAt(player, this.getEyePos(), 30.0)) {
            // BOO.
            Fx.sound(player, com.thewatcher.ModSounds.WATCHER_SCREAM, this.getEyePos(), 1.6f, 1.0f);
            Fx.effect(player, StatusEffects.DARKNESS, 100, 0);
            Fx.effect(player, StatusEffects.NAUSEA, 120, 0);
            player.damage(this.getDamageSources().mobAttack(this), 1.0f);
            this.dataTracker.set(SCREAMING, true);
            com.thewatcher.horror.Scares.send(player, com.thewatcher.net.ScarePayload.JUMPSCARE, 0);   // its face, in yours
            scareCountdown = 9; // long enough to see its face
            return;
        }

        if (life > SCARE_LIFETIME) vanish(false);
    }

    private void tickHunt(ServerWorld world, ServerPlayerEntity player) {
        this.setTarget(player);

        if (world.isDay() || life > HUNT_LIFETIME) {
            vanish(true);
            return;
        }

        // Too far away? It doesn't walk. It's just... closer now.
        if (this.squaredDistanceTo(player) > 48 * 48 && life % 40 == 0) {
            Vec3d behind = player.getPos().subtract(Fx.flatLook(player).multiply(22));
            BlockPos spot = Fx.findStandingSpot(world, behind.x, behind.z, player.getBlockY(), 10);
            if (spot != null) {
                this.refreshPositionAndAngles(spot.getX() + 0.5, spot.getY(), spot.getZ() + 0.5, this.getYaw(), 0f);
                this.getNavigation().stop();
            }
        }

        // It puts the lights out around you.
        if (life % 60 == 0 && world.getGameRules().getBoolean(com.thewatcher.TheWatcher.GRIEFING)) {
            HorrorEvent.extinguish(world, player, 7, 2);
        }

        // Your heartbeat speeds up the closer it gets.
        double dist = this.distanceTo(player);
        int interval = dist < 8 ? 10 : dist < 20 ? 18 : 30;
        if (life % interval == 0) {
            float pitch = dist < 8 ? 1.25f : dist < 20 ? 1.0f : 0.8f;
            Fx.sound(player, com.thewatcher.ModSounds.HEARTBEAT, player.getPos(), 1.2f, pitch + 0.25f);
        }
    }

    @Override
    public boolean tryAttack(net.minecraft.entity.Entity target) {
        boolean hit = super.tryAttack(target);
        if (hit && this.getWorld() instanceof ServerWorld sw) com.thewatcher.horror.GoreFx.spray(sw, target);
        // Sometimes, when it catches you, it doesn't kill you. It takes you with it.
        if (hit && isHunting() && target instanceof ServerPlayerEntity p && p.isAlive()
                && this.random.nextFloat() < 0.35f
                && Dimensions.send(p, this.random.nextBoolean() ? com.thewatcher.gen.Realm.HOLLOW : com.thewatcher.gen.Realm.FLESH)) {
            vanish(true);
        }
        return hit;
    }

    /** Disappear in a puff of black smoke. */
    public void vanish(boolean withEffect) {
        if (this.getWorld() instanceof ServerWorld world && withEffect) {
            world.spawnParticles(ParticleTypes.LARGE_SMOKE, getX(), getBodyY(0.5), getZ(), 25, 0.3, 0.9, 0.3, 0.01);
            world.spawnParticles(ParticleTypes.SQUID_INK, getX(), getBodyY(0.5), getZ(), 10, 0.2, 0.6, 0.2, 0.02);
        }
        this.discard();
    }

    // ---- It can't be hurt unless it's hunting. Hitting it just makes it leave. ----

    @Override
    public boolean damage(DamageSource source, float amount) {
        if (!isHuntingClient()) {
            if (!this.getWorld().isClient) vanish(true);
            return false;
        }
        boolean hurt = super.damage(source, amount);
        if (hurt && this.getWorld() instanceof ServerWorld sw) com.thewatcher.horror.GoreFx.spray(sw, this);
        return hurt;
    }

    @Override
    public boolean handleFallDamage(float fallDistance, float damageMultiplier, DamageSource damageSource) {
        return false;
    }

    @Override
    public boolean canImmediatelyDespawn(double distanceSquared) {
        return false; // we manage its lifetime ourselves
    }

    @Override
    public boolean isPushable() {
        return isHuntingClient();
    }

    // Silent. Always silent.
    @Nullable @Override protected SoundEvent getAmbientSound() { return null; }
    @Override protected SoundEvent getHurtSound(DamageSource source) { return SoundEvents.ENTITY_WARDEN_HURT; }
    @Override protected SoundEvent getDeathSound() { return SoundEvents.ENTITY_WARDEN_DEATH; }
    @Override protected float getSoundVolume() { return 0.6f; }
}
