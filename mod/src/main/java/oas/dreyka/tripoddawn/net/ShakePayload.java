package oas.dreyka.tripoddawn.net;

import oas.dreyka.tripoddawn.TripodDawnMod;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * How long the ground keeps moving under one player, in ticks.
 *
 * <p>The camera is a view and not a condition, so this is a packet and not a potion: an effect put a
 * timer in the corner of the screen, a line in the inventory, and a cure in every bucket of milk.
 */
public record ShakePayload(int ticks) implements CustomPacketPayload {

    public static final Type<ShakePayload> TYPE = new Type<>(TripodDawnMod.id("shake"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ShakePayload> CODEC =
            StreamCodec.composite(ByteBufCodecs.VAR_INT, ShakePayload::ticks, ShakePayload::new);

    @Override
    public Type<ShakePayload> type() {
        return TYPE;
    }
}
