package oas.dreyka.tripoddawn.entity;

import oas.dreyka.tripoddawn.particle.TripodDawnParticles;
import oas.dreyka.tripoddawn.sound.TripodDawnSounds;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animatable.manager.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.animation.object.PlayState;
import software.bernie.geckolib.animation.state.AnimationTest;
import software.bernie.geckolib.util.GeckoLibUtil;

/**
 * What the four war machines have in common.
 *
 * <p>A machine lives through three states inside one entity. It climbs out of the ground, it walks
 * and shoots, and it falls over and stays there as a wreck. The source mod spent a separate species
 * on each of the first and third, which cost eight registry entries and made the wreck lose track of
 * which machine it came from.
 *
 * <p>Nothing here blocks player damage. The source mod refused every source but two of its own
 * items, and those items are gone: armour and health are the whole defence now.
 */
public abstract class MachineEntity extends Monster implements GeoEntity {

    protected static final byte PHASE_EMERGING = 0;
    protected static final byte PHASE_ACTIVE = 1;

    /** The rise animation runs thirty seconds and holds its last frame; the state ends with it. */
    public static final int EMERGE_TICKS = 600;

    /** When the soil stops being thrown, early enough that the last of it dies with the rise. */
    private static final int DUST_TICKS = EMERGE_TICKS - 80;

    /** How long a wreck stays on the ground before it goes up. */
    private static final int WRECK_TICKS = 6000;

    /** The blast a wreck leaves. Wide enough to be a crater, short of levelling a house. */
    private static final float SCUTTLE_POWER = 3.5f;

    /** The fall lasts five seconds; past it the machine is scenery rather than a death. */
    private static final int FALL_TICKS = 100;

    /**
     * How far a machine looks for someone to walk at. It has to cover the ninety-six blocks the
     * invasion drops one at with room to spare, or the half that land past it never notice the player
     * they came for and spend the night strolling.
     */
    public static final double HUNT_RANGE = 128.0;

    /** How far it shoots, which is nearer than how far it hunts: it closes in before it fires. */
    private static final double WEAPON_RANGE = 80.0;

    /**
     * Nearer than this the machine stamps instead of firing. The ray leaves a hood twenty blocks up
     * and a target underfoot puts its blast at the machine's own legs, which is how one kills itself
     * in half a minute. It also gives a player somewhere to stand.
     */
    private static final double WEAPON_MIN_RANGE = 12.0;

    private static final EntityDataAccessor<Byte> DATA_PHASE =
            SynchedEntityData.defineId(MachineEntity.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Integer> DATA_PHASE_TICKS =
            SynchedEntityData.defineId(MachineEntity.class, EntityDataSerializers.INT);

    private static final RawAnimation SPAWN = RawAnimation.begin().thenPlayAndHold("spawn");
    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("idle");
    private static final RawAnimation WALK = RawAnimation.begin().thenLoop("walk");
    private static final RawAnimation DEATH = RawAnimation.begin().thenPlayAndHold("death");
    private static final RawAnimation GROUND = RawAnimation.begin().thenLoop("ground");

    /** The wreck model carries a single clip, and it is the pose the machine keeps. */
    private static final RawAnimation WRECK = RawAnimation.begin().thenPlayAndHold("death");

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    private int aimTicks;
    private int shotCooldown;
    private int engineTicks;
    private HeatRayProjectile.Mode mode = HeatRayProjectile.Mode.CHARGED;
    private MachinePart[] parts;

    protected MachineEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        this.xpReward = experience();
        // A machine bigger than the size it is registered at says so through the scale attribute, and
        // an attribute fires no change event for the value it was built with. Without this the box
        // stays the registered one for good: a forty block titan that is twenty-four blocks tall to
        // everything the server measures, its own hit slabs included.
        refreshDimensions();
    }

    // ---- what each machine answers for itself ----

    /** Prefix of the animation names in this machine's animation file. */
    protected abstract String animationPrefix();

    /**
     * Prefix of the collapsed model's clips, or null for a machine that has none and falls apart on
     * its own model. Three of the four came with a second model built for the ground.
     */
    public String wreckPrefix() {
        return null;
    }

    /**
     * The name its model, its animations and its texture are filed under while it is standing.
     *
     * <p>Null for a machine whose files are the ones the renderer was built with, which is every one
     * of them but the tripod: that species carries three builds behind a single entity type.
     */
    public String modelName() {
        return null;
    }

    /**
     * How tall this machine is drawn, in blocks, before its scale is applied.
     *
     * <p>It is not the registered box, and the two are a long way apart: a walker is drawn at forty
     * blocks and boxed at twenty-four. Growing the box to match would have the server walk the blocks
     * inside forty blocks of empty sky every tick, for every machine standing, and would wedge one
     * under any canopy tall enough to clear its hood. The box keeps colliding and pathing at the size
     * that is cheap; the slabs a shot lands on and the height the ray leaves from are cut from this.
     */
    public abstract float modelHeight();

