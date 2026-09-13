package oas.dreyka.tripoddawn.invasion;

import oas.dreyka.tripoddawn.entity.MachineEntity;

import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;

/**
 * One night's numbers, for one day of the calendar.
 *
 * <p>{@link InvasionTier} runs out at the emperor because there is no eighth species to open, and a
 * world whose hardest night is behind it is a world with nothing left in it. Past the last rung the
 * ladder keeps climbing on the numbers alone, one step every {@link #STEP_DAYS} days, for as long as
 * the world lasts.
 *
 * <p>The ceilings below are not a difficulty decision, they are the tick budget: a machine is
 * twenty-four blocks of pathfinding and the server walks every one of them every tick, so the count
 * standing at once is the one number that cannot be allowed to run away.
 */
public record InvasionNight(InvasionTier tier, int step, int martians, int machinesPerNight,
                            int machinesAlive, int strikes) {

    private static final int STEP_DAYS = 10;

    private static final int MARTIANS_PER_STEP = 2;
    private static final int MACHINES_PER_STEP = 1;
    private static final int ALIVE_PER_STEP = 2;
    private static final int STRIKES_PER_STEP = 8;

    private static final int MARTIANS_CAP = 32;
    private static final int MACHINES_CAP = 12;
    private static final int ALIVE_CAP = 24;
    private static final int STRIKES_CAP = 160;

    public static InvasionNight of(long day) {
        InvasionTier tier = InvasionTier.forDay(day);
        int step = step(day);
        return new InvasionNight(tier, step,
                climb(tier.martians(), step, MARTIANS_PER_STEP, MARTIANS_CAP),
                climb(tier.machinesPerNight(), step, MACHINES_PER_STEP, MACHINES_CAP),
                climb(tier.machinesAlive(), step, ALIVE_PER_STEP, ALIVE_CAP),
                climb(tier.strikes(), step, STRIKES_PER_STEP, STRIKES_CAP));
    }

    /** How many steps past the last written rung a day sits. Zero anywhere on the table itself. */
    private static int step(long day) {
        long past = day - InvasionTier.last().firstDay();
        return past <= 0 ? 0 : (int) Math.min(Integer.MAX_VALUE, past / STEP_DAYS);
    }

    /**
     * A number that was zero on the table stays zero: a rung with no martians written on it means
     * none of them belong to that night, not that it is waiting for a step to grow some.
     */
    private static int climb(int base, int step, int per, int cap) {
        if (base == 0) {
            return 0;
        }
        return (int) Math.min(cap, (long) base + (long) step * per);
    }

    public boolean hasMachines() {
        return this.tier.hasMachines();
    }

    public EntityType<? extends MachineEntity> roll(RandomSource random) {
        return this.tier.roll(random);
    }
}
