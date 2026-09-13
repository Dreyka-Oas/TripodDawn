package oas.dreyka.tripoddawn.client.render;

import oas.dreyka.tripoddawn.entity.MachineEntity;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.base.GeoRenderState;

/**
 * A machine on the ground keeps its own colours.
 *
 * <p>The game tints anything dead in red and drops it a second later, so nobody sees the tint. A
 * wreck stays for five minutes, which is long enough for a red tripod to read as a bug.
 */
public class MachineEntityRenderer<T extends MachineEntity> extends TripodDawnEntityRenderer<T> {

    public MachineEntityRenderer(EntityRendererProvider.Context context, GeoModel<T> model) {
        super(context, model);
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
