package oas.dreyka.tripoddawn.entity;

import oas.dreyka.tripoddawn.sound.TripodDawnSounds;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.BreakDoorGoal;
import net.minecraft.world.entity.ai.goal.FleeSunGoal;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RestrictSunGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.golem.IronGolem;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.npc.villager.AbstractVillager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animatable.manager.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.animation.object.PlayState;
import software.bernie.geckolib.animation.state.AnimationTest;
import software.bernie.geckolib.util.GeckoLibUtil;

/**
 * The foot soldier. It walks, it opens what it can and breaks what it cannot, and it keeps out of
 * the sun. The only creature in the mod a player can fight without planning for it.
 */
public class MartianEntity extends Monster implements GeoEntity {

    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("animation.martian.idle");
    private static final RawAnimation WALK = RawAnimation.begin().thenLoop("animation.martian.walk");
    private static final RawAnimation SWIM = RawAnimation.begin().thenLoop("animation.martian.swim");
    private static final RawAnimation ATTACK = RawAnimation.begin().thenPlay("animation.martian.attack");
    private static final RawAnimation DEATH = RawAnimation.begin().thenPlayAndHold("animation.martian.death");

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public MartianEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        this.xpReward = 20;
        this.setCanPickUpLoot(false);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MOVEMENT_SPEED, 0.25)
                .add(Attributes.MAX_HEALTH, 40.0)
                .add(Attributes.ARMOR, 0.0)
                .add(Attributes.ATTACK_DAMAGE, 8.0)
                // Covers the forty-four blocks the invasion drops one at with room left over, so a
                // martian walks at the player it was sent for instead of waiting to be walked into,
                // and keeps walking when that player backs off rather than losing them at the edge.
                .add(Attributes.FOLLOW_RANGE, 64.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.25);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new RestrictSunGoal(this));
        this.goalSelector.addGoal(2, new FleeSunGoal(this, 1.0));
        this.goalSelector.addGoal(3, new BreakDoorGoal(this, difficulty -> true));
        this.goalSelector.addGoal(4, new MeleeAttackGoal(this, 1.0, false));
        this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.8));
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8.0f));
        this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));

        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
        this.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, AbstractVillager.class, true));
        this.targetSelector.addGoal(4, new NearestAttackableTargetGoal<>(this, IronGolem.class, true));
        this.targetSelector.addGoal(5, new NearestAttackableTargetGoal<>(this, Animal.class, true));
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return TripodDawnSounds.MARTIAN_GROWL;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return TripodDawnSounds.MARTIAN_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return TripodDawnSounds.MARTIAN_DEATH;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>("main", 5, this::pickAnimation));
        // Explicit type argument: a lambda gives the diamond nothing to infer from, so `test` would
        // come back raw and the animatable with it.
        controllers.add(new AnimationController<MartianEntity>("swing", 0, test ->
                test.animatable().swinging ? test.setAndContinue(ATTACK) : PlayState.STOP));
    }

    private PlayState pickAnimation(AnimationTest<MartianEntity> test) {
        MartianEntity martian = test.animatable();
        if (martian.isDeadOrDying()) {
            return test.setAndContinue(DEATH);
        }
        if (martian.isInWater()) {
            return test.setAndContinue(SWIM);
        }
        return test.setAndContinue(test.isMoving() ? WALK : IDLE);
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }
}
