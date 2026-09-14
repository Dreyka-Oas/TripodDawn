package oas.dreyka.tripoddawn.entity;

import oas.dreyka.tripoddawn.effect.TripodDawnEffects;
import oas.dreyka.tripoddawn.particle.TripodDawnParticles;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.level.levelgen.Heightmap;

/**
 * What a player feels when a machine comes up under them: the sky is struck, the ground shakes and
 * dirt goes everywhere. Nothing is written to the chat, here or anywhere else in the mod.
 */
public final class MachineArrival {
    private MachineArrival() {
    }

    private static final double SHAKE_RADIUS = 40.0;
    private static final int SHAKE_TICKS = 240;
    private static final int DUST_PERIOD = 2;

    /** The length of the rumble clip, in ticks, so a repeat starts where the last one ran out. */
    public static final int RUMBLE_TICKS = 80;

    /** Spread over the first eight seconds of the rise, the first one on the machine itself. */
    private static final int[] STRIKE_TICKS = {0, 35, 95, 160};
    private static final double STRIKE_SPREAD = 7.0;

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

    /**
     * The sky answers before the ground opens, four times over the spot the machine is coming out
     * of.
     *
     * <p>The bolts are visual only. A siege night puts three of these arrivals in the world and each
     * one lights four bolts, so real ones would leave a player fighting a wildfire instead of the
     * thing that started it, and would hand out charged creepers on the way. The flash and the crack
     * survive the flag, which is all the arrival needs.
     */
    public static void lightning(ServerLevel level, MachineEntity machine, int tick) {
        if (!struckAt(tick)) {
            return;
        }
        double spread = tick == STRIKE_TICKS[0] ? 0.0 : STRIKE_SPREAD;
        double x = machine.getX() + (level.getRandom().nextDouble() * 2.0 - 1.0) * spread;
        double z = machine.getZ() + (level.getRandom().nextDouble() * 2.0 - 1.0) * spread;
        bolt(level, level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING,
                BlockPos.containing(x, machine.getY(), z)));
    }

    /** One strike on a column, harmless, shared by the arrival and by the barrage that opens a night. */
    public static void bolt(ServerLevel level, BlockPos ground) {
        LightningBolt bolt = EntityType.LIGHTNING_BOLT.spawn(level, ground, EntitySpawnReason.EVENT);
        if (bolt != null) {
            bolt.setVisualOnly(true);
        }
    }

    private static boolean struckAt(int tick) {
        for (int at : STRIKE_TICKS) {
            if (at == tick) {
                return true;
            }
        }
        return false;
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
