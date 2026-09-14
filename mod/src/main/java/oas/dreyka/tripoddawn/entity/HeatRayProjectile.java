package oas.dreyka.tripoddawn.entity;

import oas.dreyka.tripoddawn.particle.TripodDawnParticles;
import oas.dreyka.tripoddawn.sound.TripodDawnSounds;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.ExplosionDamageCalculator;
import net.minecraft.world.level.Level;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * The heat ray.
 *
 * <p>It keeps going through what it hits instead of stopping at the first body, up to ten of them,
 * which is what makes a machine dangerous to a group rather than to one player. The number and the
 * damage are the source mod's.
 */
public class HeatRayProjectile extends Projectile {

    /**
     * The two ways a machine shoots.
     *
     * <p>One weapon with one behaviour meant the same second and a quarter of warning whether the
     * player was at arm's length or at the far end of a field, and the same certain death at the end
     * of it. Distance decides now. Up close it snaps, which is fast enough to be frightening and weak
     * enough to be survived in armour; from far off it winds all the way up, which is death, and the
     * two seconds of arms glowing are the whole warning. Closing the distance is the answer to the
     * one that kills, and that is the fight this mod did not have before.
     */
    public enum Mode {
        // Two beams, one off each arm, aimed from the hook they leave rather than from a shared
        // point, so they close on the target from either side. Half the damage each: what a player
        // takes off one salvo is what it was when a machine fired once.
        QUICK(8, 13.0f, 4.0f, 2.5, 1.5f, 25, 2, 0.0f, 3.0f),
        CHARGED(40, 250.0f, 20.0f, 4.0, 3.0f, 100, 2, 0.0f, 3.0f),

        /**
         * The titan's, and nothing else fires it.
         *
         * <p>Five beams instead of two, fanned wide enough that at a hundred blocks they arrive as a
         * line of fire thirty blocks across rather than as a shot to dodge. Three and a half seconds
         * of arms gathering pays for it, which is long enough to get out of the way of and long
         * enough to watch, and eight and a half between two of them.
         */
        SIEGE(70, 900.0f, 70.0f, 7.0, 4.0f, 170, 5, 4.5f, 5.0f);

        private final int aim;
        private final float direct;
        private final float splash;
        private final double radius;
        private final float blast;
        private final int cooldown;
        private final int shots;
        private final float fan;
        private final float speed;

        Mode(int aim, float direct, float splash, double radius, float blast, int cooldown,
             int shots, float fan, float speed) {
            this.aim = aim;
            this.direct = direct;
            this.splash = splash;
            this.radius = radius;
            this.blast = blast;
            this.cooldown = cooldown;
            this.shots = shots;
            this.fan = fan;
            this.speed = speed;
        }

        /** Ticks of wind-up, which is the warning a player gets. */
        public int aim() {
            return this.aim;
        }

        public int cooldown() {
            return this.cooldown;
        }
    }

    /** Beyond this a machine has the time to wind all the way up, and takes it. */
    public static final double CHARGE_FROM = 36.0;

    /** The drawn height every blast is measured against: the walker, at its own scale. */
    private static final float REFERENCE_HEIGHT = 40.0f;

    /** Left arm then right, for everything that has to happen at both hooks. */
    private static final boolean[] BOTH_ARMS = {true, false};

    private static final int MAX_PIERCE = 10;

    /**
     * Ticks of flight, which at three blocks each is ninety blocks. Well past the sixty-four a
     * machine shoots at, and short enough that the ray always dies inside the area the server is
     * still ticking: one that flies out of it stops ageing and stays in the world for good.
     */
    private static final int MAX_AGE = 34;

    /**
     * How the beam is drawn.
     *
     * <p>It used to be two puffs dropped once per tick, and a tick of flight is three blocks, so what
     * a player saw was a dotted line with two metres of night between the dots. The body is now laid
     * down along the step rather than at the end of it, in two layers: a wide orange sleeve at
     * {@link #BODY_SAMPLES} points, and a thin white core at the head where the heat actually is.
     */
    private static final int BODY_SAMPLES = 3;
    private static final int BODY_PER_SAMPLE = 3;
    private static final double BODY_SPREAD = 0.22;
    private static final int CORE_COUNT = 4;
    private static final double CORE_SPREAD = 0.08;

