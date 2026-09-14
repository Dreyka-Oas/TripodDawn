package oas.dreyka.tripoddawn.client.render;

import oas.dreyka.tripoddawn.entity.MachineEntity;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.phys.AABB;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.base.GeoRenderState;

/**
 * A machine on the ground keeps its own colours.
 *
 * <p>The game tints anything dead in red and drops it a second later, so nobody sees the tint. A
 * wreck stays eight seconds, which is long enough for a red tripod to read as a bug.
 */
public class MachineEntityRenderer<T extends MachineEntity> extends TripodDawnEntityRenderer<T> {

    public MachineEntityRenderer(EntityRendererProvider.Context context, GeoModel<T> model) {
        super(context, model);
    }

    /**
     * What the game measures against the edge of the screen before it decides not to draw this.
     *
     * <p>It is the collision box by default, which here is the legs: a player standing under a
     * walker and looking up loses the whole machine, because the six blocks it collides with have
     * left the screen while the forty above them have not. Frustum culling only knows about the box
     * it is handed.
     *
     * <p>A quarter taller than the drawn height, because the model's crown stands above the number
     * everything else is measured against, and a leg's reach either side.
     */
    @Override
    protected AABB getBoundingBoxForCulling(T machine) {
        double tall = machine.drawnHeight();
        double reach = tall * 0.3;
        return new AABB(machine.getX() - reach, machine.getY() - 1.0, machine.getZ() - reach,
                machine.getX() + reach, machine.getY() + tall * 1.25, machine.getZ() + reach);
    }

    /** The fall is the machine's own animation, so the quarter turn the game adds would double it. */
    @Override
    protected float getDeathMaxRotation(GeoRenderState state) {
        return 0.0f;
    }

    @Override
    public int getPackedOverlay(T machine, Void relatedObject, float u, float partialTick) {
        return machine.fallen()
                ? OverlayTexture.NO_OVERLAY
                : super.getPackedOverlay(machine, relatedObject, u, partialTick);
    }
}
