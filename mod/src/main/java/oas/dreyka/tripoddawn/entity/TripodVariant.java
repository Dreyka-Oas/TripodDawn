package oas.dreyka.tripoddawn.entity;

import net.minecraft.util.RandomSource;

/**
 * The three walkers that come out of the same hole.
 *
 * <p>A night used to be one machine repeated, so a player who had beaten a tripod had beaten every
 * tripod they would ever meet. The three read apart at distance, which is the point: the small quick
 * one is a silhouette with no plating on it, the heavy one is a head taller than the line machine and
 * wears a second layer of it, and what you do about each is different.
 *
 * <p>One species and three of these rather than three species, because everything that differs is a
 * number, a model and a texture. Three registry entries would also mean three spawn eggs and three
 * names in the language files for one machine.
 */
public enum TripodVariant {

    /** Stripped of its plating and a sixth shorter. It arrives first and it arrives fast. */
    SCOUT("tripod_scout", 140.0, 8.0, 0.46, 0.85f, 22.0, 35),

    /** The machine the mod was built around. Everything else is measured against this one. */
    LINE("tripod", 200.0, 15.0, 0.35, 1.0f, 30.0, 50),

    /** Double plating and a fifth taller. It walks slowly because it does not have to hurry. */
    HEAVY("tripod_heavy", 320.0, 24.0, 0.26, 1.2f, 44.0, 80);

    private final String model;
    private final double health;
    private final double armour;
    private final double speed;
    private final float scale;
    private final double stamp;
    private final int experience;

    TripodVariant(String model, double health, double armour, double speed, float scale,
                  double stamp, int experience) {
        this.model = model;
        this.health = health;
        this.armour = armour;
        this.speed = speed;
        this.scale = scale;
        this.stamp = stamp;
        this.experience = experience;
    }

    /** The name its model, its animations and its texture are all filed under. */
    public String model() {
        return this.model;
    }

    public double health() {
        return this.health;
    }

    public double armour() {
        return this.armour;
    }

    public double speed() {
        return this.speed;
    }

    /** Height and width both, through the vanilla scale attribute, so the hit slabs follow it. */
    public float scale() {
        return this.scale;
    }

    public double stamp() {
        return this.stamp;
    }

    public int experience() {
        return this.experience;
    }

    public static TripodVariant byId(int id) {
        TripodVariant[] all = values();
        return id >= 0 && id < all.length ? all[id] : LINE;
    }

    /**
     * Which one the invasion sends on a given day.
     *
     * <p>Weighted rather than listed per rung, so the mix shifts instead of switching: a player on
     * day 25 still meets the odd scout, and the heavies that turn up on day 20 are the first thing
     * telling them the siege rung has opened.
     */
    public static TripodVariant roll(RandomSource random, long day) {
        if (day < 8) {
            return SCOUT;
        }
        if (day < 20) {
            return random.nextInt(3) == 0 ? LINE : SCOUT;
        }
        if (day < 40) {
            return switch (random.nextInt(4)) {
                case 0 -> SCOUT;
                case 1 -> HEAVY;
                default -> LINE;
            };
        }
        return random.nextInt(2) == 0 ? HEAVY : LINE;
    }
}
