package oas.dreyka.tripoddawn.item;

import oas.dreyka.tripoddawn.TripodDawnMod;
import oas.dreyka.tripoddawn.entity.TripodDawnEntities;

import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.SpawnEggItem;

/**
 * Six eggs, and nothing else.
 *
 * <p>The mod has no materials, no recipes and no loot by design, so the only items it owns are the
 * ones a creative player needs to put a machine somewhere and look at it. They sit in the vanilla
 * spawn egg tab rather than in a tab of their own, which would hold six entries.
 *
 * <p>One egg for the three tripod builds, not three: which build comes out is the day's roll,
 * the same one a night makes, and a creative player who wants a particular one names it in the
 * summon command.
 */
public final class TripodDawnItems {
    private TripodDawnItems() {
    }

    public static final Item MARTIAN_SPAWN_EGG = egg("martian_spawn_egg", TripodDawnEntities.MARTIAN);
    public static final Item TRIPOD_SPAWN_EGG = egg("tripod_spawn_egg", TripodDawnEntities.TRIPOD);
    public static final Item HARVESTER_SPAWN_EGG = egg("harvester_spawn_egg", TripodDawnEntities.HARVESTER);
    public static final Item UBERPOD_SPAWN_EGG = egg("uberpod_spawn_egg", TripodDawnEntities.UBERPOD);
    public static final Item EMPERORPOD_SPAWN_EGG = egg("emperorpod_spawn_egg", TripodDawnEntities.EMPERORPOD);
    public static final Item TITAN_SPAWN_EGG = egg("titan_spawn_egg", TripodDawnEntities.TITAN);

    public static void register() {
        ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.SPAWN_EGGS).register(entries -> {
            entries.accept(MARTIAN_SPAWN_EGG);
            entries.accept(TRIPOD_SPAWN_EGG);
            entries.accept(HARVESTER_SPAWN_EGG);
            entries.accept(UBERPOD_SPAWN_EGG);
            entries.accept(EMPERORPOD_SPAWN_EGG);
            entries.accept(TITAN_SPAWN_EGG);
        });
    }

    private static Item egg(String path, EntityType<?> type) {
        ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, TripodDawnMod.id(path));
        return Registry.register(BuiltInRegistries.ITEM, key,
                new SpawnEggItem(new Item.Properties().spawnEgg(type).setId(key)));
    }
}
