package oas.dreyka.tripoddawn.invasion;

import oas.dreyka.tripoddawn.entity.MachineEntity;
import oas.dreyka.tripoddawn.entity.TripodDawnEntities;

import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;

import java.util.List;

/**
 * How far the invasion has got, read off the day count.
 *
 * <p>The source mod had no progression at all: every machine could turn up on the first night, which
 * meant a new world was either empty or unplayable depending on one roll. The ladder below is what
 * replaces it. Each rung opens a species and raises the numbers, so a world gets louder the longer
 * it runs rather than deciding its difficulty once.
 */
public enum InvasionTier {

    /** Before anything. A fresh world is an ordinary world. */
    QUIET(0, 0, 0, 0, 0, List.of()),

    /** One horn over the horizon and the ground moving. Nothing arrives, and nothing is written. */
    OMEN(1, 0, 0, 0, 8, List.of()),

    /** Foot soldiers only. A player with a sword handles a night of these. */
    SCOUTS(3, 3, 0, 0, 12, List.of()),

    /** The first walker. One a night, two standing at most. */
    WALKERS(8, 4, 1, 2, 18, List.of(TripodDawnEntities.TRIPOD)),

    HARVEST(14, 5, 2, 4, 24, List.of(TripodDawnEntities.TRIPOD, TripodDawnEntities.HARVESTER)),

    /**
     * The per-night count stops being the limit here and the standing count takes over, which is
     * what "no ceiling" can mean without a server walking a hundred entities per tick.
     */
    SIEGE(20, 6, 3, 8, 34, List.of(
            TripodDawnEntities.TRIPOD, TripodDawnEntities.HARVESTER, TripodDawnEntities.UBERPOD)),

    /** The emperorpod comes once, on the first night of this rung, and never again. */
    EMPEROR(30, 8, 4, 10, 48, List.of(
            TripodDawnEntities.TRIPOD, TripodDawnEntities.HARVESTER, TripodDawnEntities.UBERPOD));

    private final int firstDay;
    private final int martians;
    private final int machinesPerNight;
    private final int machinesAlive;
    private final int strikes;
    private final List<EntityType<? extends MachineEntity>> pool;

    InvasionTier(int firstDay, int martians, int machinesPerNight, int machinesAlive, int strikes,
                 List<EntityType<? extends MachineEntity>> pool) {
        this.firstDay = firstDay;
        this.martians = martians;
        this.machinesPerNight = machinesPerNight;
        this.machinesAlive = machinesAlive;
        this.strikes = strikes;
        this.pool = pool;
    }

    public int firstDay() {
        return this.firstDay;
    }

    /** Martians per player, per night. */
    public int martians() {
        return this.martians;
    }

    public int machinesPerNight() {
        return this.machinesPerNight;
    }

    public int machinesAlive() {
        return this.machinesAlive;
    }

    /**
     * Bolts per player over the barrage that opens a night, most of them with nothing under them.
     *
     * <p>This is the one number a player can read the rung off, since nothing is written to the
     * chat: the sky over a day 30 world is six times busier than the sky over a day 1 world, and it
     * says so before the first machine is out of the ground.
     */
    public int strikes() {
        return this.strikes;
    }

    public boolean hasMachines() {
        return !this.pool.isEmpty();
    }

    /**
     * Which machine comes up. Weighted towards the newest species of the rung so that reaching a
     * tier is something a player sees rather than something they read: the last entry of the pool
     * wins half the rolls, the older ones share the rest.
     */
    public EntityType<? extends MachineEntity> roll(RandomSource random) {
        int last = this.pool.size() - 1;
        if (last <= 0 || random.nextBoolean()) {
            return this.pool.get(last);
        }
        return this.pool.get(random.nextInt(last));
    }

    /** The rung a given day sits on. */
    public static InvasionTier forDay(long day) {
        InvasionTier found = QUIET;
        for (InvasionTier tier : values()) {
            if (day >= tier.firstDay) {
                found = tier;
            }
        }
        return found;
    }
}
