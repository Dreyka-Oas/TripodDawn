package oas.dreyka.tripoddawn;

import oas.dreyka.tripoddawn.command.TripodDawnCommands;
import oas.dreyka.tripoddawn.effect.TripodDawnEffects;
import oas.dreyka.tripoddawn.entity.TripodDawnEntities;
import oas.dreyka.tripoddawn.invasion.InvasionSpawner;
import oas.dreyka.tripoddawn.item.TripodDawnItems;
import oas.dreyka.tripoddawn.particle.TripodDawnParticles;
import oas.dreyka.tripoddawn.sound.TripodDawnSounds;

import net.fabricmc.api.ModInitializer;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TripodDawnMod implements ModInitializer {
    public static final String MOD_ID = "tripoddawn";
    public static final Logger LOGGER = LoggerFactory.getLogger("TripodDawn");

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }

    @Override
    public void onInitialize() {
        TripodDawnSounds.register();
        TripodDawnParticles.register();
        TripodDawnEffects.register();
        TripodDawnEntities.register();
        TripodDawnItems.register();
        InvasionSpawner.register();
        TripodDawnCommands.register();
        LOGGER.info("TripodDawn loaded");
    }
}
