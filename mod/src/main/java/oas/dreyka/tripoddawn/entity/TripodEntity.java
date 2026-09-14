package oas.dreyka.tripoddawn.entity;

import oas.dreyka.tripoddawn.sound.TripodDawnSounds;

import net.minecraft.core.Holder;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import java.util.Locale;

/** The machine the mod is named after, in its three builds. */
public class TripodEntity extends MachineEntity {

    private static final EntityDataAccessor<Byte> DATA_VARIANT =
            SynchedEntityData.defineId(TripodEntity.class, EntityDataSerializers.BYTE);

    public TripodEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MOVEMENT_SPEED, TripodVariant.LINE.speed())
                .add(Attributes.MAX_HEALTH, TripodVariant.LINE.health())
                .add(Attributes.ARMOR, TripodVariant.LINE.armour())
                .add(Attributes.ATTACK_DAMAGE, TripodVariant.LINE.stamp())
                .add(Attributes.FOLLOW_RANGE, HUNT_RANGE)
                .add(Attributes.STEP_HEIGHT, STEP_UP)
                .add(Attributes.KNOCKBACK_RESISTANCE, 10.0)
                .add(Attributes.ATTACK_KNOCKBACK, 0.5)
                .add(Attributes.SCALE, TripodVariant.LINE.scale());
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_VARIANT, (byte) TripodVariant.LINE.ordinal());
    }

    public TripodVariant variant() {
        return TripodVariant.byId(this.entityData.get(DATA_VARIANT));
    }

    /**
     * Writes the build onto the machine.
     *
     * <p>Base values rather than modifiers: a modifier survives being reapplied and a second call
     * would stack, and this is called again every time the world reloads the entity.
     */
    public void setVariant(TripodVariant variant) {
        this.entityData.set(DATA_VARIANT, (byte) variant.ordinal());
        set(Attributes.MAX_HEALTH, variant.health());
        set(Attributes.ARMOR, variant.armour());
        set(Attributes.MOVEMENT_SPEED, variant.speed());
        set(Attributes.ATTACK_DAMAGE, variant.stamp());
        set(Attributes.SCALE, variant.scale());
        this.xpReward = variant.experience();
        this.setHealth(this.getMaxHealth());
        this.refreshDimensions();
    }

    private void set(Holder<Attribute> attribute, double value) {
        AttributeInstance instance = this.getAttribute(attribute);
        if (instance != null) {
            instance.setBaseValue(value);
        }
    }

    /** A spawn egg or a command gives no build, so the day the world is on picks one. */
    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty,
                                        EntitySpawnReason reason, SpawnGroupData data) {
        setVariant(TripodVariant.roll(this.random, level.getLevel().getDayTime() / 24000L));
        return super.finalizeSpawn(level, difficulty, reason, data);
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putString("Variant", variant().name().toLowerCase(Locale.ROOT));
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        setVariant(parse(input.getStringOr("Variant", TripodVariant.LINE.name())));
        // Written after the attributes, which reset the bar to full: a machine that was loaded
        // half dead has to come back half dead.
        this.setHealth(input.getFloatOr("Health", this.getMaxHealth()));
    }

    private static TripodVariant parse(String name) {
        try {
            return TripodVariant.valueOf(name.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException unknown) {
            return TripodVariant.LINE;
        }
    }

    @Override
    protected String animationPrefix() {
        return "tripod_invaders";
    }

    /** The bones and the sheet are the build's own. The clips stay the ones all three walk on. */
    @Override
    public String modelName() {
        return variant().model();
    }

    @Override
    public String wreckPrefix() {
        return "dead_tripod";
    }

    /** Measured against a block ruler in game: the box it is registered at holds three fifths of it. */
    @Override
    public float modelHeight() {
        return 40.0f;
    }

    @Override
    protected int experience() {
        return TripodVariant.LINE.experience();
    }

    @Override
    protected SoundEvent hornSound() {
        return TripodDawnSounds.TRIPOD_HORN;
    }
}
