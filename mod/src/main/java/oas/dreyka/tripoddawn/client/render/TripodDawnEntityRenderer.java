package oas.dreyka.tripoddawn.client.render;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.world.entity.LivingEntity;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

/**
 * The one renderer the five creatures share. Everything that differs between them is in the model
 * it is handed, so a new species costs a line in the client initialiser rather than a class.
 */
public class TripodDawnEntityRenderer<T extends LivingEntity & GeoEntity>
        extends GeoEntityRenderer<T, LivingEntityRenderState> {

    public TripodDawnEntityRenderer(EntityRendererProvider.Context context, GeoModel<T> model) {
        super(context, model);
    }
}
