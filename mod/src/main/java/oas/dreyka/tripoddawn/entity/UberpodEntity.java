package oas.dreyka.tripoddawn.entity;

import oas.dreyka.tripoddawn.sound.TripodDawnSounds;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;

/** Heavier, and it heals itself while it is still mostly intact. */
public class UberpodEntity extends MachineEntity {

    private static final float REGEN_ABOVE = 220.0f;

    public UberpodEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MOVEMENT_SPEED, 0.35)
                .add(Attributes.MAX_HEALTH, 280.0)
                .add(Attributes.ARMOR, 25.0)
                .add(Attributes.ATTACK_DAMAGE, 40.0)
                .add(Attributes.FOLLOW_RANGE, HUNT_RANGE)
                .add(Attributes.STEP_HEIGHT, STEP_UP)
                .add(Attributes.KNOCKBACK_RESISTANCE, 10.0)
                .add(Attributes.ATTACK_KNOCKBACK, 0.5);
    }

    @Override
    public void tick() {
        super.tick();
        // Above the threshold it patches itself up, below it the damage sticks. Hitting it hard
        // enough in one push is what beats it; chipping at it is not.
        if (this.level() instanceof ServerLevel && !emerging() && this.isAlive()
                && this.getHealth() > REGEN_ABOVE && this.tickCount % 40 == 0) {
            this.heal(1.0f);
        }
    }

    @Override
    protected String animationPrefix() {
        return "uberpod";
    }

    @Override
    public String wreckPrefix() {
        return "dead_uberpod";
    }

    /** Measured against a block ruler in game, like the walker, and it is the taller model. */
    @Override
    public float modelHeight() {
        return 44.0f;
    }

    @Override
    protected int experience() {
        return 80;
    }

    @Override
    protected SoundEvent hornSound() {
        return TripodDawnSounds.UBERPOD_HORN;
    }
}
