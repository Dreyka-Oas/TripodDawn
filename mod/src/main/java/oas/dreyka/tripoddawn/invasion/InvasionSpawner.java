package oas.dreyka.tripoddawn.invasion;

import oas.dreyka.tripoddawn.entity.MachineArrival;
import oas.dreyka.tripoddawn.entity.MachineEntity;
import oas.dreyka.tripoddawn.entity.TripodDawnEntities;
import oas.dreyka.tripoddawn.sound.TripodDawnSounds;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.level.levelgen.Heightmap;

import java.util.ArrayList;
import java.util.List;

/**
 * Who turns up at night, and where.
 *
 * <p>The whole invasion is decided once per night rather than rolled per tick, which is what keeps
 * the cost of the mod at nothing on a server nobody is playing on. Nothing here writes to the chat:
 * a player learns what rung they are on by hearing it.
 */
public final class InvasionSpawner {
    private InvasionSpawner() {
    }

    /** Far enough that a walker is a silhouette when it comes up, near enough to be found. */
    private static final int MACHINE_MIN = 48;
    private static final int MACHINE_MAX = 96;
    private static final int MARTIAN_MIN = 20;
    private static final int MARTIAN_MAX = 44;

    /** Tries before a placement is given up on, per creature. */
    private static final int PLACEMENT_TRIES = 16;

    /** A machine refuses far more ground than a martian, so it gets to knock on more doors. */
    private static final int MACHINE_TRIES = 48;

    /** How much room a machine needs above its feet. Its full height would refuse every forest. */
    private static final int MACHINE_CLEARANCE = 6;

    /** Half the footprint a machine is measured on, at its corners rather than block by block. */
    private static final int MACHINE_FOOTPRINT = 2;

    /** Ground can slope under a machine; past this it is a cliff and a leg ends up in the air. */
    private static final int MACHINE_SLOPE = 2;

    private static final int OMEN_SHAKE_TICKS = 200;

    /** How long the sky keeps tearing after a night opens, and how long the weather is held. */
    private static final int STORM_TICKS = 400;
    private static final int STORM_WEATHER_TICKS = 9000;

    /** Where a barrage bolt can land. Near enough to light the ground a player stands on. */
    private static final int STORM_MIN = 20;
    private static final int STORM_MAX = 96;

    /** Sundown and sunup, in day time. */
    private static final int NIGHT_FROM = 13000;
    private static final int NIGHT_UNTIL = 23000;

    public static void register() {
        ServerTickEvents.END_WORLD_TICK.register(InvasionSpawner::tick);
    }

    private static void tick(ServerLevel level) {
        // The invasion is an overworld affair: a machine climbing out of the nether roof would be a
        // bug rather than a scene.
        if (level.dimension() != Level.OVERWORLD) {
            return;
        }
        InvasionState state = InvasionState.of(level);
        if (state.storming(level)) {
            storm(level, state);
        }
        if (!night(level)) {
            return;
        }
        long day = state.day(level);
        if (targets(level).isEmpty()) {
            return;
        }
        if (state.claimNight(day)) {
            runNight(level, state, InvasionNight.of(day));
        }
    }

    /**
     * Whether the sun is down.
     *
     * <p>Read off the clock rather than off the light, because a thunderstorm darkens a noon sky and
     * this mod forces one itself at the top of every invasion night. Asking the sky instead would open
     * a night at midday, and open the next one at dawn while the previous night's storm was still
     * running.
     */
    private static boolean night(ServerLevel level) {
        long time = level.getDayTime() % InvasionState.TICKS_PER_DAY;
        return time >= NIGHT_FROM && time < NIGHT_UNTIL;
    }

    /**
     * The barrage, spread over the twenty seconds a night opens on rather than fired at once.
     *
     * <p>Almost none of these bolts has anything under it. That is the point: the four a machine
     * lights on its way up are the same bolts, so a player watching the horizon cannot tell which
     * strike is about to stand up and walk at them.
     */
    private static void storm(ServerLevel level, InvasionState state) {
        int strikes = state.night(level).strikes();
        if (strikes <= 0) {
            return;
        }
        RandomSource random = level.getRandom();
        for (ServerPlayer player : targets(level)) {
            if (random.nextInt(STORM_TICKS) >= strikes) {
                continue;
            }
            double angle = random.nextDouble() * Math.PI * 2.0;
            double reach = STORM_MIN + random.nextDouble() * (STORM_MAX - STORM_MIN);
            BlockPos probe = new BlockPos(
                    Mth.floor(player.getX() + Math.cos(angle) * reach),
                    level.getMinY(),
                    Mth.floor(player.getZ() + Math.sin(angle) * reach));
            if (level.isLoaded(probe)) {
                MachineArrival.bolt(level, column(level, probe));
            }
        }
    }