    /** The same, at the size this machine actually stands. */
    public float drawnHeight() {
        return modelHeight() * getScale();
    }

    protected abstract int experience();

    /** The horn this machine sounds when it reaches the surface. */
    protected abstract SoundEvent hornSound();

    // ---- registry-side state ----

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_PHASE, PHASE_EMERGING);
        builder.define(DATA_PHASE_TICKS, 0);
    }

    public byte phase() {
        return this.entityData.get(DATA_PHASE);
    }

    public int phaseTicks() {
        return this.entityData.get(DATA_PHASE_TICKS);
    }

    public boolean emerging() {
        return phase() == PHASE_EMERGING;
    }

    /** Down for good. The five minutes it then spends on the ground are scenery, not an agony. */
    public boolean fallen() {
        return this.isDeadOrDying() && this.deathTime >= FALL_TICKS;
    }

    /** Past the fall, when the renderer swaps to the collapsed model. */
    public boolean wrecked() {
        return fallen() && wreckPrefix() != null;
    }

    /** Skips the rise, for whoever asked for a machine rather than for a night. */
    public void skipEmerge() {
        this.entityData.set(DATA_PHASE, PHASE_ACTIVE);
        this.entityData.set(DATA_PHASE_TICKS, 0);
    }

    /**
     * A machine stays where it came up.
     *
     * <p>It arrives fifty blocks away, so a player who walks off in the other direction puts it past
     * the range the game deletes a monster at, and it is gone before it is ever fought. The number
     * of machines the world holds is capped by the rung instead. Martians keep the ordinary rule,
     * which is what stops a week of nights from piling up.
     */
    @Override
    public boolean removeWhenFarAway(double distanceToPlayer) {
        return false;
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putByte("Phase", phase());
        output.putInt("PhaseTicks", phaseTicks());
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        // A machine arriving without a phase has not arrived yet: summon writes no tag, and reading
        // that as a standing machine is what skips the rise on everything but a natural night.
        this.entityData.set(DATA_PHASE, input.getByteOr("Phase", PHASE_EMERGING));
        this.entityData.set(DATA_PHASE_TICKS, input.getIntOr("PhaseTicks", 0));
    }

    // ---- behaviour ----

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.0, false));
        this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.8));
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 24.0f));
        this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));

        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new HuntPlayerGoal(this));
        this.targetSelector.addGoal(3, new HuntLifeGoal(this));
    }

    /** Anything the invasion brought with it. A machine neither aims at these nor burns them. */
    public static boolean invader(Entity entity) {
        return entity instanceof MachineEntity || entity instanceof MartianEntity
                || entity instanceof MachinePart;
    }

    /**
     * A machine picks a player out without having to see them first.
     *
     * <p>The ordinary goal needs a clear line from the attacker's eyes, which sit twenty blocks off
     * the ground on something this tall: a single rise between it and a player on foot is enough to
     * make it stand still all night, which is backwards for a thing that towers over the trees. It
     * still has to see its target to shoot, and that test is in {@link #tickWeapon}.
     */
    private static final class HuntPlayerGoal extends NearestAttackableTargetGoal<Player> {
        private HuntPlayerGoal(MachineEntity machine) {
            super(machine, Player.class, 10, false, false, null);
            this.targetConditions.ignoreLineOfSight();
        }
    }

    /**
     * Everything else alive, which is the point of the thing: a harvester walks past a cow and a
     * villager because a list of two classes did not have them on it.
     *
     * <p>Hostile mobs are left off. They are on the invasion's side by accident of who they fight,
     * its own martians are monsters too, and a machine spending a night clearing the zombies off a
     * village would be defending it.
     *
     * <p>Searched over the range it can shoot at rather than the hundred blocks it hunts a player
     * over: this one walks every living entity in its box, and a siege night stands ten of them.
     */
    private static final class HuntLifeGoal extends NearestAttackableTargetGoal<LivingEntity> {
        private static final double SEARCH_RANGE = 64.0;

        private HuntLifeGoal(MachineEntity machine) {
            super(machine, LivingEntity.class, 20, false, false,
                    (living, level) -> !(living instanceof Monster) && !(living instanceof Player));
        }

        @Override
        protected AABB getTargetSearchArea(double range) {
            return super.getTargetSearchArea(Math.min(range, SEARCH_RANGE));
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (!(this.level() instanceof ServerLevel server)) {
            return;
        }
        tickParts(server);

        if (emerging()) {
            tickEmerge(server);
            return;
        }
        tickEngine();
        tickWeapon(server);
    }

    // ---- where a shot lands ----

    /**
     * The slabs a shot can land on.
     *
     * <p>Built here rather than at spawn because they are never saved: a machine that comes back with
     * the world has to grow them again, and the tick after loading is the first moment it can.
     */
    private void tickParts(ServerLevel server) {
        if (this.isDeadOrDying()) {
            dropParts();
            return;
        }
        // Rebuilt rather than only built, because a slab is an entity like any other and anything
        // that clears entities takes them with it. A machine that kept the dead references would
        // stand there unhittable, which reads as invulnerability rather than as a missing box.
        if (this.parts == null || this.parts[0].isRemoved()) {
            MachinePart.Zone[] zones = MachinePart.Zone.values();
            this.parts = new MachinePart[zones.length];
            for (int i = 0; i < zones.length; i++) {
                this.parts[i] = new MachinePart(this, zones[i]);
                server.addFreshEntity(this.parts[i]);
            }
            return;
        }
        for (MachinePart part : this.parts) {
            part.follow();
        }
    }

    private void dropParts() {
        if (this.parts == null) {
            return;
        }
        for (MachinePart part : this.parts) {
            part.discard();
        }
        this.parts = null;
    }

    /**
     * Nothing lands on the machine itself, only on its slabs.
     *
     * <p>Two boxes over the same metal would mean a shot worth double when it clips the outer one
     * and normal when it clips the inner, decided by which the game happened to test first.
     */
    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public void remove(RemovalReason reason) {
        dropParts();
        super.remove(reason);
    }

    /** Held in place, silent apart from its own rise, and unable to walk out of the animation. */
    private void tickEmerge(ServerLevel server) {
        int t = phaseTicks();
        if (t == 0) {
            playSound(TripodDawnSounds.MACHINE_SPAWN, 8.0f, 1.0f);
            MachineArrival.announce(server, this);
        }
        this.setDeltaMovement(0.0, Math.min(0.0, this.getDeltaMovement().y), 0.0);
        this.setTarget(null);
        MachineArrival.lightning(server, this, t);

        // The soil stops flying a few seconds before the animation ends, so the last of it has burnt
        // out by the time the machine is standing and nothing is left lying around its feet.
        if (t < DUST_TICKS) {
            MachineArrival.dust(server, this);
        }

        // The horn lands late in the animation, once the hood is clear of the ground.
        if (t == EMERGE_TICKS - 120) {
            playSound(hornSound(), 12.0f, 1.0f);
        }

        if (t >= EMERGE_TICKS) {
            this.entityData.set(DATA_PHASE, PHASE_ACTIVE);
            this.entityData.set(DATA_PHASE_TICKS, 0);
            return;
        }
        this.entityData.set(DATA_PHASE_TICKS, t + 1);
    }

    /**
     * The engine is played from the machine rather than pushed to every player each tick. The source
     * mod broadcast a packet per living machine per tick to do the same job.
     */
    private void tickEngine() {
        if (--this.engineTicks > 0) {
            return;
        }
        this.engineTicks = 100 + this.random.nextInt(60);
        playSound(TripodDawnSounds.MACHINE_ENGINE, 6.0f, 0.9f + this.random.nextFloat() * 0.2f);
    }

    private void tickWeapon(ServerLevel server) {
        if (this.shotCooldown > 0) {
            this.shotCooldown--;
        }

        LivingEntity target = this.getTarget();
        if (target == null || !target.isAlive() || this.shotCooldown > 0) {
            this.aimTicks = 0;
            return;
        }
        double reach = this.distanceToSqr(target);
        if (reach > WEAPON_RANGE * WEAPON_RANGE || reach < WEAPON_MIN_RANGE * WEAPON_MIN_RANGE
                || !this.hasLineOfSight(target)) {
            this.aimTicks = 0;
            return;
        }

        // Distance picks the shot, and it is picked once at the start of the wind-up rather than
        // read again every tick: a player walking across the threshold mid-aim would otherwise turn
        // a charged shot into a snap one halfway through, and the warning they had been reading
        // would have been a lie.
        if (++this.aimTicks == 1) {
            this.mode = reach > HeatRayProjectile.CHARGE_FROM * HeatRayProjectile.CHARGE_FROM
                    ? HeatRayProjectile.Mode.CHARGED
                    : HeatRayProjectile.Mode.QUICK;
            playSound(TripodDawnSounds.MACHINE_SHOOT, 8.0f,
                    this.mode == HeatRayProjectile.Mode.QUICK ? 1.4f : 0.85f);
        }
        HeatRayProjectile.charge(server, this, this.mode, this.aimTicks);
        if (this.aimTicks < this.mode.aim()) {
            return;
        }

        HeatRayProjectile.fire(server, this, target, this.mode);
        this.aimTicks = 0;
        this.shotCooldown = this.mode.cooldown();
    }

    // ---- death, then the wreck ----

    @Override
    protected void tickDeath() {
        this.deathTime++;
        if (this.deathTime < WRECK_TICKS || !(this.level() instanceof ServerLevel server)) {
            return;
        }
        scuttle(server);
        this.remove(RemovalReason.KILLED);
    }

    /**
     * What a wreck does instead of blinking out.
     *
     * <p>Five minutes of a machine lying in a field is scenery, and scenery that vanishes between two
     * glances reads as the game forgetting it. Bound to the vanilla mob griefing rule like every other
     * blast this mod sets off, so a server that turned block damage off has already said so.
     */
    private void scuttle(ServerLevel server) {
        // Counts kept low on purpose: the mod's own flame quads are three blocks across, so a
        // handful of them reads as a blast and forty of them paints the whole screen orange.
        double y = this.getY() + 1.0;
        TripodDawnParticles.send(server, ParticleTypes.EXPLOSION_EMITTER,
                this.getX(), y, this.getZ(), 2, 1.4, 0.6, 1.4, 0.0);
        TripodDawnParticles.send(server, TripodDawnParticles.BLAST,
                this.getX(), y, this.getZ(), 5, 0.9, 0.5, 0.9, 0.05);
        TripodDawnParticles.send(server, TripodDawnParticles.HEAT_RAY,
                this.getX(), y, this.getZ(), 8, 1.1, 0.7, 1.1, 0.08);
        TripodDawnParticles.send(server, ParticleTypes.LARGE_SMOKE,
                this.getX(), y, this.getZ(), 24, 2.0, 1.2, 2.0, 0.05);

        // No fire, unlike the ray. The ray burns something a player is looking at during a fight; this
        // goes off five minutes after one ended, as often as not next to the base they went back to.
        server.explode(this, this.getX(), y, this.getZ(), SCUTTLE_POWER, false,
                Level.ExplosionInteraction.MOB);
    }

    /** A wreck is scenery: it neither pushes nor is pushed. */
    @Override
    public boolean isPushable() {
        return !this.isDeadOrDying() && !emerging();
    }

    @Override
    protected void pushEntities() {
        if (!this.isDeadOrDying()) {
            super.pushEntities();
        }
    }

    @Override
    public boolean hurtServer(ServerLevel server, DamageSource source, float amount) {
        // Nothing lands while the machine is still coming out of the ground: the animation has it
        // half buried, and letting it die there would leave a wreck standing in the air.
        if (emerging()) {
            return false;
        }
        return super.hurtServer(server, source, amount);
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return TripodDawnSounds.MACHINE_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return TripodDawnSounds.MACHINE_DEATH;
    }

    @Override
    protected float getSoundVolume() {
        return 6.0f;
    }

    @Override
    public SoundSource getSoundSource() {
        return SoundSource.HOSTILE;
    }

    // ---- GeckoLib ----

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>("main", 5, this::pickAnimation));
    }

    private PlayState pickAnimation(AnimationTest<MachineEntity> test) {
        MachineEntity machine = test.animatable();
        String p = "animation." + machine.animationPrefix() + ".";

        if (machine.wrecked()) {
            return test.setAndContinue(prefixed(WRECK, "animation." + machine.wreckPrefix() + "."));
        }
        if (machine.isDeadOrDying()) {
            // The fall plays once, then the ground pose holds for as long as the wreck lasts. Only
            // the emperorpod gets this far, the other three have collapsed onto their second model.
            return test.setAndContinue(prefixed(machine.deathTime < FALL_TICKS ? DEATH : GROUND, p));
        }
        if (machine.emerging()) {
            PlayState state = test.setAndContinue(prefixed(SPAWN, p));
            // The rise is a thirty second clip laid over a counter the server owns, so it is driven
            // from that counter rather than from whenever this client started drawing the machine.
            // Without it, dying beside one and coming back has it climb out of the ground again.
            test.controller().setAnimationTime(machine.phaseTicks() / 20.0);
            return state;
        }
        return test.setAndContinue(prefixed(test.isMoving() ? WALK : IDLE, p));
    }

    /**
     * The four animation files each name their clips after the model they were authored against, so
     * the shared controller cannot hold one finished RawAnimation. Rebuilt per call rather than
     * cached per machine: the object is three fields and the controller only asks on a state change.
     */
    private static RawAnimation prefixed(RawAnimation template, String prefix) {
        RawAnimation built = RawAnimation.begin();
        for (RawAnimation.Stage stage : template.getAnimationStages()) {
            built.then(prefix + stage.animationName(), stage.loopType());
        }
        return built;
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }

    /** Handy for the renderer and the spawner, which both work on any machine. */
    public static boolean isMachine(Mob mob) {
        return mob instanceof MachineEntity;
    }
}
