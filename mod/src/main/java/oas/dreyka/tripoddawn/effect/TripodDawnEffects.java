package oas.dreyka.tripoddawn.effect;

import oas.dreyka.tripoddawn.TripodDawnMod;

import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

/**
 * One effect, and it is the whole staging of the mod. It does nothing on the server: the client
 * reads it and shakes the camera, which is why it is hidden from the HUD and from the inventory.
 */
public final class TripodDawnEffects {
    private TripodDawnEffects() {
    }

    public static final Holder<MobEffect> EARTHQUAKE = Registry.registerForHolder(
            BuiltInRegistries.MOB_EFFECT,
            TripodDawnMod.id("earthquake"),
            new EarthquakeEffect());

    public static void register() {
    }

    private static final class EarthquakeEffect extends MobEffect {
        private EarthquakeEffect() {
            super(MobEffectCategory.HARMFUL, 0x6B3A24);
        }
    }
}