    /** One night's worth of arrivals. Called by the tick above and by the test command. */
    public static int runNight(ServerLevel level, InvasionState state, InvasionNight night) {
        List<ServerPlayer> players = targets(level);
        if (players.isEmpty()) {
            return 0;
        }

        // The sky is the announcement, and it has to be the right sky. Left to the vanilla cycle an
        // invasion night lands clear as often as not, and a barrage under a clear sky reads as a bug.
        level.setWeatherParameters(0, STORM_WEATHER_TICKS, true, true);
        state.openStorm(level, STORM_TICKS);

        // The first night of a world gets the horn and the ground moving on top of its arrival, once
        // and never again, so that the thing coming up is announced rather than simply there.
        if (state.claimOmen()) {
            omen(level, players);
        }

        int spawned = 0;
        for (ServerPlayer player : players) {
            for (int i = 0; i < night.martians(); i++) {
                if (place(level, TripodDawnEntities.MARTIAN, player, MARTIAN_MIN, MARTIAN_MAX, false) != null) {
                    spawned++;
                }
            }
        }

        if (!night.hasMachines()) {
            return spawned;
        }

        // Past the siege rung the per-night number stops mattering and the standing number takes
        // over, which is the ceiling a server can actually hold.
        int room = Math.min(night.machinesPerNight(), night.machinesAlive() - countMachines(level));
        RandomSource random = level.getRandom();
        for (int i = 0; i < room; i++) {
            ServerPlayer player = players.get(random.nextInt(players.size()));
            if (place(level, night.roll(random), player, MACHINE_MIN, MACHINE_MAX, true) != null) {
                spawned++;
            }
        }

        if (night.tier() == InvasionTier.EMPEROR && state.claimEmperor()) {
            ServerPlayer player = players.get(random.nextInt(players.size()));
            if (place(level, TripodDawnEntities.EMPERORPOD, player, MACHINE_MIN, MACHINE_MAX, true) != null) {
                spawned++;
            }
        }
        return spawned;
    }

    private static void omen(ServerLevel level, List<ServerPlayer> players) {
        for (ServerPlayer player : players) {
            // Played at the player's own feet rather than from a point on the horizon, because a
            // sound emitted sixteen chunks away never reaches them at all.
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    TripodDawnSounds.DISTANT_HORN, SoundSource.AMBIENT, 0.6f, 0.7f);
            MachineArrival.shake(player, OMEN_SHAKE_TICKS);
        }
    }

    /** Drops one creature on solid ground around a player, or returns null if nowhere works. */
    public static <T extends Mob> T place(ServerLevel level, EntityType<T> type, ServerPlayer around,
                                          int min, int max, boolean machine) {
        RandomSource random = level.getRandom();
        int tries = machine ? MACHINE_TRIES : PLACEMENT_TRIES;
        for (int attempt = 0; attempt < tries; attempt++) {
            double angle = random.nextDouble() * Math.PI * 2.0;
            double reach = min + random.nextDouble() * (max - min);
            int x = Mth.floor(around.getX() + Math.cos(angle) * reach);
            int z = Mth.floor(around.getZ() + Math.sin(angle) * reach);

            // Asked before the heightmap, which would otherwise generate the chunk to answer.
            BlockPos probe = new BlockPos(x, level.getMinY(), z);
            if (!level.isLoaded(probe)) {
                continue;
            }
            BlockPos surface = column(level, probe);
            if (!suitable(level, surface, machine)) {
                continue;
            }

            T mob = type.spawn(level, surface, EntitySpawnReason.EVENT);
            if (mob == null) {
                continue;
            }
            // The rise animation puts a machine half under the ground for thirty seconds, which only
            // works when the spawner put it there on purpose.
            if (!(mob instanceof MachineEntity)) {
                mob.setYRot(random.nextFloat() * 360.0f);
            }
            return mob;
        }
        return null;
    }

    private static boolean suitable(ServerLevel level, BlockPos surface, boolean machine) {
        if (!standable(level, surface, machine ? MACHINE_CLEARANCE : 2)) {
            return false;
        }
        if (!machine) {
            return true;
        }
        // A machine covers more ground than a block, and its corners are read on their own column:
        // measuring them at the middle's height turns every slope in the world into a refusal.
        int step = MACHINE_FOOTPRINT * 2;
        for (int dx = -MACHINE_FOOTPRINT; dx <= MACHINE_FOOTPRINT; dx += step) {
            for (int dz = -MACHINE_FOOTPRINT; dz <= MACHINE_FOOTPRINT; dz += step) {
                BlockPos corner = column(level, surface.offset(dx, 0, dz));
                if (Math.abs(corner.getY() - surface.getY()) > MACHINE_SLOPE
                        || !standable(level, corner, MACHINE_CLEARANCE)) {
                    return false;
                }
            }
        }
        return true;
    }

    /** The first free block over the ground of a column. */
    private static BlockPos column(ServerLevel level, BlockPos anywhere) {
        return level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, anywhere);
    }

    /**
     * Whether a creature can be dropped on this block. Clearance counts what nothing walks into
     * rather than air, since a meadow is a solid ceiling of short grass as far as air is concerned.
     */
    private static boolean standable(ServerLevel level, BlockPos surface, int clearance) {
        BlockPos ground = surface.below();
        if (!level.getFluidState(ground).isEmpty()
                || !level.getBlockState(ground).isFaceSturdy(level, ground, Direction.UP)) {
            return false;
        }
        for (int dy = 0; dy < clearance; dy++) {
            BlockPos above = surface.above(dy);
            if (!level.getBlockState(above).getCollisionShape(level, above).isEmpty()) {
                return false;
            }
        }
        return true;
    }

    public static int countMachines(ServerLevel level) {
        return level.getEntities(EntityTypeTest.forClass(MachineEntity.class), Entity::isAlive).size();
    }

    /** Who the invasion is aimed at. A spectator is not in the world as far as it is concerned. */
    private static List<ServerPlayer> targets(ServerLevel level) {
        List<ServerPlayer> found = new ArrayList<>();
        for (ServerPlayer player : level.players()) {
            if (!player.isSpectator()) {
                found.add(player);
            }
        }
        return found;
    }
}
