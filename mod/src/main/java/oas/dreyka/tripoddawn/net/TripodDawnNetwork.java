package oas.dreyka.tripoddawn.net;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;

/** Declared on both sides at startup, otherwise the packet is dropped as an unknown channel. */
public final class TripodDawnNetwork {
    private TripodDawnNetwork() {
    }

    public static void register() {
        PayloadTypeRegistry.playS2C().register(ShakePayload.TYPE, ShakePayload.CODEC);
    }
}
