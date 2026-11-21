package com.pokehub.typewheel;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Sent to players when a wheel lands, so their screen can play the result animation. */
public record WheelResultPayload(int typeIndex, String spinner) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<WheelResultPayload> TYPE =
            new CustomPacketPayload.Type<>(TypeWheelMod.id("result"));
    public static final StreamCodec<RegistryFriendlyByteBuf, WheelResultPayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, WheelResultPayload::typeIndex,
            ByteBufCodecs.STRING_UTF8, WheelResultPayload::spinner,
            WheelResultPayload::new);

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
