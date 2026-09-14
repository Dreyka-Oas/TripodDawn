package oas.dreyka.tripoddawn.client;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;

/**
 * How far the view is off true while the ground is moving.
 *
 * <p>Two waves rather than one, on frequencies that do not divide into each other, because a single
 * sine reads as a machine rocking the camera on purpose. The amplitude is in degrees and stays small
 * enough to aim through: a shake that stops a player from fighting back is a punishment, not a scene.
 *
 * <p>The countdown lives here rather than on the player, because there is one camera and it belongs
 * to this client.
 */
public final class CameraShake {
    private CameraShake() {
    }

    private static final float YAW_DEGREES = 0.85f;
    private static final float PITCH_DEGREES = 0.55f;

    /** Ticks of taper at the end, so the ground settles rather than stopping on one frame. */
    private static final int RAMP = 25;

    private static int ticksLeft;

    /** A second machine coming up during the first one extends the shake, it does not restart it. */
    public static void start(int ticks) {
        ticksLeft = Math.max(ticksLeft, ticks);
    }

    public static void tick() {
        if (ticksLeft > 0) {
            ticksLeft--;
        }
    }

    /** Leaving the world drops it, otherwise the next world opens on a camera still moving. */
    public static void clear() {
        ticksLeft = 0;
    }

    /** Asked before the two offsets, since either of them legitimately crosses zero every cycle. */
    public static boolean active() {
        return ticksLeft > 0;
    }

    public static float yaw(Entity viewer, float partialTick) {
        return offset(viewer, partialTick, 0.83f, YAW_DEGREES);
    }

    public static float pitch(Entity viewer, float partialTick) {
        return offset(viewer, partialTick, 1.27f, PITCH_DEGREES);
    }

    private static float offset(Entity viewer, float partialTick, float speed, float degrees) {
        float strength = strength();
        if (strength <= 0.0f) {
            return 0.0f;
        }
        float time = viewer.tickCount + partialTick;
        return Mth.sin(time * speed) * degrees * strength;
    }

    /** Full while there is room left, fading over the last second and a bit. */
    private static float strength() {
        if (ticksLeft <= 0) {
            return 0.0f;
        }
        return Math.min(1.0f, Math.min(ticksLeft, RAMP) / (float) RAMP);
    }
}
