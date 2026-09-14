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
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * One hittable slab of a machine.
 *
 * <p>A walker is three thin legs holding a hood forty blocks up, and one box around all of it is
 * wrong whichever width it is given: narrow enough to sit on the hull and an arrow aimed at a leg
 * flies past, wide enough to hold the splay and the machine answers shots fired at the daylight
 * between its shins. It wears a column of slabs per leg and one up its own axis instead, and where a
 * shot lands decides what it is worth.
 *
 * <p>The leg columns turn with the machine, since a leg is out on one side rather than all round.
 * The axis columns do not, because nothing about them changes with the angle it is read from.
 *
 * <p>Columns, but many more slabs than columns. The game finds an entity by the chunk section its
 * position falls in, widened by four blocks, and never by the box it carries: a single slab covering
 * a walker's thirty blocks of legs answers arrows over the fifteen nearest its feet and lets
 * everything above that through. {@link MachineEntity} cuts each column into slabs short enough to
 * sit inside that reach.
 */
public class MachinePart extends Entity {

    /**
     * Where a shot can land, and what it is worth there.
     *
     * <p>The spans meet without overlapping. A shot that arrives at a seam belongs to whichever box
     * the game hands it and both answers are defensible, but a span covered twice is a second box
     * standing inside the first one, paid for on every tick of every machine in sight.
     */
    public enum Zone {
        /** Thin, far apart, and mostly air. Little of what is aimed at them is load bearing. */
        LEGS(0.00f, 0.76f, 0.6f, 0.75f),

        /** The hull. What a machine is, as far as damage is concerned. */
        HULL(0.76f, 0.90f, 1.0f, 1.05f),

        /**
         * The hood, where the ray comes out and where the armour cannot be.
         *
         * <p>Past the top of the drawn height on purpose. That height is the number every other
         * measurement here is taken against, and the model's own crown stands a tenth above it:
         * measured in game against a column of blocks, a walker drawn at forty reaches forty-four.
         */
        HOOD(0.90f, 1.12f, 2.5f, 1.55f);

        private final float bottom;
        private final float top;
        private final float worth;
        private final float pitch;

        Zone(float bottom, float top, float worth, float pitch) {
            this.bottom = bottom;
            this.top = top;
            this.worth = worth;
            this.pitch = pitch;
        }

        /** Where this zone starts and ends, as a fraction of the machine's drawn height. */
        public float bottom() {
            return this.bottom;
        }

        public float top() {
            return this.top;
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
    private float bottom;
    private float top = 1.0f;
    private float reach;
    private float angle;
    private float width = 0.1f;

    public MachinePart(EntityType<? extends MachinePart> type, Level level) {
        super(type, level);
        this.noPhysics = true;
    }

    /**
     * One slab of a column, every measurement a fraction of the machine's drawn height.
     *
     * <p>{@code reach} and {@code angle} are where the column stands around the machine's axis: a
     * leg column sits out on the leg and turns with the machine, the hull column sits on the axis
     * and ignores both.
     */
    public MachinePart(MachineEntity owner, Zone zone, float bottom, float top,
                       float reach, float angle, float width) {
        this(TripodDawnEntities.MACHINE_PART, owner.level());
        this.owner = owner;
        this.zone = zone;
        this.bottom = bottom;
        this.top = top;
        this.reach = reach;
        this.angle = angle;
        this.width = width;
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
        // one: a forty block titan would wear a walker's boxes for good.
        float tall = this.owner.drawnHeight();
        // Capped for the same reason the columns are cut into slabs at all. A search reaches four
        // blocks past the section holding the slab's own position, so a box reaching further than
        // that answers a shot coming from one direction and lets the same shot through from the
        // other: the walker was hittable from the east and transparent from the west at the waist.
        // The two caps differ because a column too tall is cut into more slabs and a box too wide
        // cannot be, so holding width to the height's limit left a titan's hull outside its own box.
        float width = Math.min(tall * this.width, MachineEntity.SPAN_SIZE);
        float height = Math.min(tall * (this.top - this.bottom), MachineEntity.SLAB_SIZE);
        // The synched-data callback that turns these two numbers into a box only fires on a client,
        // where the packet arrives, so the server has to rebuild the box itself.
        if (this.entityData.get(DATA_WIDTH) != width || this.entityData.get(DATA_HEIGHT) != height) {
            this.entityData.set(DATA_WIDTH, width);
            this.entityData.set(DATA_HEIGHT, height);
            refreshDimensions();
        }
        float yaw = (this.owner.yBodyRot + this.angle) * Mth.DEG_TO_RAD;
        double out = tall * this.reach;
        this.setPos(this.owner.getX() - Mth.sin(yaw) * out,
                this.owner.getY() + tall * (this.bottom + this.top) * 0.5f,
                this.owner.getZ() + Mth.cos(yaw) * out);
    }

    /**
     * Hung around its position rather than standing on it.
     *
     * <p>The reach of a search is four blocks either way, so a box growing upwards out of its own
     * position is hittable over three and a half and invisible above that, while the same reach
     * spent on both sides buys seven. It halves the slabs a forty block walker has to carry.
     */
    @Override
    protected AABB makeBoundingBox(Vec3 pos) {
        double half = this.entityData.get(DATA_WIDTH) * 0.5;
        double up = this.entityData.get(DATA_HEIGHT) * 0.5;
        return new AABB(pos.x - half, pos.y - up, pos.z - half,
                pos.x + half, pos.y + up, pos.z + half);
    }

    /**
     * Nothing of its own. The machine places it, and the rest of what an entity does every tick is
     * work a slab has no use for: the block scan alone would walk every block inside a box fourteen
     * wide, for every slab of every machine standing.
     */
    @Override
    public void tick() {
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
        server.playSound(null, this.getX(), this.getY(), this.getZ(),
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
