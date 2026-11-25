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
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/** The Hollow Hound: fast pack hunter. Hurt one and the whole pack comes for you. */
public class HoundEntity extends HostileEntity {
    public HoundEntity(EntityType<? extends HostileEntity> type, World world) {
        super(type, world);
        this.experiencePoints = 6;
    }

    public static DefaultAttributeContainer.Builder createHoundAttributes() {
        return HostileEntity.createHostileAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 22.0)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.36)
                .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 6.0)
                .add(EntityAttributes.GENERIC_FOLLOW_RANGE, 64.0)
                .add(EntityAttributes.GENERIC_STEP_HEIGHT, 1.1);
    }

    @Override
    protected void initGoals() {
        this.goalSelector.add(1, new SwimGoal(this));
        this.goalSelector.add(2, new PounceAtTargetGoal(this, 0.4f));
        this.goalSelector.add(3, new MeleeAttackGoal(this, 1.35, true));
        this.goalSelector.add(5, new WanderAroundFarGoal(this, 0.9));
        this.goalSelector.add(6, new LookAtEntityGoal(this, PlayerEntity.class, 20f));
        this.goalSelector.add(7, new LookAroundGoal(this));
        this.targetSelector.add(1, new RevengeGoal(this).setGroupRevenge());
        this.targetSelector.add(2, new ActiveTargetGoal<>(this, PlayerEntity.class, false));
    }

    @Override
    public boolean handleFallDamage(float fallDistance, float damageMultiplier, DamageSource damageSource) {
        return false;
    }

    @Override protected SoundEvent getAmbientSound() { return com.thewatcher.ModSounds.HOUND_GROWL; }
    @Override protected SoundEvent getHurtSound(DamageSource source) { return SoundEvents.ENTITY_WOLF_HURT; }
    @Override protected SoundEvent getDeathSound() { return SoundEvents.ENTITY_WOLF_DEATH; }
    @Override public float getSoundPitch() { return 0.5f + this.random.nextFloat() * 0.12f; }

    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {
        this.playSound(SoundEvents.ENTITY_WOLF_STEP, 0.2f, 0.7f);
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
        if (this.age % 12 == 0 && this.getWorld() instanceof net.minecraft.server.world.ServerWorld sw) {
            com.thewatcher.horror.GoreFx.drip(sw, this, 0.55, 0.75);
        }
    }
}
