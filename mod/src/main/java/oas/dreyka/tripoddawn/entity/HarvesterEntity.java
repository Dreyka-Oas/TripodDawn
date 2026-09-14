package oas.dreyka.tripoddawn.entity;

import oas.dreyka.tripoddawn.sound.TripodDawnSounds;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;

/** Lighter than a tripod and it hunts villagers first. */
public class HarvesterEntity extends MachineEntity {

    public HarvesterEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MOVEMENT_SPEED, 0.35)
                .add(Attributes.MAX_HEALTH, 180.0)
                .add(Attributes.ARMOR, 15.0)
                .add(Attributes.ATTACK_DAMAGE, 30.0)
                .add(Attributes.FOLLOW_RANGE, HUNT_RANGE)
                .add(Attributes.STEP_HEIGHT, STEP_UP)
                .add(Attributes.KNOCKBACK_RESISTANCE, 10.0)
                .add(Attributes.ATTACK_KNOCKBACK, 0.5);
    }

    @Override
    protected String animationPrefix() {
        return "tripod_harvester";
    }

    @Override
    public String wreckPrefix() {
        return "dead_harvester";
    }

    /** Measured against a block ruler in game: the box it is registered at holds three fifths of it. */
    @Override
    public float modelHeight() {
        return 40.0f;
    }

    @Override
    protected int experience() {
        return 30;
    }

    @Override
    protected SoundEvent hornSound() {
        return TripodDawnSounds.TRIPOD_HORN;
    }
}