    /** How often the arms flare through the wind-up, so the shot is seen before it lands. */
    private static final int CHARGE_PERIOD = 4;

    private int pierced;
    private Mode mode = Mode.CHARGED;
    private float power = 1.0f;

    public HeatRayProjectile(EntityType<? extends HeatRayProjectile> type, Level level) {
        super(type, level);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
    }

    /**
     * The arms lighting up through the wind-up.
     *
     * <p>A beam that arrives with no warning is a death a player cannot read afterwards. The glow
     * tightens as the shot gets closer: wide and dim at the start, a hard point by the end.
     */
    public static void charge(ServerLevel level, MachineEntity machine, Mode mode, int aimTicks) {
        if (aimTicks % CHARGE_PERIOD != 0) {
            return;
        }
        // A snap shot gathers nothing worth seeing, so it gets a flicker at the hooks and the wide
        // closing glow is kept for the one that kills: the two shots have to be told apart in the
        // second a player has to decide whether to run or to close.
        double closing = 1.0 - Math.min(1.0, (double) aimTicks / mode.aim);
        double spread = switch (mode) {
            case QUICK -> 0.3;
            case CHARGED -> 0.2 + closing * 1.6;
            // Gathered over arms four times the span, and it has to be legible from the far side
            // of the field it is about to burn, so it starts wider and it draws in further.
            case SIEGE -> 0.4 + closing * 5.0;
        };
        int count = mode == Mode.SIEGE ? 10 : 3;
        for (boolean left : BOTH_ARMS) {
            Vec3 hook = machine.muzzle(left);
            TripodDawnParticles.send(level, TripodDawnParticles.HEAT_RAY,
                    hook.x, hook.y, hook.z, count, spread, spread, spread, 0.0);
            TripodDawnParticles.send(level, TripodDawnParticles.HEAT_RAY_BRIGHT,
                    hook.x, hook.y, hook.z, count - 1, 0.25, 0.25, 0.25, 0.0);
        }
    }

    /**
     * Leaves the hooks on the ends of the arms, one shot per arm and back again.
     *
     * <p>The arms are what a machine points at a target, so a beam out of the middle of the hull read
     * as coming from nowhere. Each shot is aimed from the hook it leaves, not from a shared centre,
     * which is what makes two of them converge on a target instead of running parallel.
     */
    public static void fire(ServerLevel level, MachineEntity machine, LivingEntity target, Mode mode) {
        Vec3 at = target.getPosition(1.0f).add(0.0, target.getBbHeight() * 0.5, 0.0);
        float power = power(machine);

        // Fanned around the upright rather than around the line of fire, so a volley lands as a row
        // along the ground and not as a ring around the target. The middle beam keeps the aim.
        for (int i = 0; i < mode.shots; i++) {
            Vec3 hook = machine.muzzle(i % 2 == 0);
            float turn = (i - (mode.shots - 1) * 0.5f) * mode.fan * Mth.DEG_TO_RAD;
            Vec3 spread = at.subtract(hook).yRot(turn);
            HeatRayProjectile ray = new HeatRayProjectile(TripodDawnEntities.HEAT_RAY, level);
            ray.mode = mode;
            ray.power = power;
            ray.setPos(hook.x, hook.y, hook.z);
            ray.setOwner(machine);
            ray.shoot(spread.x, spread.y, spread.z, mode.speed, 0.0f);
            level.addFreshEntity(ray);
            muzzleFlash(level, hook, ray.getDeltaMovement().normalize());
        }

        machine.playSound(TripodDawnSounds.HEAT_RAY, mode == Mode.SIEGE ? 16.0f : 10.0f,
                switch (mode) {
                    case QUICK -> 1.35f;
                    case CHARGED -> 0.9f;
                    case SIEGE -> 0.55f;
                });
    }

