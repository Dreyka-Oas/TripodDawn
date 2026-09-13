package oas.dreyka.tripoddawn.client.render;

import oas.dreyka.tripoddawn.TripodDawnMod;
import oas.dreyka.tripoddawn.entity.MachineEntity;

import net.minecraft.resources.Identifier;
import software.bernie.geckolib.constant.dataticket.DataTicket;
import software.bernie.geckolib.renderer.base.GeoRenderState;

/**
 * A machine standing and the same machine on the ground are two different models, and the second one
 * only exists once the fall is over.
 *
 * <p>The render state is built on the client from a snapshot rather than from the entity, so the
 * flag has to travel with it: reading the entity in {@code getModelResource} would be reading a
 * different thread's field.
 */
public class MachineGeoModel<T extends MachineEntity> extends TripodDawnGeoModel<T> {

    private static final DataTicket<Boolean> WRECK = DataTicket.create("tripoddawn_wreck", Boolean.class);

    /** The build a tripod is on, since one entity type covers three of them. */
    private static final DataTicket<String> BUILD = DataTicket.create("tripoddawn_build", String.class);

    private final Identifier wreckModel;
    private final Identifier wreckTexture;
    private final Identifier wreckAnimations;

    public MachineGeoModel(String name, String wreckName) {
        this(name, name, wreckName);
    }

    public MachineGeoModel(String name, String animationName, String wreckName) {
        super(name, animationName);
        this.wreckModel = TripodDawnMod.id(wreckName);
        this.wreckTexture = TripodDawnMod.id("textures/entity/" + wreckName + ".png");
        this.wreckAnimations = TripodDawnMod.id(wreckName);
    }

    @Override
    public void addAdditionalStateData(T machine, Object relatedObject, GeoRenderState state) {
        super.addAdditionalStateData(machine, relatedObject, state);
        state.addGeckolibData(WRECK, machine.wrecked());
        String build = machine.modelName();
        state.addGeckolibData(BUILD, build == null ? "" : build);
    }

    @Override
    public Identifier getModelResource(GeoRenderState state) {
        if (wrecked(state)) {
            return this.wreckModel;
        }
        String build = build(state);
        return build.isEmpty() ? super.getModelResource(state) : TripodDawnMod.id(build);
    }

    @Override
    public Identifier getTextureResource(GeoRenderState state) {
        if (wrecked(state)) {
            return this.wreckTexture;
        }
        String build = build(state);
        return build.isEmpty()
                ? super.getTextureResource(state)
                : TripodDawnMod.id("textures/entity/" + build + ".png");
    }

    /**
     * The animation file stays the machine's own whatever build it is on: the three tripods differ
     * by which bones they carry, not by how the bones they share move.
     */
    @Override
    public Identifier getAnimationResource(T machine) {
        return machine.wrecked() ? this.wreckAnimations : super.getAnimationResource(machine);
    }

    private static boolean wrecked(GeoRenderState state) {
        return state.getOrDefaultGeckolibData(WRECK, false);
    }

    private static String build(GeoRenderState state) {
        return state.getOrDefaultGeckolibData(BUILD, "");
    }
}
