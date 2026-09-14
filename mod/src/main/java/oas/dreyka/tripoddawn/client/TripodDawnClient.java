package oas.dreyka.tripoddawn.client;

import oas.dreyka.tripoddawn.client.particle.TripodDawnParticle;
import oas.dreyka.tripoddawn.client.render.MachineEntityRenderer;
import oas.dreyka.tripoddawn.client.render.MachineGeoModel;
import oas.dreyka.tripoddawn.client.render.TripodDawnEntityRenderer;
import oas.dreyka.tripoddawn.client.render.TripodDawnGeoModel;
import oas.dreyka.tripoddawn.entity.EmperorpodEntity;
import oas.dreyka.tripoddawn.entity.HarvesterEntity;
import oas.dreyka.tripoddawn.entity.MartianEntity;
import oas.dreyka.tripoddawn.entity.TitanEntity;
import oas.dreyka.tripoddawn.entity.TripodDawnEntities;
import oas.dreyka.tripoddawn.entity.TripodEntity;
import oas.dreyka.tripoddawn.entity.UberpodEntity;
import oas.dreyka.tripoddawn.net.ShakePayload;
import oas.dreyka.tripoddawn.particle.TripodDawnParticles;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.particle.v1.ParticleFactoryRegistry;
import net.minecraft.client.renderer.entity.EntityRenderers;
import net.minecraft.client.renderer.entity.NoopRenderer;

public class TripodDawnClient implements ClientModInitializer {

    /** Thrown up by a machine's rise: heavy, short-lived, and it lands. */
    private static final TripodDawnParticle.Settings DIRT =
            new TripodDawnParticle.Settings(0.18f, 0.9f, 1.6f, 10, 8, true, false);

    /**
     * The sleeve of the beam, wide and soft. It is laid down every half block of flight, so the
     * quads have to overlap: anything under a block across turns the ray back into a dotted line.
     */
    private static final TripodDawnParticle.Settings BEAM =
            new TripodDawnParticle.Settings(0.0f, 0.86f, 1.6f, 5, 5, false, true);

    /** The white core, inside the sleeve. Small, so the sleeve reads as a glow around something. */
    private static final TripodDawnParticle.Settings CORE =
            new TripodDawnParticle.Settings(0.0f, 0.78f, 0.55f, 4, 4, false, true);

    /** The impact bloom. It drifts upward the way heat does. */
    private static final TripodDawnParticle.Settings IMPACT =
            new TripodDawnParticle.Settings(-0.04f, 0.88f, 3.4f, 16, 12, false, true);

    @Override
    public void onInitializeClient() {
        EntityRenderers.register(TripodDawnEntities.MARTIAN, context ->
                new TripodDawnEntityRenderer<MartianEntity>(context, new TripodDawnGeoModel<>("martian")));
        EntityRenderers.register(TripodDawnEntities.TRIPOD, context ->
                new MachineEntityRenderer<TripodEntity>(context,
                        new MachineGeoModel<>("tripod", "dead_tripod")));
        EntityRenderers.register(TripodDawnEntities.HARVESTER, context ->
                new MachineEntityRenderer<HarvesterEntity>(context,
                        new MachineGeoModel<>("harvester", "dead_harvester")));
        EntityRenderers.register(TripodDawnEntities.UBERPOD, context ->
                new MachineEntityRenderer<UberpodEntity>(context,
                        new MachineGeoModel<>("uberpod", "dead_uberpod")));
        // The emperorpod came without a collapsed model, so it falls apart on its own and holds the
        // ground pose its animation file already carries.
        EntityRenderers.register(TripodDawnEntities.EMPERORPOD, context ->
                new MachineEntityRenderer<EmperorpodEntity>(context, new TripodDawnGeoModel<>("emperorpod")));
        // Cut from the walker, so it walks on the walker's clips and falls on the walker's wreck.
        EntityRenderers.register(TripodDawnEntities.TITAN, context ->
                new MachineEntityRenderer<TitanEntity>(context,
                        new MachineGeoModel<>("titan", "tripod", "dead_tripod")));

        // The beam itself is drawn by the particles it leaves; there is no model to put on it. The
        // slabs are drawn by the machine standing inside them.
        EntityRenderers.register(TripodDawnEntities.HEAT_RAY, NoopRenderer::new);
        EntityRenderers.register(TripodDawnEntities.MACHINE_PART, NoopRenderer::new);

        ParticleFactoryRegistry registry = ParticleFactoryRegistry.getInstance();
        registry.register(TripodDawnParticles.DIRT_CLOUD, sprites ->
                new TripodDawnParticle.Provider(sprites, DIRT));
        registry.register(TripodDawnParticles.HEAT_RAY, sprites ->
                new TripodDawnParticle.Provider(sprites, BEAM));
        registry.register(TripodDawnParticles.HEAT_RAY_BRIGHT, sprites ->
                new TripodDawnParticle.Provider(sprites, CORE));
        registry.register(TripodDawnParticles.BLAST, sprites ->
                new TripodDawnParticle.Provider(sprites, IMPACT));

        ClientPlayNetworking.registerGlobalReceiver(ShakePayload.TYPE,
                (payload, context) -> CameraShake.start(payload.ticks()));
        ClientTickEvents.END_CLIENT_TICK.register(client -> CameraShake.tick());
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> CameraShake.clear());
    }
}
