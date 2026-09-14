package oas.dreyka.tripoddawn.entity;

import oas.dreyka.tripoddawn.sound.TripodDawnSounds;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * One hittable slab of a machine.
 *
 * <p>A walker is twenty-four blocks of thin legs holding a hood up, and one box around all of it is
 * wrong whichever width it is given: narrow enough to sit on the legs and an arrow aimed at the hood
 * flies past, wide enough to hold the hood and the thing has a four-block wall of nothing around its
 * shins. It is cut into three stacked boxes instead, and where a shot lands decides what it is worth.
 *
 * <p>Boxes stacked rather than placed around the model, because a slab centred on the machine needs
 * no rotating: it reads the same from every angle, which is what a player firing at a thing that
 * walks in circles actually needs.
 */
public class MachinePart extends Entity {

    /**
     * Where a shot can land, and what it is worth there.
     *
     * <p>The spans overlap on purpose. A shot that arrives at the seam belongs to whichever box the
     * game hands it, and both answers are defensible; a gap there would be a shot that hits nothing.
     */
    public enum Zone {
        /**
         * Thin, far apart, and mostly air. Little of what is aimed at them is load bearing.
         *
         * <p>Four fifths of the machine and wide enough to hold the splay: a walker's feet are
         * sixteen blocks apart on the ground, which is nearly five times the box it collides with.
         */
        LEGS(4.0f, 0.00f, 0.80f, 0.6f, 0.75f),

        /** The hull. What a machine is, as far as damage is concerned. */
        HULL(2.9f, 0.76f, 0.92f, 1.0f, 1.05f),

        /** The hood, where the ray comes out and where the armour cannot be. */
        HOOD(1.9f, 0.88f, 1.03f, 2.5f, 1.55f);

        private final float width;
        private final float bottom;
        private final float top;
        private final float worth;
        private final float pitch;

        Zone(float width, float bottom, float top, float worth, float pitch) {
            this.width = width;
            this.bottom = bottom;
            this.top = top;
            this.worth = worth;
            this.pitch = pitch;
        }

        /** What a hit here is multiplied by before it reaches the machine. */
        public float worth() {
            return this.worth;
        }

        /**
         * How the metal rings here.
         *
         * <p>A thick strut answers low and the thin plate over the hood answers high, which is the
         * one cue telling a player their arrow found the weak point. Nothing else says so: no number
         * comes up and the machine's own hurt sound is the same wherever it was hit.
         */
        public float pitch() {
            return this.pitch;
        }
    }

    // The size travels rather than being recomputed from the owner, because a player aims on their
    // own client: a slab the client thinks is one block cube cannot be shot at where it really is.
    private static final EntityDataAccessor<Float> DATA_WIDTH =
            SynchedEntityData.defineId(MachinePart.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_HEIGHT =
            SynchedEntityData.defineId(MachinePart.class, EntityDataSerializers.FLOAT);

    private MachineEntity owner;
    private Zone zone = Zone.HULL;

    public MachinePart(EntityType<? extends MachinePart> type, Level level) {
        super(type, level);
        this.noPhysics = true;
    }

    public MachinePart(MachineEntity owner, Zone zone) {
        this(TripodDawnEntities.MACHINE_PART, owner.level());
        this.owner = owner;
        this.zone = zone;
        follow();
    }

    public MachineEntity owner() {
        return this.owner;
    }

    public Zone zone() {
        return this.zone;
    }

    /** Sits the slab back on its machine. Called by the machine, every tick, before anything moves. */
    public void follow() {
        if (this.owner == null) {
            return;
        }
        // Measured every tick rather than once at birth. A machine's size comes from its scale
        // attribute, and an attribute carries no change event for the value it was created with, so
        // the first tick of a machine bigger than its registered size still reports the registered
        // one: a forty block titan would wear a twenty-four block machine's boxes for good.
        float tall = this.owner.drawnHeight();
        this.entityData.set(DATA_WIDTH, this.owner.getBbWidth() * this.zone.width);
        this.entityData.set(DATA_HEIGHT, tall * (this.zone.top - this.zone.bottom));
        this.setPos(this.owner.getX(),
                this.owner.getY() + tall * this.zone.bottom,
                this.owner.getZ());
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_WIDTH, 1.0f);
        builder.define(DATA_HEIGHT, 1.0f);
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
        if (DATA_WIDTH.equals(key) || DATA_HEIGHT.equals(key)) {
            this.refreshDimensions();
        }
        super.onSyncedDataUpdated(key);
    }

    @Override
    public EntityDimensions getDimensions(Pose pose) {
        return EntityDimensions.fixed(this.entityData.get(DATA_WIDTH), this.entityData.get(DATA_HEIGHT));
    }

    @Override
    public boolean isPickable() {
        return true;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean canBeCollidedWith(Entity entity) {
        return false;
    }

    @Override
    public boolean hurtServer(ServerLevel server, DamageSource source, float amount) {
        if (this.owner == null || MachineEntity.invader(source.getEntity())
                || MachineEntity.invader(source.getDirectEntity())) {
            return false;
        }
        if (!this.owner.hurtServer(server, source, amount * this.zone.worth)) {
            return false;
        }
        // Played from the slab and not from the machine, so the ring comes from the height the shot
        // landed at. On a forty block walker the difference between the shins and the hood is most
        // of the distance a sound has to travel to be placed at all.
        server.playSound(null, this.getX(), this.getY() + this.getBbHeight() * 0.5, this.getZ(),
                TripodDawnSounds.MACHINE_DEFLECT, this.owner.getSoundSource(), 2.5f, this.zone.pitch);
        return true;
    }

    /** A slab is the machine as far as the game is concerned, so a pick lands on the machine. */
    @Override
    public ItemStack getPickResult() {
        return this.owner == null ? ItemStack.EMPTY : this.owner.getPickResult();
    }

    /**
     * Never written to disk. A machine builds its own slabs on the first tick after it loads, and a
     * saved slab would come back orphaned, invisible and standing in the air.
     */
    @Override
    public boolean shouldBeSaved() {
        return false;
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
    }
}
