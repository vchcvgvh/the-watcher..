package com.thewatcher.entity;

import com.thewatcher.horror.Fx;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ai.goal.ActiveTargetGoal;
import net.minecraft.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

/**
 * The Faceless. It cannot move while anyone is looking at it. Look away and it moves fast; if it's far,
 * it skips closer. Every time you look back it's frozen in a different pose. Hits very hard.
 */
public class FacelessEntity extends HostileEntity {
    private static final TrackedData<Boolean> FROZEN = DataTracker.registerData(FacelessEntity.class, TrackedDataHandlerRegistry.BOOLEAN);
    private static final TrackedData<Integer> POSE = DataTracker.registerData(FacelessEntity.class, TrackedDataHandlerRegistry.INTEGER);

    private int stingCooldown;

    public FacelessEntity(EntityType<? extends HostileEntity> type, World world) {
        super(type, world);
        this.experiencePoints = 15;
    }

    public static DefaultAttributeContainer.Builder createFacelessAttributes() {
        return HostileEntity.createHostileAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 60.0)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.42)
                .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 10.0)
                .add(EntityAttributes.GENERIC_FOLLOW_RANGE, 64.0)
                .add(EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE, 1.0)
                .add(EntityAttributes.GENERIC_STEP_HEIGHT, 1.1);
    }

    @Override
    protected void initDataTracker(DataTracker.Builder builder) {
        super.initDataTracker(builder);
        builder.add(FROZEN, false);
        builder.add(POSE, 0);
    }

    @Override
    protected void initGoals() {
        this.goalSelector.add(1, new MeleeAttackGoal(this, 1.0, true));
        this.targetSelector.add(1, new ActiveTargetGoal<>(this, PlayerEntity.class, false));
    }

    public boolean isFrozen() { return this.dataTracker.get(FROZEN); }
    public int getFrozenPose() { return this.dataTracker.get(POSE); }

    /** Client-side only: for the full-screen jumpscare dummy. */
    public void setFrozenLocal(boolean frozen, int pose) {
        this.dataTracker.set(FROZEN, frozen);
        this.dataTracker.set(POSE, pose);
    }

    @Override
    public void tick() {
        super.tick();
        if (!(this.getWorld() instanceof ServerWorld world)) return;
        if (stingCooldown > 0) stingCooldown--;

        @Nullable ServerPlayerEntity watcher = null;
        for (ServerPlayerEntity p : world.getPlayers()) {
            if (p.isSpectator() || p.squaredDistanceTo(this) > 64 * 64) continue;
            if (p.canSee(this) && Fx.isLookingAt(p, this.getBoundingBox().getCenter(), 50.0)) {
                watcher = p;
                break;
            }
        }
        boolean seen = watcher != null;
        if (seen != isFrozen()) {
            this.dataTracker.set(FROZEN, seen);
            if (seen) {
                this.dataTracker.set(POSE, this.random.nextInt(6));
                // Turn around and it's right there.
                if (stingCooldown == 0 && watcher.squaredDistanceTo(this) < 7 * 7) {
                    Fx.sound(watcher, com.thewatcher.ModSounds.FACELESS_HUM, this.getEyePos(), 1.0f, 0.9f);
                    stingCooldown = 200;
                }
            }
        }
        this.setAiDisabled(seen);
        if (seen) {
            this.setVelocity(0, Math.min(0, this.getVelocity().y), 0);
            this.getNavigation().stop();
            return;
        }

        // Unseen and far from its prey: skip forward a few blocks at a time.
        if (this.getTarget() instanceof ServerPlayerEntity target && this.age % 30 == 0
                && this.squaredDistanceTo(target) > 18 * 18) {
            Vec3d dir = target.getPos().subtract(this.getPos()).multiply(1, 0, 1).normalize();
            Vec3d to = this.getPos().add(dir.multiply(6));
            BlockPos spot = Fx.findStandingSpot(world, to.x, to.z, this.getBlockY(), 4);
            if (spot != null) this.refreshPositionAndAngles(to.x, spot.getY(), to.z, this.getYaw(), 0);
        }
    }

    @Override
    public boolean isPushable() {
        return !isFrozen();
    }

    @Override
    public boolean handleFallDamage(float fallDistance, float damageMultiplier, DamageSource damageSource) {
        return false;
    }

    @Override
    public boolean canImmediatelyDespawn(double distanceSquared) {
        return distanceSquared > 96 * 96;
    }

    // It makes no sound at all, except when hurt.
    @Override protected SoundEvent getAmbientSound() { return null; }
    @Override protected SoundEvent getHurtSound(DamageSource source) { return SoundEvents.BLOCK_DEEPSLATE_HIT; }
    @Override protected SoundEvent getDeathSound() { return SoundEvents.ENTITY_WARDEN_DEATH; }
    @Override protected void playStepSound(BlockPos pos, net.minecraft.block.BlockState state) { }

    // ---- blood
    @Override
    public boolean damage(net.minecraft.entity.damage.DamageSource source, float amount) {
        boolean hurt = super.damage(source, amount);
        if (hurt && this.getWorld() instanceof net.minecraft.server.world.ServerWorld sw) com.thewatcher.horror.GoreFx.spray(sw, this);
        return hurt;
    }

    @Override
    public boolean tryAttack(net.minecraft.entity.Entity target) {
        boolean hit = super.tryAttack(target);
        if (hit && this.getWorld() instanceof net.minecraft.server.world.ServerWorld sw) com.thewatcher.horror.GoreFx.spray(sw, target);
        return hit;
    }

    @Override
    public void baseTick() {
        super.baseTick();
        if (this.age % 22 == 0 && this.getWorld() instanceof net.minecraft.server.world.ServerWorld sw) {
            com.thewatcher.horror.GoreFx.drip(sw, this, 0.86, 0.15);
        }
    }
}
