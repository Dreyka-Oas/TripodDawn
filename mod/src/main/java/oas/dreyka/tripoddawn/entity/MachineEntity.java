package oas.dreyka.tripoddawn.entity;

import oas.dreyka.tripoddawn.particle.TripodDawnParticles;
import oas.dreyka.tripoddawn.sound.TripodDawnSounds;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ChunkLevel;
import net.minecraft.server.level.FullChunkStatus;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.Ticket;
import net.minecraft.server.level.TicketType;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animatable.manager.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.animation.object.PlayState;
import software.bernie.geckolib.animation.state.AnimationTest;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

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

    /**
     * How long a wreck stays on the ground before it goes up.
     *
     * <p>Five seconds of that is the machine still falling, so what is left is three on its side,
     * venting, and then the blast. It used to be five minutes, and a field of them turned into a
     * scrapyard nobody could walk through: the kill and the crater are one moment now.
     */
    private static final int WRECK_TICKS = 160;

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

    /**
     * How high a leg steps without the machine leaving the ground.
     *
     * <p>It walks at what it wants instead of routing around it, so the ground has to be climbed
     * rather than jumped: a forty block walker hopping over a garden wall is the one thing that would
     * make it look light.
     */
    protected static final double STEP_UP = 3.0;

    /** How far it shoots, which is nearer than how far it hunts: it closes in before it fires. */
    private static final double WEAPON_RANGE = 80.0;

    /**
     * How far this one shoots, in blocks.
     *
     * <p>A method rather than the constant so the biggest machine can outrange the rest, which is
     * what its speed is traded against: it walks slower than anything else on the field and it is
     * meant to be answered by closing the distance, not by waiting it out.
     */
    public double weaponRange() {
        return WEAPON_RANGE;
    }

    /** How far this one looks for someone, which can never be shorter than how far it shoots. */
    public double huntRange() {
        return HUNT_RANGE;
    }

    /**
     * Nearer than this the machine stamps instead of firing. The ray leaves a hood twenty blocks up
     * and a target underfoot puts its blast at the machine's own legs, which is how one kills itself
     * in half a minute. It also gives a player somewhere to stand.
     */
    private static final double WEAPON_MIN_RANGE = 12.0;

    /**
     * Past this a footfall reaches a player as a roll off the horizon rather than as the foot itself.
     */
    private static final double STEP_NEAR = 64.0;

    /** How much of its own drawn height a machine covers between two footfalls. */
    private static final double STRIDE = 0.2;

    /**
     * Ticks of shake a footfall and a stamp hand out.
     *
     * <p>Both sit inside the effect's own taper, which is what makes a step land as a jolt that
     * fades rather than as the flat shake an arrival gives: the strength a player feels is the
     * duration measured against that taper, so a short one is a weak one and costs no second knob.
     */
    private static final int STEP_SHAKE_TICKS = 14;
    private static final int STAMP_SHAKE_TICKS = 30;

    /**
     * The biggest a hittable slab is allowed to be, in blocks, on any axis.
     *
     * <p>The game finds an entity by the chunk section its position falls in, widened by four blocks,
     * and never by the box it carries. One slab over a walker's thirty blocks of legs answers shots
     * over the fifteen nearest its feet and lets the rest through; a slab fourteen wide answers from
     * one side and not the other. Seven is that reach spent on both sides of the position, with half
     * a block to spare for whatever section boundary the machine is standing across.
     */
    static final float SLAB_SIZE = 7.0f;

    /**
     * The widest a slab is allowed to be sideways, in blocks.
     *
     * <p>Height is what the section lookup punishes, since a column is cut into as many slabs as it
     * needs and each one sits at its own position. Width is not: one hood is one slab whatever the
     * machine, so capping it at the height limit would leave a titan's hull sticking out of its own
     * box. Sixteen is the reach a search has from a slab's own section plus the section itself,
     * which is as wide as a box can get and still answer from both of its edges.
     */
    static final float SPAN_SIZE = 16.0f;

    /**
     * The columns of slabs a machine wears, each one a fraction of its drawn height.
     *
     * <p>Measured off the model in game, with the silhouette read row by row off a screenshot rather
     * than guessed: a leg stands a fifth of the height out from the axis at the foot and around half
     * that under the hip, and it is barely a fortieth of the height thick along its whole length. The
     * middle of the machine below the hull is daylight, so nothing stands there and a shot between
     * the legs goes through.
     */
    private static final int LEG_COUNT = 3;
    private static final float LEG_REACH = 0.185f;
    private static final float LEG_TOP = 0.76f;
    private static final float LEG_WIDTH = 0.07f;

    /** How much of its foot reach a leg column still has once it arrives under the hull. */
    private static final float LEG_HIP = 0.55f;
    private static final float HULL_WIDTH = 0.20f;
    private static final float HOOD_WIDTH = 0.16f;

    /**
     * The hook on the end of an arm, as fractions of the drawn height.
     *
     * <p>Out to the side, forward of the hood, and a little under it: the arms reach past the snout,
     * which is why a beam that left the middle of the machine read as coming out of nothing.
     */
    private static final double ARM_SIDE = 0.083;
    private static final double ARM_AHEAD = 0.17;
    private static final double ARM_HIGH = 0.86;

    /**
     * Where the first leg column stands, in degrees off the way the machine faces.
     *
     * <p>Read off the model with the boxes drawn in game: the walker carries one leg behind it and
     * two spread in front, not one in front and two behind.
     */
    private static final float LEG_OFFSET = 180.0f;

    /** Well inside the ticket's own timeout, so the ground never lapses under a walking machine. */
    private static final int TICKET_PERIOD = 20;

    /** How often a wreck lets go of a lungful while it burns. */
    private static final int VENT_PERIOD = 45;

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
    private Float emergeFacing;
    private Vec3 partAnchor;
    private float partFacing;
    private float partTall;
    private double strideLeft;

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
     * blocks and collides at six. Growing the box to match would have the server walk the blocks
     * inside forty blocks of empty sky every tick, for every machine standing, and would wedge one
     * under any canopy tall enough to clear its hood. The box keeps colliding at the size that is
     * cheap; the slabs a shot lands on and the height the ray leaves from are cut from this.
     */
    public abstract float modelHeight();

    /** The same, at the size this machine actually stands. */
    public float drawnHeight() {
        return modelHeight() * getScale();
    }

    /**
     * It looks out of its hood, not out of the box it collides with.
     *
     * <p>The box is the legs, so the inherited eye sits four blocks off the ground on something forty
     * blocks tall. Everything that asks whether the machine can see a target would then be asking
     * whether its shins can, and a fence would be cover from a walker that steps over houses.
     */
    @Override
    public double getEyeY() {
        return getY() + drawnHeight() * 0.92;
    }

    /**
     * The hook at the end of one arm, which is where a beam leaves from.
     *
     * <p>The three numbers are the hook bone read off the model and turned into fractions of the
     * drawn height, so one arm's worth of offset stays on the arm whatever size the machine is
     * standing at. Taken off the body's facing rather than the head's, because the arms hang from the
     * hood and turn with the whole machine.
     *
     * @param left the machine's own left, which is the arm on a player's right when it faces them
     */
    public Vec3 muzzle(boolean left) {
        float facing = this.yBodyRot * Mth.DEG_TO_RAD;
        double sin = Mth.sin(facing);
        double cos = Mth.cos(facing);
        double tall = drawnHeight();
        double side = (left ? tall : -tall) * ARM_SIDE;
        double ahead = tall * ARM_AHEAD;
        return new Vec3(
                getX() + cos * side - sin * ahead,
                getY() + tall * ARM_HIGH,
                getZ() + sin * side + cos * ahead);
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

    /** Down for good. The seconds it then spends on the ground are scenery, not an agony. */
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

    /**
     * How far a client keeps drawing this once the server has told it about it.
     *
     * <p>The distance a mob stops being drawn at is sixty-four times the size of its collision box,
     * and this box is the legs. That put a walker's vanishing point near two hundred and seventy
     * blocks, so the silhouette that is the whole point of the thing popped out of the sky while the
     * ground it stood on was still being drawn, and worse with a mod that draws terrain for a
     * kilometre. Measured off the drawn height instead, which is what a player is actually looking
     * at, and still respecting the entity distance slider so anyone short of frames can turn it down.
     */
    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        double reach = drawnHeight() * 64.0 * getViewScale();
        return distance < reach * reach;
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
        this.goalSelector.addGoal(1, new StrideGoal(this));
        this.goalSelector.addGoal(5, new RoamGoal(this));
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
        private final MachineEntity machine;

        private HuntPlayerGoal(MachineEntity machine) {
            super(machine, Player.class, 10, false, false, null);
            this.machine = machine;
            this.targetConditions.ignoreLineOfSight();
        }

        /**
         * The vanilla search box reaches out as far as the follow range and up only four blocks,
         * which suits something that walks the ground and looks at what is in front of it. A machine
         * fires from a hood ninety-six blocks in the air, so a player on a hilltop or on a roof was
         * standing out of a box the machine was towering over.
         */
        @Override
        protected AABB getTargetSearchArea(double range) {
            double up = Math.max(4.0, this.machine.drawnHeight());
            return this.machine.getBoundingBox().inflate(range, up, range);
        }
    }

    /**
     * Walking at a target without asking for a route to it.
     *
     * <p>The vanilla melee goal asks the navigator for a path, and a path costs the number of blocks
     * the walker fills at every node it considers. A machine fills a few hundred, and it hunts over a
     * hundred and twenty-eight blocks, so a single one deciding how to reach a player was measured at
     * half a second of server time: eight of them held the tick at ninety-five milliseconds against a
     * budget of fifty, and the reading swung between eight and a thousand depending on who happened to
     * be recomputing. Handing the move control a position costs nothing and is what this thing does
     * anyway. It is taller than the trees and it walks through the wall rather than around the house.
     */
    private static final class StrideGoal extends Goal {
        /** Between two stamps, so a machine standing over someone does not hit them every tick. */
        private static final int STRIKE_PERIOD = 20;

        private final MachineEntity machine;
        private int strikeCooldown;

        private StrideGoal(MachineEntity machine) {
            this.machine = machine;
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            LivingEntity target = this.machine.getTarget();
            return target != null && target.isAlive() && !this.machine.emerging();
        }

        @Override
        public boolean canContinueToUse() {
            return canUse();
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void stop() {
            this.machine.getMoveControl().setWantedPosition(this.machine.getX(),
                    this.machine.getY(), this.machine.getZ(), 0.0);
        }

        @Override
        public void tick() {
            LivingEntity target = this.machine.getTarget();
            if (target == null) {
                return;
            }
            this.machine.getLookControl().setLookAt(target, 30.0f, 30.0f);

            if (this.strikeCooldown > 0) {
                this.strikeCooldown--;
            }
            if (this.machine.isWithinMeleeAttackRange(target)) {
                if (this.strikeCooldown <= 0 && this.machine.level() instanceof ServerLevel server) {
                    this.strikeCooldown = STRIKE_PERIOD;
                    this.machine.doHurtTarget(server, target);
                }
                return;
            }
            this.machine.getMoveControl().setWantedPosition(target.getX(), target.getY(),
                    target.getZ(), 1.0);
        }
    }

    /**
     * What it does with nobody to walk at: it keeps walking, in a direction it picks now and then.
     *
     * <p>Same reason as the goal above. The stroll goal vanilla ships asks for a short path every few
     * seconds, which is cheap for a chicken and was worth ten milliseconds a tick for six machines
     * standing in an empty field.
     */
    private static final class RoamGoal extends Goal {
        private static final int HOLD_TICKS = 120;
        private static final double REACH = 16.0;

        private final MachineEntity machine;
        private int held;

        private RoamGoal(MachineEntity machine) {
            this.machine = machine;
            setFlags(EnumSet.of(Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            return this.machine.getTarget() == null && !this.machine.emerging();
        }

        @Override
        public boolean canContinueToUse() {
            return canUse();
        }

        @Override
        public void tick() {
            if (--this.held > 0) {
                return;
            }
            this.held = HOLD_TICKS + this.machine.getRandom().nextInt(HOLD_TICKS);
            double angle = this.machine.getRandom().nextDouble() * Math.PI * 2.0;
            this.machine.getMoveControl().setWantedPosition(
                    this.machine.getX() + Math.cos(angle) * REACH,
                    this.machine.getY(),
                    this.machine.getZ() + Math.sin(angle) * REACH,
                    0.8);
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
        tickTicket(server);

        if (emerging()) {
            tickEmerge(server);
            return;
        }
        tickEngine(server);
        tickStride(server);
        tickWeapon(server);
    }

    /**
     * Keeps the ground under the machine loaded so it goes on walking with nobody near it.
     *
     * <p>A machine is a thing you watch cross a valley, and the vanilla rule is that an entity stops
     * existing the moment its chunk leaves the player's view distance: one that set off towards a
     * village a hundred blocks away used to freeze the second the player turned round, and be exactly
     * where it was left an hour later. With a mod that draws terrain far past what the server sends,
     * the same machine is now something the player can see standing still.
     *
     * <p>The ticket carries its own timeout, so nothing has to clean up after a machine that dies or
     * despawns: it stops renewing and the ground goes back to being unloaded a couple of seconds
     * later. Renewed on a period well inside that timeout rather than every tick, since the cost is
     * in the renewal and not in the holding.
     *
     * <p>One level under entity ticking rather than at it, which costs the same single ticket and
     * gets the eight chunks around it as well, since a level spreads outwards one rung per chunk. A
     * ticket on the one chunk the machine stands in would be a trap: the tick that walks it over the
     * border lands it somewhere nothing ticks, so it never reaches the tick that would have claimed
     * the ground it just stepped on, and it stops for good on a chunk line.
     */
    private void tickTicket(ServerLevel server) {
        if (this.tickCount % TICKET_PERIOD != 0) {
            return;
        }
        int around = ChunkLevel.byStatus(FullChunkStatus.ENTITY_TICKING) - 1;
        server.getChunkSource().addTicket(new Ticket(TicketType.ENDER_PEARL, around), chunkPosition());
    }

    // ---- the ground under it ----

    /**
     * A footfall every time the machine has covered its own stride.
     *
     * <p>Counted off the ground covered rather than read from the walk animation, because the clip
     * runs on the client and the sound, the shake and the dust all have to be decided where the
     * machine is. It also keeps the cadence honest when a build walks at three quarters of the speed:
     * fewer steps per second, same distance between two of them.
     */
    private void tickStride(ServerLevel server) {
        double moved = Math.hypot(this.getX() - this.xo, this.getZ() - this.zo);
        if (moved < 1.0e-3 || !this.onGround()) {
            return;
        }
        this.strideLeft -= moved;
        if (this.strideLeft > 0.0) {
            return;
        }
        this.strideLeft = drawnHeight() * STRIDE;
        footfall(server);
    }

    /**
     * One leg landing: heard, felt, and seen in the dirt it throws.
     *
     * <p>A machine near enough to make out gets the foot itself; with nobody in that range the same
     * step goes out as the far version, which has lost its top end and gained a tail. One sound per
     * step either way, since two layers over the same footfall arrive as one muddy thump to whoever
     * stands between the two ranges.
     */
    private void footfall(ServerLevel server) {
        float scale = getScale();
        Player nearest = server.getNearestPlayer(this, STEP_NEAR);
        if (nearest == null) {
            server.playSound(null, this.getX(), this.getY(), this.getZ(),
                    TripodDawnSounds.MACHINE_STEP_FAR, getSoundSource(), 9.0f, 1.0f / scale);
            return;
        }

        // Pitch off the size and not off the build: the titan is the walker's own model grown by its
        // scale attribute, so anything read from the scale covers every machine there is.
        server.playSound(null, this.getX(), this.getY(), this.getZ(),
                TripodDawnSounds.MACHINE_STEP, getSoundSource(), 5.0f,
                (1.0f / scale) * (0.94f + this.random.nextFloat() * 0.12f));
        TripodDawnParticles.send(server, TripodDawnParticles.DIRT_CLOUD,
                this.getX(), this.getY() + 0.1, this.getZ(), 3,
                this.getBbWidth() * 0.6, 0.15, this.getBbWidth() * 0.6, 0.01);
        shakeAround(server, drawnHeight() * 0.75, STEP_SHAKE_TICKS);
    }

    /**
     * Hands the shake to everyone standing close enough for the ground to move under them.
     *
     * <p>Reach comes from how tall the machine is drawn, so a titan is felt from further out than a
     * scout without either of them carrying a number of its own.
     */
    private void shakeAround(ServerLevel server, double reach, int ticks) {
        double square = reach * reach;
        for (ServerPlayer player : server.players()) {
            if (player.distanceToSqr(this) <= square) {
                MachineArrival.shake(player, ticks);
            }
        }
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
            buildParts(server);
            return;
        }
        // A machine standing still still animates, but the columns do not follow the animation, so
        // there is nothing for them to do until the body itself moves or turns. Moving a slab writes
        // it into a new chunk section, and forty of those per machine per tick is a bill a row of
        // them pays for nothing.
        if (position().equals(this.partAnchor) && this.yBodyRot == this.partFacing
                && drawnHeight() == this.partTall) {
            return;
        }
        this.partAnchor = position();
        this.partFacing = this.yBodyRot;
        this.partTall = drawnHeight();
        for (MachinePart part : this.parts) {
            part.follow();
        }
    }

    /**
     * Stands the columns of slabs up and puts them in the world.
     *
     * <p>Everything is a fraction of the machine's drawn height rather than a number of blocks,
     * because the same layout has to fit a scout and a titan twice its size.
     */
    private void buildParts(ServerLevel server) {
        List<MachinePart> built = new ArrayList<>();
        for (int leg = 0; leg < LEG_COUNT; leg++) {
            column(server, built, MachinePart.Zone.LEGS, 0.0f, LEG_TOP,
                    LEG_REACH, LEG_OFFSET + 360.0f * leg / LEG_COUNT, LEG_WIDTH);
        }
        column(server, built, MachinePart.Zone.HULL, MachinePart.Zone.HULL.bottom(),
                MachinePart.Zone.HULL.top(), 0.0f, 0.0f, HULL_WIDTH);
        column(server, built, MachinePart.Zone.HOOD, MachinePart.Zone.HOOD.bottom(),
                MachinePart.Zone.HOOD.top(), 0.0f, 0.0f, HOOD_WIDTH);
        this.parts = built.toArray(new MachinePart[0]);
    }

    /** Cuts one column into slabs short enough to be found where they stand, and spawns them. */
    private void column(ServerLevel server, List<MachinePart> built, MachinePart.Zone zone,
                        float bottom, float top, float reach, float angle, float width) {
        float span = top - bottom;
        int slabs = Math.max(1, Mth.ceil(drawnHeight() * span / SLAB_SIZE));
        for (int i = 0; i < slabs; i++) {
            float low = bottom + span * i / slabs;
            float high = bottom + span * (i + 1) / slabs;
            MachinePart part = new MachinePart(this, zone, low, high,
                    reach * lean((low + high) * 0.5f), angle, width);
            server.addFreshEntity(part);
            built.add(part);
        }
    }

    /**
     * How much of its foot reach a leg column keeps at this height.
     *
     * <p>A leg slopes inwards the whole way up rather than standing straight and bending at the last
     * moment, so the columns follow one line from the foot to the hip.
     */
    private static float lean(float height) {
        return Mth.lerp(Mth.clamp(height / LEG_TOP, 0.0f, 1.0f), 1.0f, LEG_HIP);
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
        holdFacing();
        MachineArrival.lightning(server, this, t);

        // The rumble is laid end to end for the whole rise, so the shake a player is already under
        // has something to be the sound of. Retriggered on the clip's own length rather than looped,
        // since Minecraft gives a positional sound no loop of its own.
        if (t % MachineArrival.RUMBLE_TICKS == 0) {
            server.playSound(null, this.getX(), this.getY(), this.getZ(),
                    TripodDawnSounds.MACHINE_RUMBLE, getSoundSource(), 8.0f, 1.0f);
        }

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
     * Nails head, body and eyes to the way the machine came out of the ground.
     *
     * <p>The rise is a clip of a machine climbing straight up, so a body turning under it reads as the
     * whole thing swivelling in its own hole. The look control keeps running whatever the phase is,
     * and clearing the target is not enough to stop it: an idle machine still has a goal that turns
     * its head. Written after the AI has had its tick rather than before, since the last value put
     * there is the one the client is told about.
     */
    private void holdFacing() {
        if (this.emergeFacing == null) {
            this.emergeFacing = getYRot();
        }
        float facing = this.emergeFacing;
        setYRot(facing);
        this.yRotO = facing;
        setYHeadRot(facing);
        this.yHeadRotO = facing;
        setYBodyRot(facing);
        this.yBodyRotO = facing;
        setXRot(0.0f);
        this.xRotO = 0.0f;
        getLookControl().setLookAt(
                getX() - Mth.sin(facing * Mth.DEG_TO_RAD) * 16.0,
                getEyeY(),
                getZ() + Mth.cos(facing * Mth.DEG_TO_RAD) * 16.0);
    }

    /**
     * The engine is played from the machine rather than pushed to every player each tick. The source
     * mod broadcast a packet per living machine per tick to do the same job.
     */
    private void tickEngine(ServerLevel server) {
        if (--this.engineTicks > 0) {
            return;
        }
        this.engineTicks = 100 + this.random.nextInt(60);

        // The far loop had been registered and never played. Which one goes out is the same question
        // the footfall asks, and it gets the same answer: the near sound for a machine somebody can
        // make out, the worn one for a shape on the horizon.
        boolean close = server.getNearestPlayer(this, STEP_NEAR) != null;
        playSound(close ? TripodDawnSounds.MACHINE_ENGINE : TripodDawnSounds.MACHINE_ENGINE_FAR,
                close ? 6.0f : 10.0f,
                (1.0f / getScale()) * (0.9f + this.random.nextFloat() * 0.2f));
    }

    /**
     * The foot coming down on whatever was standing under it.
     *
     * <p>The stamp had been the one thing a machine did with no sound on it at all: twelve blocks in
     * and the scene went quiet, which read as the machine losing interest rather than as it changing
     * weapon.
     */
    @Override
    public boolean doHurtTarget(ServerLevel server, Entity target) {
        if (!super.doHurtTarget(server, target)) {
            return false;
        }
        server.playSound(null, this.getX(), this.getY(), this.getZ(),
                TripodDawnSounds.MACHINE_STAMP, getSoundSource(), 7.0f, 1.0f / getScale());
        TripodDawnParticles.send(server, TripodDawnParticles.DIRT_CLOUD,
                target.getX(), target.getY() + 0.1, target.getZ(), 8, 1.2, 0.3, 1.2, 0.03);
        shakeAround(server, drawnHeight(), STAMP_SHAKE_TICKS);
        return true;
    }

    /**
     * Whether the beam would reach the target, traced from the arm it leaves rather than from the
     * feet, and with no ceiling on how far it may look.
     *
     * <p>The vanilla sight test refuses anything past a hundred and twenty-eight blocks outright,
     * which is a number chosen for mobs whose eyes are at head height. A machine's arms stand ninety
     * blocks up, so the line from one to someone a hundred and forty blocks away is already a hundred
     * and sixty long and comes back blind: the big one could hunt a player it was never allowed to
     * shoot at.
     */
    private boolean clearShot(LivingEntity target) {
        ClipContext trace = new ClipContext(muzzle(true), target.getEyePosition(),
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this);
        return level().clip(trace).getType() == HitResult.Type.MISS;
    }

    /** Which of the shots this machine gathers at that distance, squared. */
    protected HeatRayProjectile.Mode pickShot(double reachSqr) {
        return reachSqr > HeatRayProjectile.CHARGE_FROM * HeatRayProjectile.CHARGE_FROM
                ? HeatRayProjectile.Mode.CHARGED
                : HeatRayProjectile.Mode.QUICK;
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
        if (reach > weaponRange() * weaponRange() || reach < WEAPON_MIN_RANGE * WEAPON_MIN_RANGE
                || !clearShot(target)) {
            this.aimTicks = 0;
            return;
        }

        // Distance picks the shot, and it is picked once at the start of the wind-up rather than
        // read again every tick: a player walking across the threshold mid-aim would otherwise turn
        // a charged shot into a snap one halfway through, and the warning they had been reading
        // would have been a lie.
        if (++this.aimTicks == 1) {
            this.mode = pickShot(reach);
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
        if (!(this.level() instanceof ServerLevel server)) {
            return;
        }
        if (this.deathTime < WRECK_TICKS) {
            vent(server);
            return;
        }
        scuttle(server);
        this.remove(RemovalReason.KILLED);
    }

    /**
     * What a wreck does with the few seconds it has.
     *
     * <p>The gas sound had been registered and never played, and a machine lying in a field in total
     * silence reads as a prop. It lets go of a lungful instead, three times over, which is also the
     * warning that the thing has not finished.
     */
    private void vent(ServerLevel server) {
        if (this.deathTime % VENT_PERIOD != 0) {
            return;
        }
        server.playSound(null, this.getX(), this.getY(), this.getZ(),
                TripodDawnSounds.MACHINE_GAS, getSoundSource(), 3.0f,
                0.75f + this.random.nextFloat() * 0.3f);
        TripodDawnParticles.send(server, ParticleTypes.LARGE_SMOKE,
                this.getX(), this.getY() + 1.0, this.getZ(), 6, 1.6, 0.8, 1.6, 0.02);
    }

    /**
     * What a wreck does instead of blinking out.
     *
     * <p>A machine left lying is scenery, and scenery that vanishes between two glances reads as the
     * game forgetting it. It goes up instead, a few seconds after it lands, while whoever killed it is
     * still standing there. Bound to the vanilla mob griefing rule like every other blast this mod
     * sets off, so a server that turned block damage off has already said so.
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
