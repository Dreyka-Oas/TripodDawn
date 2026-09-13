package oas.dreyka.tripoddawn.particle;

import oas.dreyka.tripoddawn.TripodDawnMod;

import net.fabricmc.fabric.api.particle.v1.FabricParticleTypes;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.server.level.ServerLevel;

/**
 * The four particles, and every one of them is actually emitted. The source mod shipped all four
 * with their textures and their json, and only ever spawned one.
 */
public final class TripodDawnParticles {
    private TripodDawnParticles() {
    }

    /** The impact bloom, at the far end of a heat ray. */
    public static final SimpleParticleType BLAST = register("blast");

    /** Along the beam, every tick of its flight. */
    public static final SimpleParticleType HEAT_RAY = register("heat_ray");

    /** At the muzzle, on the tick the shot leaves. */
    public static final SimpleParticleType HEAT_RAY_BRIGHT = register("heat_ray_bright");

    /** Thrown up around a machine for as long as it is climbing out of the ground. */
    public static final SimpleParticleType DIRT_CLOUD = register("dirt_cloud");

    /**
     * Sends a puff to everyone who could see it rather than to the thirty-two blocks the plain call
     * stops at. Nothing in this mod happens inside that radius: a machine climbs out fifty blocks
     * away and shoots from sixty, so the short reach would cut every effect the mod has.
     */
    public static void send(ServerLevel level, SimpleParticleType type, double x, double y, double z,
                            int count, double spreadX, double spreadY, double spreadZ, double speed) {
        level.sendParticles(type, true, false, x, y, z, count, spreadX, spreadY, spreadZ, speed);
    }

    private static SimpleParticleType register(String path) {
        return Registry.register(BuiltInRegistries.PARTICLE_TYPE, TripodDawnMod.id(path),
                FabricParticleTypes.simple());
    }

    public static void register() {
    }
}
