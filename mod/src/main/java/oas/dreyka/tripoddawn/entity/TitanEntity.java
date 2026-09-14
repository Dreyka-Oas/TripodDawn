package oas.dreyka.tripoddawn.entity;

import oas.dreyka.tripoddawn.sound.TripodDawnSounds;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;

/**
 * The big one.
 *
 * <p>A hundred blocks, two and a half times anything else the invasion owns, and the first thing
 * about it a player learns is that it is on the horizon and the trees are not. It is the only
 * machine whose silhouette is the whole warning, so nothing announces it: no bar at the top of the
 * screen, no line in the chat, the same silence as the rest.
 *
 * <p>Scaled through the vanilla attribute rather than drawn at that size, so the hit slabs, the
 * muzzle height and the stamp reach all follow it on their own.
 */
public class TitanEntity extends MachineEntity {

    /** A hundred blocks out of the walker the model was cut for. */
    public static final float SCALE = 2.6f;

    public TitanEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                // Slower than anything else on the field. It does not need to close, it outranges you.
                .add(Attributes.MOVEMENT_SPEED, 0.22)
                .add(Attributes.MAX_HEALTH, 900.0)
                .add(Attributes.ARMOR, 30.0)
                .add(Attributes.ATTACK_DAMAGE, 70.0)
                .add(Attributes.FOLLOW_RANGE, HUNT_RANGE)
                .add(Attributes.STEP_HEIGHT, STEP_UP)
                .add(Attributes.KNOCKBACK_RESISTANCE, 10.0)
                .add(Attributes.ATTACK_KNOCKBACK, 1.0)
                .add(Attributes.SCALE, SCALE);
    }

    @Override
    protected String animationPrefix() {
        return "tripod_invaders";
    }

    /** It falls apart on the walker's collapsed model, grown by the same scale. */
    @Override
    public String wreckPrefix() {
        return "dead_tripod";
    }

    /** The walker's model, and the scale attribute does the rest. */
    @Override
    public float modelHeight() {
        return 40.0f;
    }

    @Override
    protected int experience() {
        return 200;
    }

    @Override
    protected SoundEvent hornSound() {
        return TripodDawnSounds.EMPERORPOD_HORN;
    }
}
