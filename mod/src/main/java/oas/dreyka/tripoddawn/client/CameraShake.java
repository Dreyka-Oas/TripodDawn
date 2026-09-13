package oas.dreyka.tripoddawn.client;

import oas.dreyka.tripoddawn.effect.TripodDawnEffects;

import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

/**
 * How far the view is off true while the ground is moving.
 *
 * <p>Two waves rather than one, on frequencies that do not divide into each other, because a single
 * sine reads as a machine rocking the camera on purpose. The amplitude is in degrees and stays small
 * enough to aim through: a shake that stops a player from fighting back is a punishment, not a scene.
 */
public final class CameraShake {
    private CameraShake() {
    }

    private static final float YAW_DEGREES = 0.85f;
    private static final float PITCH_DEGREES = 0.55f;

    /** Ticks of taper at the end, so the ground settles rather than stopping on one frame. */
    private static final int RAMP = 25;

    /** Asked before the two offsets, since either of them legitimately crosses zero every cycle. */
    public static boolean active(Entity viewer) {
        return strength(viewer) > 0.0f;
    }

    public static float yaw(Entity viewer, float partialTick) {
        return offset(viewer, partialTick, 0.83f, YAW_DEGREES);
    }

    public static float pitch(Entity viewer, float partialTick) {
        return offset(viewer, partialTick, 1.27f, PITCH_DEGREES);
    }

    private static float offset(Entity viewer, float partialTick, float speed, float degrees) {
        float strength = strength(viewer);
        if (strength <= 0.0f) {
            return 0.0f;
        }
        float time = viewer.tickCount + partialTick;
        return Mth.sin(time * speed) * degrees * strength;
    }

    /** Full while the effect has room left, fading over its last second and a bit. */
    private static float strength(Entity viewer) {
        if (!(viewer instanceof LivingEntity living)) {
            return 0.0f;
        }
        MobEffectInstance quake = living.getEffect(TripodDawnEffects.EARTHQUAKE);
        if (quake == null) {
            return 0.0f;
        }
        return Math.min(1.0f, Math.min(quake.getDuration(), RAMP) / (float) RAMP);
    }
}