    /**
     * How hard the machine that fired hits the ground, against the walker the mod is built around.
     *
     * <p>A scout is a third of a titan, so its shot should not leave the same crater. The number is
     * the machine's own drawn height, which already carries its build and its scale attribute, so a
     * machine added later is covered without a value of its own.
     */
    private static float power(MachineEntity machine) {
        return machine.drawnHeight() / REFERENCE_HEIGHT;
    }

    /** The bloom at the barrel, thrown forward rather than sat on the hook. */
    private static void muzzleFlash(ServerLevel level, Vec3 hook, Vec3 forward) {
        double x = hook.x + forward.x * 2.0;
        double y = hook.y + forward.y * 2.0;
        double z = hook.z + forward.z * 2.0;
        TripodDawnParticles.send(level, TripodDawnParticles.BLAST, x, y, z, 3, 0.5, 0.5, 0.5, 0.0);
        TripodDawnParticles.send(level, TripodDawnParticles.HEAT_RAY_BRIGHT, x, y, z, 8, 0.7, 0.7, 0.7, 0.04);
        TripodDawnParticles.send(level, TripodDawnParticles.HEAT_RAY, x, y, z, 10, 1.1, 1.1, 1.1, 0.06);
    }

    @Override
    public void tick() {
        super.tick();
        if (this.tickCount > MAX_AGE) {
            this.discard();
            return;
        }

        // Projectile does not move itself and does not look for what it runs into: both belong to
        // the subclass. Asked before the step rather than after, so the ray stops at the face it
        // would otherwise already be behind.
        HitResult hit = ProjectileUtil.getHitResultOnMoveVector(this, this::canHitEntity);
        if (hit.getType() != HitResult.Type.MISS) {
            hitTargetOrDeflectSelf(hit);
        }
        if (this.isRemoved()) {
            return;
        }

        Vec3 from = this.position();
        Vec3 motion = this.getDeltaMovement();
        this.setPos(from.x + motion.x, from.y + motion.y, from.z + motion.z);

        if (this.level() instanceof ServerLevel server) {
            trail(server, from, motion);
        }
    }

    /** The beam, laid down along the three blocks the ray just crossed rather than at its head. */
    private void trail(ServerLevel server, Vec3 from, Vec3 motion) {
        for (int i = 0; i < BODY_SAMPLES; i++) {
            double along = (i + 0.5) / BODY_SAMPLES;
            TripodDawnParticles.send(server, TripodDawnParticles.HEAT_RAY,
                    from.x + motion.x * along, from.y + motion.y * along, from.z + motion.z * along,
                    BODY_PER_SAMPLE, BODY_SPREAD, BODY_SPREAD, BODY_SPREAD, 0.0);
        }
        TripodDawnParticles.send(server, TripodDawnParticles.HEAT_RAY_BRIGHT,
                this.getX(), this.getY(), this.getZ(),
                CORE_COUNT, CORE_SPREAD, CORE_SPREAD, CORE_SPREAD, 0.0);
    }

    /** The invasion never shoots its own, for the same reason it never catches its own fire. */
    @Override
    protected boolean canHitEntity(Entity target) {
        return !MachineEntity.invader(target) && super.canHitEntity(target);
    }

    @Override
    protected void onHitEntity(EntityHitResult hit) {
        super.onHitEntity(hit);
        if (!(this.level() instanceof ServerLevel server)) {
            return;
        }
        Entity victim = hit.getEntity();
        victim.hurtServer(server, this.damageSources().onFire(), this.mode.direct);
        victim.igniteForSeconds(this.mode == Mode.QUICK ? 3.0f : 8.0f);

        // The beam goes through, so the hit has to read on the body rather than on the beam ending.
        TripodDawnParticles.send(server, TripodDawnParticles.BLAST,
                victim.getX(), victim.getY() + victim.getBbHeight() * 0.5, victim.getZ(),
                2, 0.3, 0.3, 0.3, 0.0);
        TripodDawnParticles.send(server, TripodDawnParticles.HEAT_RAY_BRIGHT,
                victim.getX(), victim.getY() + victim.getBbHeight() * 0.5, victim.getZ(),
                10, 0.4, 0.5, 0.4, 0.14);

        if (++this.pierced >= MAX_PIERCE) {
            this.discard();
        }
    }

