package com.kjmaster.memento.network;

import com.kjmaster.memento.Memento;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public record StatUpdatePayload(UUID itemUuid, ResourceLocation stat, long value) implements CustomPacketPayload {

    public static final Type<StatUpdatePayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Memento.MODID, "stat_update"));

    public static final StreamCodec<RegistryFriendlyByteBuf, StatUpdatePayload> STREAM_CODEC = StreamCodec.composite(
            UUIDUtil.STREAM_CODEC, StatUpdatePayload::itemUuid,
            ResourceLocation.STREAM_CODEC, StatUpdatePayload::stat,
            ByteBufCodecs.VAR_LONG, StatUpdatePayload::value,
            StatUpdatePayload::new
    );

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}