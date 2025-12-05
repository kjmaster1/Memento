package com.kjmaster.memento.network;

import com.kjmaster.memento.Memento;
import com.kjmaster.memento.data.StatRegistry;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

public record StatRegistryPayload(Map<ResourceLocation, Integer> mapping) implements CustomPacketPayload {
    public static final Type<StatRegistryPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Memento.MODID, "stat_registry"));

    public static final StreamCodec<ByteBuf, StatRegistryPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.map(HashMap::new, ResourceLocation.STREAM_CODEC, ByteBufCodecs.VAR_INT),
            StatRegistryPayload::mapping,
            StatRegistryPayload::new
    );

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(final StatRegistryPayload payload, final net.neoforged.neoforge.network.handling.IPayloadContext context) {
        context.enqueueWork(() -> {
            StatRegistry.setMapping(payload.mapping());
            Memento.LOGGER.info("Received Stat Registry with {} entries", payload.mapping().size());
        });
    }
}