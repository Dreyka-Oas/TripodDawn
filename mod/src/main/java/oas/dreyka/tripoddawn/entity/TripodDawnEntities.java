package oas.dreyka.tripoddawn.entity;

import oas.dreyka.tripoddawn.TripodDawnMod;

import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;

/**
 * The six creatures, the projectile, and the slabs a machine is hit on.
 *
 * <p>The source mod registered eleven entity types for the same content: each machine carried a
 * separate species for its rise out of the ground and another for its wreck. Both are states of one
 * machine here, and the three tripod builds are one species too, so the registry stays short.
 */
public final class TripodDawnEntities {
    private TripodDawnEntities() {
    }

    public static final ResourceKey<EntityType<?>> MARTIAN_KEY = key("martian");
    public static final ResourceKey<EntityType<?>> TRIPOD_KEY = key("tripod");
    public static final ResourceKey<EntityType<?>> HARVESTER_KEY = key("harvester");
    public static final ResourceKey<EntityType<?>> UBERPOD_KEY = key("uberpod");
    public static final ResourceKey<EntityType<?>> EMPERORPOD_KEY = key("emperorpod");
    public static final ResourceKey<EntityType<?>> TITAN_KEY = key("titan");
    public static final ResourceKey<EntityType<?>> HEAT_RAY_KEY = key("heat_ray");
    public static final ResourceKey<EntityType<?>> MACHINE_PART_KEY = key("machine_part");

    public static final EntityType<MartianEntity> MARTIAN = Registry.register(
            BuiltInRegistries.ENTITY_TYPE,
            MARTIAN_KEY,
            EntityType.Builder.of(MartianEntity::new, MobCategory.MONSTER)
                    .sized(1.2f, 2.5f)
                    .clientTrackingRange(8)
                    .build(MARTIAN_KEY));

    // A walker is drawn forty blocks tall, so a player sees it long before the chunk it stands in is
    // anywhere near them. The tracking range is what decides whether it is there at all.
    //
    // The box is six, which is the legs and nothing above them. It used to be twenty-four, and that
    // was two problems in one. A shot already lands on the three slabs cut from the drawn height, so
    // the tall box was a fourth box sitting inside the first with nothing to do; and the cost of
    // finding a way anywhere is paid per block a walker fills, so those eighteen blocks of nothing
    // were most of the server time an invasion spent.
    public static final EntityType<TripodEntity> TRIPOD = Registry.register(
            BuiltInRegistries.ENTITY_TYPE,
            TRIPOD_KEY,
            EntityType.Builder.of(TripodEntity::new, MobCategory.MONSTER)
                    .sized(3.5f, 6.0f)
                    .clientTrackingRange(16)
                    .fireImmune()
                    .build(TRIPOD_KEY));

    public static final EntityType<HarvesterEntity> HARVESTER = Registry.register(
            BuiltInRegistries.ENTITY_TYPE,
            HARVESTER_KEY,
            EntityType.Builder.of(HarvesterEntity::new, MobCategory.MONSTER)
                    .sized(3.5f, 6.0f)
                    .clientTrackingRange(16)
                    .fireImmune()
                    .build(HARVESTER_KEY));

    public static final EntityType<UberpodEntity> UBERPOD = Registry.register(
            BuiltInRegistries.ENTITY_TYPE,
            UBERPOD_KEY,
            EntityType.Builder.of(UberpodEntity::new, MobCategory.MONSTER)
                    .sized(5.0f, 7.0f)
                    .clientTrackingRange(16)
                    .fireImmune()
                    .build(UBERPOD_KEY));

    public static final EntityType<EmperorpodEntity> EMPERORPOD = Registry.register(
            BuiltInRegistries.ENTITY_TYPE,
            EMPERORPOD_KEY,
            EntityType.Builder.of(EmperorpodEntity::new, MobCategory.MONSTER)
                    .sized(5.0f, 7.0f)
                    .clientTrackingRange(16)
                    .fireImmune()
                    .build(EMPERORPOD_KEY));

    /** Registered at the walker's own size and grown by its scale attribute, like the builds. */
    public static final EntityType<TitanEntity> TITAN = Registry.register(
            BuiltInRegistries.ENTITY_TYPE,
            TITAN_KEY,
            EntityType.Builder.of(TitanEntity::new, MobCategory.MONSTER)
                    .sized(3.5f, 6.0f)
                    .clientTrackingRange(16)
                    .fireImmune()
                    .build(TITAN_KEY));

    public static final EntityType<HeatRayProjectile> HEAT_RAY = Registry.register(
            BuiltInRegistries.ENTITY_TYPE,
            HEAT_RAY_KEY,
            EntityType.Builder.<HeatRayProjectile>of(HeatRayProjectile::new, MobCategory.MISC)
                    .sized(0.5f, 0.5f)
                    .clientTrackingRange(16)
                    .updateInterval(1)
                    .noSummon()
                    .fireImmune()
                    .build(HEAT_RAY_KEY));

    /**
     * The slabs a machine is hit on. Tracked like anything else, because a player aims on their own
     * client and a box nobody was told about cannot be shot at.
     *
     * <p>Tracked close, though. Forty machines' worth of slabs is what a row of them costs a client,
     * and the only thing a client does with one is put the crosshair on it, which happens within
     * arm's reach. An arrow is judged by the server, which knows about every slab at any distance.
     */
    public static final EntityType<MachinePart> MACHINE_PART = Registry.register(
            BuiltInRegistries.ENTITY_TYPE,
            MACHINE_PART_KEY,
            EntityType.Builder.<MachinePart>of(MachinePart::new, MobCategory.MISC)
                    .sized(1.0f, 1.0f)
                    .clientTrackingRange(4)
                    .noSummon()
                    .noSave()
                    .fireImmune()
                    .build(MACHINE_PART_KEY));

    public static void register() {
        FabricDefaultAttributeRegistry.register(MARTIAN, MartianEntity.createAttributes());
        FabricDefaultAttributeRegistry.register(TRIPOD, TripodEntity.createAttributes());
        FabricDefaultAttributeRegistry.register(HARVESTER, HarvesterEntity.createAttributes());
        FabricDefaultAttributeRegistry.register(UBERPOD, UberpodEntity.createAttributes());
        FabricDefaultAttributeRegistry.register(EMPERORPOD, EmperorpodEntity.createAttributes());
        FabricDefaultAttributeRegistry.register(TITAN, TitanEntity.createAttributes());
        TripodDawnMod.LOGGER.debug("entity types registered");
    }

    private static ResourceKey<EntityType<?>> key(String path) {
        return ResourceKey.create(Registries.ENTITY_TYPE, TripodDawnMod.id(path));
    }
}
