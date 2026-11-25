package com.thewatcher.entity;

import net.minecraft.block.BlockState;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ai.goal.ActiveTargetGoal;
import net.minecraft.entity.ai.goal.LookAroundGoal;
import net.minecraft.entity.ai.goal.LookAtEntityGoal;
import net.minecraft.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.entity.ai.goal.PounceAtTargetGoal;
import net.minecraft.entity.ai.goal.RevengeGoal;
import net.minecraft.entity.ai.goal.SwimGoal;
import net.minecraft.entity.ai.goal.WanderAroundFarGoal;
import net.minecraft.entity.ai.pathing.EntityNavigation;
import net.minecraft.entity.ai.pathing.SpiderNavigation;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * The Crawler: skitters on all fours (on its back), climbs walls like a spider, pounces.
 * Burns in sunlight. Lives in caves, dark places and the Hallways.
 */
public class CrawlerEntity extends HostileEntity {
    private static final TrackedData<Boolean> CLIMBING = DataTracker.registerData(CrawlerEntity.class, TrackedDataHandlerRegistry.BOOLEAN);

    public CrawlerEntity(EntityType<? extends HostileEntity> type, World world) {
        super(type, world);
        this.experiencePoints = 8;
    }

    public static DefaultAttributeContainer.Builder createCrawlerAttributes() {
        return HostileEntity.createHostileAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 26.0)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.34)
                .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 5.0)
                .add(EntityAttributes.GENERIC_FOLLOW_RANGE, 48.0)
                .add(EntityAttributes.GENERIC_ARMOR, 2.0);
    }

    @Override
    protected void initDataTracker(DataTracker.Builder builder) {
        super.initDataTracker(builder);
        builder.add(CLIMBING, false);
    }

    @Override
    protected void initGoals() {
        this.goalSelector.add(1, new SwimGoal(this));
        this.goalSelector.add(2, new PounceAtTargetGoal(this, 0.45f));
        this.goalSelector.add(3, new MeleeAttackGoal(this, 1.3, true));
        this.goalSelector.add(5, new WanderAroundFarGoal(this, 0.8));
        this.goalSelector.add(6, new LookAtEntityGoal(this, PlayerEntity.class, 16f));
        this.goalSelector.add(7, new LookAroundGoal(this));
        this.targetSelector.add(1, new RevengeGoal(this));
        this.targetSelector.add(2, new ActiveTargetGoal<>(this, PlayerEntity.class, false));
    }

    @Override
    protected EntityNavigation createNavigation(World world) {
        return new SpiderNavigation(this, world);
    }

    @Override
    public void tick() {
        super.tick();
        if (!this.getWorld().isClient) this.dataTracker.set(CLIMBING, this.horizontalCollision);
    }

    @Override
    public boolean isClimbing() {
        return this.dataTracker.get(CLIMBING);
    }

    @Override
    public void tickMovement() {
        if (this.isAlive() && this.isAffectedByDaylight()) this.setOnFireFor(8f);
        super.tickMovement();
    }

    @Override
    public boolean handleFallDamage(float fallDistance, float damageMultiplier, DamageSource damageSource) {
        return false;
    }

    @Override protected SoundEvent getAmbientSound() { return this.random.nextBoolean() ? com.thewatcher.ModSounds.CRAWLER_CHITTER : com.thewatcher.ModSounds.CRAWLER_SKITTER; }
    @Override protected SoundEvent getHurtSound(DamageSource source) { return SoundEvents.ENTITY_HUSK_HURT; }
    @Override protected SoundEvent getDeathSound() { return SoundEvents.ENTITY_HUSK_DEATH; }
    @Override public float getSoundPitch() { return 0.45f + this.random.nextFloat() * 0.15f; }

    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {
        this.playSound(SoundEvents.ENTITY_SPIDER_STEP, 0.2f, 1.6f);
    }

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
        if (this.age % 16 == 0 && this.getWorld() instanceof net.minecraft.server.world.ServerWorld sw) {
            com.thewatcher.horror.GoreFx.drip(sw, this, 0.55, 0.7);
        }
    }
}
