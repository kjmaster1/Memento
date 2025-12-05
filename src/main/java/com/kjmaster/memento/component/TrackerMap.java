package com.kjmaster.memento.component;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;

import java.util.*;
import java.util.function.BiFunction;

public record TrackerMap(Map<ResourceLocation, Long> stats, Map<ResourceLocation, Integer> counts, Set<ResourceLocation> sealed) {
    public static final TrackerMap EMPTY = new TrackerMap(Map.of(), Map.of(), Set.of());

    // Safety Cap to prevent packet overflow exploits or extreme bloat
    private static final int MAX_STATS = 128;

    // Codec for saving to disk
    public static final Codec<TrackerMap> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.unboundedMap(ResourceLocation.CODEC, Codec.LONG).fieldOf("stats").forGetter(TrackerMap::stats),
            Codec.unboundedMap(ResourceLocation.CODEC, Codec.INT).optionalFieldOf("counts", Map.of()).forGetter(TrackerMap::counts),
            ResourceLocation.CODEC.listOf().optionalFieldOf("sealed", List.of())
                    .xmap(list -> (Set<ResourceLocation>) new HashSet<>(list), ArrayList::new)
                    .forGetter(TrackerMap::sealed)
    ).apply(instance, TrackerMap::new));

    // StreamCodec for syncing to client
    public static final StreamCodec<ByteBuf, TrackerMap> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.map(HashMap::new, ResourceLocation.STREAM_CODEC, ByteBufCodecs.VAR_LONG), TrackerMap::stats,
            ByteBufCodecs.map(HashMap::new, ResourceLocation.STREAM_CODEC, ByteBufCodecs.VAR_INT), TrackerMap::counts,
            ByteBufCodecs.collection(HashSet::new, ResourceLocation.STREAM_CODEC)
                    .map(s -> s, HashSet::new), TrackerMap::sealed,
            TrackerMap::new
    );

    public TrackerMap(Map<ResourceLocation, Long> stats, Map<ResourceLocation, Integer> counts) {
        this(stats, counts, new HashSet<>());
    }

    public long getValue(ResourceLocation trackerId) {
        return stats.getOrDefault(trackerId, 0L);
    }

    public int getCount(ResourceLocation trackerId) {
        return counts.getOrDefault(trackerId, 0);
    }

    public boolean isSealed(ResourceLocation trackerId) {
        return sealed.contains(trackerId);
    }

    /**
     * Seals a stat, preventing further updates.
     */
    public TrackerMap seal(ResourceLocation trackerId) {
        if (sealed.contains(trackerId)) return this;
        Set<ResourceLocation> newSealed = new HashSet<>(this.sealed);
        newSealed.add(trackerId);
        return new TrackerMap(this.stats, this.counts, newSealed);
    }

    /**
     * Updates a stat using the provided merge function.
     * Increments the count for the stat by 1.
     */
    public TrackerMap update(ResourceLocation trackerId, long value, BiFunction<Long, Long, Long> remappingFunction) {
        // Optimization 0: Sealed Check
        if (sealed.contains(trackerId)) {
            return this;
        }

        long currentValue = stats.getOrDefault(trackerId, 0L);
        long newValue = remappingFunction.apply(currentValue, value);

        // Optimization 1: Pruning
        if (newValue == 0) {
            if (!stats.containsKey(trackerId)) return this;
            Map<ResourceLocation, Long> newStats = new HashMap<>(this.stats);
            Map<ResourceLocation, Integer> newCounts = new HashMap<>(this.counts);
            newStats.remove(trackerId);
            newCounts.remove(trackerId);
            return new TrackerMap(newStats, newCounts, this.sealed);
        }

        // Optimization 2: No-Op Check
        if (currentValue == newValue && stats.containsKey(trackerId)) {
            return this;
        }

        // Optimization 3: Hard Cap
        if (!stats.containsKey(trackerId) && stats.size() >= MAX_STATS) {
            return this;
        }

        Map<ResourceLocation, Long> newStats = new HashMap<>(this.stats);
        Map<ResourceLocation, Integer> newCounts = new HashMap<>(this.counts);

        newStats.put(trackerId, newValue);
        // Increment sample count (defaulting to 1 if new)
        newCounts.merge(trackerId, 1, Integer::sum);

        return new TrackerMap(newStats, newCounts, this.sealed);
    }
}