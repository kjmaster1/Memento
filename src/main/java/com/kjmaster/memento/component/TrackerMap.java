package com.kjmaster.memento.component;

import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;
import java.util.Map;

public record TrackerMap(Map<ResourceLocation, Long> stats) {
    public static final TrackerMap EMPTY = new TrackerMap(Map.of());

    // Codec for saving to disk
    public static final Codec<TrackerMap> CODEC = Codec.unboundedMap(ResourceLocation.CODEC, Codec.LONG)
            .xmap(TrackerMap::new, TrackerMap::stats);

    // StreamCodec for syncing to client
    public static final StreamCodec<ByteBuf, TrackerMap> STREAM_CODEC = ByteBufCodecs.map(
            HashMap::new, // Factory creates a HashMap
            ResourceLocation.STREAM_CODEC,
            ByteBufCodecs.VAR_LONG
    ).map(
            TrackerMap::new,
            trackerMap -> new HashMap<>(trackerMap.stats())
    );

    public long getValue(ResourceLocation trackerId) {
        return stats.getOrDefault(trackerId, 0L);
    }

    public TrackerMap increment(ResourceLocation trackerId, long amount) {
        Map<ResourceLocation, Long> newStats = new HashMap<>(this.stats);
        newStats.merge(trackerId, amount, Long::sum);
        return new TrackerMap(newStats);
    }
}