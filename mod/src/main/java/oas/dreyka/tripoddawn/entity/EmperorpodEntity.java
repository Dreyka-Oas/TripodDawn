package oas.dreyka.tripoddawn.entity;

import oas.dreyka.tripoddawn.sound.TripodDawnSounds;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;

/**
 * The one that ends a run.
 *
 * <p>It carries no bar at the top of the screen. Nothing else in this mod tells a player what is
 * happening in writing, and a health bar with a name on it is writing: it also gives away that the
 * shape on the horizon is the last one rather than another uberpod.
 */
public class EmperorpodEntity extends MachineEntity {

    public EmperorpodEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MOVEMENT_SPEED, 0.35)
                .add(Attributes.MAX_HEALTH, 450.0)
                .add(Attributes.ARMOR, 25.0)
                .add(Attributes.ATTACK_DAMAGE, 60.0)
                .add(Attributes.FOLLOW_RANGE, HUNT_RANGE)
                .add(Attributes.STEP_HEIGHT, STEP_UP)
                .add(Attributes.KNOCKBACK_RESISTANCE, 40.0)
                .add(Attributes.ATTACK_KNOCKBACK, 1.0);
    }

    @Override
    protected String animationPrefix() {
        return "emperorpod";
    }

    /** Measured against a block ruler in game, like the walker, and it is the taller model. */
    @Override
    public float modelHeight() {
        return 44.0f;
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
