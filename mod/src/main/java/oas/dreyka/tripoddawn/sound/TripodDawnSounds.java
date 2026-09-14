package oas.dreyka.tripoddawn.sound;

import oas.dreyka.tripoddawn.TripodDawnMod;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;

/**
 * Every sound the mod plays. The three item sounds of the source mod are gone with the items, and
 * the silent placeholder it shipped had no caller to begin with.
 */
public final class TripodDawnSounds {
    private TripodDawnSounds() {
    }

    public static final SoundEvent MARTIAN_GROWL = register("entity.martian.growl");
    public static final SoundEvent MARTIAN_HURT = register("entity.martian.hurt");
    public static final SoundEvent MARTIAN_DEATH = register("entity.martian.death");

    public static final SoundEvent MACHINE_SPAWN = register("entity.machine.spawn");
    public static final SoundEvent MACHINE_DEATH = register("entity.machine.death");
    public static final SoundEvent MACHINE_HURT = register("entity.machine.hurt");
    public static final SoundEvent MACHINE_SHOOT = register("entity.machine.shoot");
    public static final SoundEvent MACHINE_GAS = register("entity.machine.gas");

    /** The engine loop, close and far. Played from the machine, never broadcast per tick. */
    public static final SoundEvent MACHINE_ENGINE = register("entity.machine.engine");
    public static final SoundEvent MACHINE_ENGINE_FAR = register("entity.machine.engine_far");

    /**
     * The ground: a leg landing, the same leg heard from far enough away that only the low end
     * arrives, the foot coming down on something, and the bed under a shake.
     *
     * <p>None of these came out of the source mod, which had nothing at all under its machines.
     * They are drawn by {@code scripts/steps.py}.
     */
    public static final SoundEvent MACHINE_STEP = register("entity.machine.step");
    public static final SoundEvent MACHINE_STEP_FAR = register("entity.machine.step_far");
    public static final SoundEvent MACHINE_STAMP = register("entity.machine.stamp");
    public static final SoundEvent MACHINE_RUMBLE = register("entity.machine.rumble");

    /** A shot skidding off plating, pitched by the zone it landed on. */
    public static final SoundEvent MACHINE_DEFLECT = register("entity.machine.deflect");

    public static final SoundEvent TRIPOD_HORN = register("entity.tripod.horn");
    public static final SoundEvent TRIPOD_HORN_BROKEN = register("entity.tripod.horn_broken");
    public static final SoundEvent UBERPOD_HORN = register("entity.uberpod.horn");
    public static final SoundEvent EMPERORPOD_HORN = register("entity.emperorpod.horn");

    public static final SoundEvent HEAT_RAY = register("entity.machine.heat_ray");

    /** The night-one warning: a horn from somewhere over the horizon, with nothing to see. */
    public static final SoundEvent DISTANT_HORN = register("ambient.distant_horn");

    private static SoundEvent register(String path) {
        Identifier id = TripodDawnMod.id(path);
        return Registry.register(BuiltInRegistries.SOUND_EVENT, id, SoundEvent.createVariableRangeEvent(id));
    }

    /** Touching the class runs its initialisers; this makes that intent readable. */
    public static void register() {
    }
}
