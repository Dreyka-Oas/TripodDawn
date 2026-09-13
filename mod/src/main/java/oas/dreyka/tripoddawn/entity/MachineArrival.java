package oas.dreyka.tripoddawn.entity;

import oas.dreyka.tripoddawn.effect.TripodDawnEffects;
import oas.dreyka.tripoddawn.particle.TripodDawnParticles;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;

/**
 * What a player feels when a machine comes up under them: the ground shakes and dirt goes
 * everywhere. Nothing is written to the chat, here or anywhere else in the mod.
 */
public final class MachineArrival {
    private MachineArrival() {
    }

    private static final double SHAKE_RADIUS = 40.0;
    private static final int SHAKE_TICKS = 240;
    private static final int DUST_PERIOD = 2;

    public static void announce(ServerLevel level, MachineEntity machine) {
        for (ServerPlayer player : level.players()) {
            if (player.distanceToSqr(machine) <= SHAKE_RADIUS * SHAKE_RADIUS) {
                shake(player, SHAKE_TICKS);
            }
        }
    }

    /** The effect carries no icon and no particles: the camera moving is the whole of it. */
    public static void shake(ServerPlayer player, int ticks) {
        player.addEffect(new MobEffectInstance(TripodDawnEffects.EARTHQUAKE, ticks, 0, false, false, true));
    }

    /** Called through the rise, so the ground keeps breaking up for the whole thirty seconds. */
    public static void dust(ServerLevel level, MachineEntity machine) {
        // Once every two ticks rather than every one: the cloud reads the same and a rising machine
        // stops costing its watchers six hundred packets.
        if (machine.tickCount % DUST_PERIOD != 0) {
            return;
        }
        double reach = machine.getBbWidth();
        TripodDawnParticles.send(level, TripodDawnParticles.DIRT_CLOUD,
                machine.getX(), machine.getY() + 0.2, machine.getZ(),
                8, reach, 0.4, reach, 0.02);
    }
}
