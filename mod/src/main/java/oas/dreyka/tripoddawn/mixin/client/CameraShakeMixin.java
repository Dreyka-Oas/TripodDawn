package oas.dreyka.tripoddawn.mixin.client;

import oas.dreyka.tripoddawn.client.CameraShake;

import net.minecraft.client.Camera;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * The one thing the mod could not do without touching the game's own code.
 *
 * <p>Applied after the camera has settled rather than to the player's rotation, so the shake never
 * reaches the server and a player's aim comes back exactly where they left it.
 */
@Mixin(Camera.class)
public abstract class CameraShakeMixin {

    @Shadow
    private float xRot;

    @Shadow
    private float yRot;

    @Shadow
    protected abstract void setRotation(float yRot, float xRot);

    @Inject(method = "setup", at = @At("TAIL"))
    private void tripoddawn$shake(Level level, Entity viewer, boolean detached, boolean reversed,
                                  float partialTick, CallbackInfo info) {
        if (!CameraShake.active()) {
            return;
        }
        setRotation(this.yRot + CameraShake.yaw(viewer, partialTick),
                this.xRot + CameraShake.pitch(viewer, partialTick));
    }
}
