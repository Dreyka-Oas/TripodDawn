package oas.dreyka.tripoddawn.client.render;

import oas.dreyka.tripoddawn.TripodDawnMod;

import net.minecraft.resources.Identifier;
import software.bernie.geckolib.animatable.GeoAnimatable;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.base.GeoRenderState;

/**
 * Where a creature's three files live.
 *
 * <p>GeckoLib 5 keeps its own index of everything under {@code geckolib/models} and
 * {@code geckolib/animations}, keyed by the bare name: the folder and the {@code .geo.json} or
 * {@code .animation.json} ending are stripped when the pack is read, so an id carrying either is
 * refused. Only the texture is an ordinary resource path.
 */
public class TripodDawnGeoModel<T extends GeoAnimatable> extends GeoModel<T> {

    private final Identifier model;
    private final Identifier texture;
    private final Identifier animations;

    public TripodDawnGeoModel(String name) {
        this.model = TripodDawnMod.id(name);
        this.texture = TripodDawnMod.id("textures/entity/" + name + ".png");
        this.animations = TripodDawnMod.id(name);
    }

    @Override
    public Identifier getModelResource(GeoRenderState state) {
        return this.model;
    }

    @Override
    public Identifier getTextureResource(GeoRenderState state) {
        return this.texture;
    }

    @Override
    public Identifier getAnimationResource(T animatable) {
        return this.animations;
    }
}
