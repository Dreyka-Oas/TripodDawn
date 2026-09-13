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
     * two seconds of hood glowing are the whole warning. Closing the distance is the answer to the
     * one that kills, and that is the fight this mod did not have before.
     */
    public enum Mode {
        QUICK(8, 25.0f, 8.0f, 2.5, 1.5f, 25),
        CHARGED(40, 500.0f, 40.0f, 4.0, 3.0f, 100);

        private final int aim;
        private final float direct;
        private final float splash;
        private final double radius;
        private final float blast;
        private final int cooldown;

        Mode(int aim, float direct, float splash, double radius, float blast, int cooldown) {
            this.aim = aim;
            this.direct = direct;
            this.splash = splash;
            this.radius = radius;
            this.blast = blast;
            this.cooldown = cooldown;
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

    private static final int MAX_PIERCE = 10;

    /**
     * Ticks of flight, which at three blocks each is ninety blocks. Well past the sixty-four a
     * machine shoots at, and short enough that the ray always dies inside the area the server is
     * still ticking: one that flies out of it stops ageing and stays in the world for good.
     */
    private static final int MAX_AGE = 30;

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

    /** Where the hood glows while the machine is winding up, so the shot is seen before it lands. */
    private static final int CHARGE_PERIOD = 4;

    private int pierced;
    private Mode mode = Mode.CHARGED;

    public HeatRayProjectile(EntityType<? extends HeatRayProjectile> type, Level level) {
        super(type, level);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
    }

    /** The height the beam leaves from, which is the hood and not the feet forty blocks below. */
    public static double muzzleY(MachineEntity machine) {
        return machine.getY() + machine.drawnHeight() * 0.92;
    }

    /**
     * The hood lighting up through the wind-up.
     *
     * <p>A beam that arrives with no warning is a death a player cannot read afterwards. The glow
     * tightens as the shot gets closer: wide and dim at the start, a hard point by the end.
     */
    public static void charge(ServerLevel level, MachineEntity machine, Mode mode, int aimTicks) {
        if (aimTicks % CHARGE_PERIOD != 0) {
            return;
        }
        // A snap shot gathers nothing worth seeing, so it gets a flicker at the hood and the wide
        // closing glow is kept for the one that kills: the two shots have to be told apart in the
        // second a player has to decide whether to run or to close.
        double closing = 1.0 - Math.min(1.0, (double) aimTicks / mode.aim);
        double spread = mode == Mode.QUICK ? 0.3 : 0.2 + closing * 1.6;
        TripodDawnParticles.send(level, TripodDawnParticles.HEAT_RAY,
                machine.getX(), muzzleY(machine), machine.getZ(), 3, spread, spread, spread, 0.0);
        TripodDawnParticles.send(level, TripodDawnParticles.HEAT_RAY_BRIGHT,
                machine.getX(), muzzleY(machine), machine.getZ(), 2, 0.25, 0.25, 0.25, 0.0);
    }

    /** Aims from the machine's hood rather than from its feet, twenty-four blocks lower. */
    public static void fire(ServerLevel level, MachineEntity machine, LivingEntity target, Mode mode) {
        HeatRayProjectile ray = new HeatRayProjectile(TripodDawnEntities.HEAT_RAY, level);
        double y = muzzleY(machine);
        ray.mode = mode;
        ray.setPos(machine.getX(), y, machine.getZ());
        ray.setOwner(machine);

        Vec3 aim = new Vec3(
                target.getX() - machine.getX(),
                target.getY() + target.getBbHeight() * 0.5 - y,
                target.getZ() - machine.getZ());
        ray.shoot(aim.x, aim.y, aim.z, 3.0f, 0.0f);

        level.addFreshEntity(ray);
        muzzleFlash(level, machine, ray.getDeltaMovement().normalize());
        machine.playSound(TripodDawnSounds.HEAT_RAY, 10.0f, mode == Mode.QUICK ? 1.35f : 0.9f);
    }

    /** The bloom at the barrel, thrown forward rather than sat on the hood. */
    private static void muzzleFlash(ServerLevel level, MachineEntity machine, Vec3 forward) {
        double x = machine.getX() + forward.x * 2.0;
        double y = muzzleY(machine) + forward.y * 2.0;
        double z = machine.getZ() + forward.z * 2.0;
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
     * The hole and the fire. Bound to the vanilla mob griefing rule rather than to a rule of its
     * own: a server that has turned block damage off has said so once, for every mob.
     */
    private void splash(ServerLevel server) {
        impact(server);

        for (LivingEntity nearby : server.getEntitiesOfClass(LivingEntity.class,
                new AABB(this.position(), this.position()).inflate(this.mode.radius))) {
            // Machines never catch their own fire, otherwise two of them in one street kill each
            // other before they reach anything a player built.
            if (nearby == this.getOwner() || MachineEntity.invader(nearby)) {
                continue;
            }
            nearby.hurtServer(server, this.damageSources().onFire(), this.mode.splash);
        }

        server.explode(this.getOwner(), this.getX(), this.getY(), this.getZ(), this.mode.blast,
                true, Level.ExplosionInteraction.MOB);
        server.gameEvent(GameEvent.EXPLODE, this.position(), GameEvent.Context.of(this));
    }

    /**
     * The ground hit, in three layers so that it reads at sixty blocks as well as at ten: the white
     * flash, the orange ball thrown back along the beam, and the smoke the vanilla explosion leaves.
     */
    private void impact(ServerLevel server) {
        double x = this.getX();
        double y = this.getY();
        double z = this.getZ();
        Vec3 back = this.getDeltaMovement().normalize().scale(-1.0);

        TripodDawnParticles.send(server, TripodDawnParticles.HEAT_RAY_BRIGHT, x, y, z, 24, 0.5, 0.5, 0.5, 0.3);
        TripodDawnParticles.send(server, TripodDawnParticles.BLAST, x, y, z, 10, 1.4, 1.4, 1.4, 0.06);
        TripodDawnParticles.send(server, TripodDawnParticles.HEAT_RAY,
                x + back.x, y + back.y, z + back.z, 20, 1.6, 1.6, 1.6, 0.18);
        TripodDawnParticles.send(server, ParticleTypes.LAVA, x, y, z, 8, 0.6, 0.6, 0.6, 0.0);
        TripodDawnParticles.send(server, ParticleTypes.LARGE_SMOKE, x, y + 0.5, z, 12, 1.0, 0.8, 1.0, 0.04);
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putInt("Pierced", this.pierced);
        output.putString("Mode", this.mode.name());
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        this.pierced = input.getIntOr("Pierced", 0);
        this.mode = Mode.valueOf(input.getStringOr("Mode", Mode.CHARGED.name()));
    }

    @Override
    protected double getDefaultGravity() {
        return 0.0;
    }
}
