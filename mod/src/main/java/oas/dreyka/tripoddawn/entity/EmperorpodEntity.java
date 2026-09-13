package oas.dreyka.tripoddawn.entity;

import oas.dreyka.tripoddawn.sound.TripodDawnSounds;

import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.BossEvent;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;

/** The one that ends a run. It carries the bar at the top of the screen. */
public class EmperorpodEntity extends MachineEntity {

    private final ServerBossEvent bossBar =
            new ServerBossEvent(this.getDisplayName(), BossEvent.BossBarColor.RED, BossEvent.BossBarOverlay.PROGRESS);

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
                .add(Attributes.KNOCKBACK_RESISTANCE, 40.0)
                .add(Attributes.ATTACK_KNOCKBACK, 1.0);
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide()) {
            return;
        }
        // The wreck lies on the ground for five minutes. A bar held at zero over it reads as a fight
        // still going on, so the bar ends with the machine rather than with the body.
        if (this.isDeadOrDying()) {
            this.bossBar.removeAllPlayers();
            return;
        }
        this.bossBar.setProgress(this.getHealth() / this.getMaxHealth());
    }

    /**
     * The bar leaves with the entity whichever way the entity leaves.
     *
     * <p>Left to {@link #stopSeenByPlayer} alone, a machine taken out of the world without being
     * killed, which is what peaceful difficulty does to every monster in it, leaves its bar at the
     * top of the screen with nothing underneath, and the next one stacks below it.
     */
    @Override
    public void remove(RemovalReason reason) {
        super.remove(reason);
        this.bossBar.removeAllPlayers();
    }

    @Override
    public void startSeenByPlayer(ServerPlayer player) {
        super.startSeenByPlayer(player);
        this.bossBar.addPlayer(player);
    }

    @Override
    public void stopSeenByPlayer(ServerPlayer player) {
        super.stopSeenByPlayer(player);
        this.bossBar.removePlayer(player);
    }

    @Override
    protected String animationPrefix() {
        return "emperorpod";
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
