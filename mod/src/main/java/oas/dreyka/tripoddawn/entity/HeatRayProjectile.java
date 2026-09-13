package oas.dreyka.tripoddawn.entity;

import oas.dreyka.tripoddawn.particle.TripodDawnParticles;
import oas.dreyka.tripoddawn.sound.TripodDawnSounds;

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

    private static final float DIRECT_DAMAGE = 500.0f;
    private static final float SPLASH_DAMAGE = 40.0f;
    private static final double SPLASH_RADIUS = 3.5;
    private static final int MAX_PIERCE = 10;

    /**
     * Ticks of flight, which at three blocks each is ninety blocks. Well past the sixty-four a
     * machine shoots at, and short enough that the ray always dies inside the area the server is
     * still ticking: one that flies out of it stops ageing and stays in the world for good.
     */
    private static final int MAX_AGE = 30;

    private int pierced;

    public HeatRayProjectile(EntityType<? extends HeatRayProjectile> type, Level level) {
        super(type, level);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
    }

    /** Aims from the machine's hood rather than from its feet, twenty-four blocks lower. */
    public static void fire(ServerLevel level, MachineEntity machine, LivingEntity target) {
        HeatRayProjectile ray = new HeatRayProjectile(TripodDawnEntities.HEAT_RAY, level);
        double y = machine.getY() + machine.getBbHeight() * 0.85;
        ray.setPos(machine.getX(), y, machine.getZ());
        ray.setOwner(machine);

        Vec3 aim = new Vec3(
                target.getX() - machine.getX(),
                target.getY() + target.getBbHeight() * 0.5 - y,
                target.getZ() - machine.getZ());
        ray.shoot(aim.x, aim.y, aim.z, 3.0f, 0.0f);

        level.addFreshEntity(ray);
        TripodDawnParticles.send(level, TripodDawnParticles.HEAT_RAY_BRIGHT,
                machine.getX(), y, machine.getZ(), 4, 0.4, 0.4, 0.4, 0.02);
        machine.playSound(TripodDawnSounds.HEAT_RAY, 10.0f, 1.0f);
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

        Vec3 motion = this.getDeltaMovement();
        this.setPos(this.getX() + motion.x, this.getY() + motion.y, this.getZ() + motion.z);

        if (this.level() instanceof ServerLevel server) {
            TripodDawnParticles.send(server, TripodDawnParticles.HEAT_RAY,
                    this.getX(), this.getY(), this.getZ(), 2, 0.1, 0.1, 0.1, 0.0);
        }
    }

    /** Machines never shoot each other, for the same reason they never catch each other's fire. */
    @Override
    protected boolean canHitEntity(Entity target) {
        return !(target instanceof MachineEntity) && super.canHitEntity(target);
    }

    @Override
    protected void onHitEntity(EntityHitResult hit) {
        super.onHitEntity(hit);
        if (!(this.level() instanceof ServerLevel server)) {
            return;
        }
        Entity victim = hit.getEntity();
        victim.hurtServer(server, this.damageSources().onFire(), DIRECT_DAMAGE);
        victim.igniteForSeconds(8.0f);

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
        TripodDawnParticles.send(server, TripodDawnParticles.BLAST,
                this.getX(), this.getY(), this.getZ(), 8, 1.2, 1.2, 1.2, 0.05);

        for (LivingEntity nearby : server.getEntitiesOfClass(LivingEntity.class,
                new AABB(this.position(), this.position()).inflate(SPLASH_RADIUS))) {
            // Machines never catch their own fire, otherwise two of them in one street kill each
            // other before they reach anything a player built.
            if (nearby == this.getOwner() || nearby instanceof MachineEntity) {
                continue;
            }
            nearby.hurtServer(server, this.damageSources().onFire(), SPLASH_DAMAGE);
        }

        server.explode(this.getOwner(), this.getX(), this.getY(), this.getZ(), 2.5f, true,
                Level.ExplosionInteraction.MOB);
        server.gameEvent(GameEvent.EXPLODE, this.position(), GameEvent.Context.of(this));
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putInt("Pierced", this.pierced);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        this.pierced = input.getIntOr("Pierced", 0);
    }

    @Override
    protected double getDefaultGravity() {
        return 0.0;
    }
}