    @Override
    protected void onHitBlock(BlockHitResult hit) {
        super.onHitBlock(hit);
        if (!(this.level() instanceof ServerLevel server)) {
            return;
        }
        splash(server);
        this.discard();
    }

    /**
     * The blast takes the ground and whoever is standing on it, never the machines that called it
     * down. Blocks are left to the default rules, so a server that turned griefing off keeps it off.
     */
    private static final ExplosionDamageCalculator SPARE_INVADERS = new ExplosionDamageCalculator() {
        @Override
        public boolean shouldDamageEntity(Explosion explosion, Entity entity) {
            return !MachineEntity.invader(entity);
        }
    };

    /**
     * The hole and the fire. Bound to the vanilla mob griefing rule rather than to a rule of its
     * own: a server that has turned block damage off has said so once, for every mob.
     */
    private void splash(ServerLevel server) {
        double reach = this.mode.radius * this.power;
        impact(server, reach);

        for (LivingEntity nearby : server.getEntitiesOfClass(LivingEntity.class,
                new AABB(this.position(), this.position()).inflate(reach))) {
            // Machines never catch their own fire, otherwise two of them in one street kill each
            // other before they reach anything a player built.
            if (nearby == this.getOwner() || MachineEntity.invader(nearby)) {
                continue;
            }
            nearby.hurtServer(server, this.damageSources().onFire(), this.mode.splash);
        }

        server.explode(this.getOwner(), null, SPARE_INVADERS, this.getX(), this.getY(), this.getZ(),
                this.mode.blast * this.power, true, Level.ExplosionInteraction.MOB);
        server.gameEvent(GameEvent.EXPLODE, this.position(), GameEvent.Context.of(this));
    }

    /**
     * The ground hit, in three layers so that it reads at sixty blocks as well as at ten: the white
     * flash, the orange ball thrown back along the beam, and the smoke the vanilla explosion leaves.
     *
     * <p>The cloud is spread over the crater the shot is about to open rather than over a fixed
     * ball, since a titan's hit that looked like a scout's read as the big machine missing. Counts
     * rise with the square root of that reach: a cloud twice as wide needs more than twice the
     * grains to stay opaque, and rather fewer than the four times filling it evenly would cost.
     */
    private void impact(ServerLevel server, double reach) {
        double x = this.getX();
        double y = this.getY();
        double z = this.getZ();
        Vec3 back = this.getDeltaMovement().normalize().scale(-1.0);
        double spread = reach * 0.55;
        float thick = Mth.sqrt((float) (reach / this.mode.radius));

        TripodDawnParticles.send(server, TripodDawnParticles.HEAT_RAY_BRIGHT, x, y, z,
                grains(24, thick), spread * 0.4, spread * 0.4, spread * 0.4, 0.3);
        TripodDawnParticles.send(server, TripodDawnParticles.BLAST, x, y, z,
                grains(10, thick), spread, spread, spread, 0.06);
        TripodDawnParticles.send(server, TripodDawnParticles.HEAT_RAY,
                x + back.x, y + back.y, z + back.z,
                grains(20, thick), spread * 1.1, spread * 1.1, spread * 1.1, 0.18);
        TripodDawnParticles.send(server, ParticleTypes.LAVA, x, y, z,
                grains(8, thick), spread * 0.45, spread * 0.45, spread * 0.45, 0.0);
        TripodDawnParticles.send(server, ParticleTypes.LARGE_SMOKE, x, y + 0.5, z,
                grains(12, thick), spread * 0.7, spread * 0.6, spread * 0.7, 0.04);
    }

    private static int grains(int base, float thick) {
        return Math.round(base * thick);
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putInt("Pierced", this.pierced);
        output.putString("Mode", this.mode.name());
        output.putFloat("Power", this.power);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        this.pierced = input.getIntOr("Pierced", 0);
        this.mode = Mode.valueOf(input.getStringOr("Mode", Mode.CHARGED.name()));
        this.power = input.getFloatOr("Power", 1.0f);
    }

    @Override
    protected double getDefaultGravity() {
        return 0.0;
    }
}
