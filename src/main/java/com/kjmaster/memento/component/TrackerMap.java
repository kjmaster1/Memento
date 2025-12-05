package com.kjmaster.memento.component;

import com.kjmaster.memento.data.StatRegistry;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.function.BiFunction;

public record TrackerMap(Map<ResourceLocation, Long> stats, Map<ResourceLocation, Integer> counts, Set<ResourceLocation> sealed) {
    public static final TrackerMap EMPTY = new TrackerMap(Map.of(), Map.of(), Set.of());

    // Safety Cap to prevent packet overflow exploits or extreme bloat
    private static final int MAX_STATS = 128;

    // Codec for saving to disk (Always use full RL strings for persistence)
    public static final Codec<TrackerMap> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.unboundedMap(ResourceLocation.CODEC, Codec.LONG).fieldOf("stats").forGetter(TrackerMap::stats),
            Codec.unboundedMap(ResourceLocation.CODEC, Codec.INT).optionalFieldOf("counts", Map.of()).forGetter(TrackerMap::counts),
            ResourceLocation.CODEC.listOf().optionalFieldOf("sealed", List.of())
                    .xmap(list -> (Set<ResourceLocation>) new HashSet<>(list), ArrayList::new)
                    .forGetter(TrackerMap::sealed)
    ).apply(instance, TrackerMap::new));

    // Optimized StreamCodec for network (Uses Registry IDs where possible)
    public static final StreamCodec<ByteBuf, TrackerMap> STREAM_CODEC = StreamCodec.composite(
            // Stats Map: Optimized Key
            ByteBufCodecs.map(HashMap::new, new StreamCodec<>() {
                @Override
                public @NotNull ResourceLocation decode(@NotNull ByteBuf buffer) {
                    int id = ByteBufCodecs.VAR_INT.decode(buffer);
                    if (id == 0) {
                        return ResourceLocation.STREAM_CODEC.decode(buffer);
                    }
                    return StatRegistry.getStat(id - 1);
                }

                @Override
                public void encode(@NotNull ByteBuf buffer, @NotNull ResourceLocation value) {
                    int id = StatRegistry.getId(value);
                    if (id != -1) {
                        ByteBufCodecs.VAR_INT.encode(buffer, id + 1); // +1 because 0 is reserved
                    } else {
                        ByteBufCodecs.VAR_INT.encode(buffer, 0);
                        ResourceLocation.STREAM_CODEC.encode(buffer, value);
                    }
                }
            }, ByteBufCodecs.VAR_LONG), TrackerMap::stats,

            // Counts Map: Same Optimized Key
            ByteBufCodecs.map(HashMap::new, new StreamCodec<>() {
                @Override
                public @NotNull ResourceLocation decode(@NotNull ByteBuf buffer) {
                    int id = ByteBufCodecs.VAR_INT.decode(buffer);
                    if (id == 0) {
                        return ResourceLocation.STREAM_CODEC.decode(buffer);
                    }
                    return StatRegistry.getStat(id - 1);
                }

                @Override
                public void encode(@NotNull ByteBuf buffer, @NotNull ResourceLocation value) {
                    int id = StatRegistry.getId(value);
                    if (id != -1) {
                        ByteBufCodecs.VAR_INT.encode(buffer, id + 1);
                    } else {
                        ByteBufCodecs.VAR_INT.encode(buffer, 0);
                        ResourceLocation.STREAM_CODEC.encode(buffer, value);
                    }
                }
            }, ByteBufCodecs.VAR_INT), TrackerMap::counts,

            // Sealed Set: Same Optimized Key
            ByteBufCodecs.collection(HashSet::new, new StreamCodec<ByteBuf, ResourceLocation>() {
                @Override
                public @NotNull ResourceLocation decode(@NotNull ByteBuf buffer) {
                    int id = ByteBufCodecs.VAR_INT.decode(buffer);
                    if (id == 0) {
                        return ResourceLocation.STREAM_CODEC.decode(buffer);
                    }
                    return StatRegistry.getStat(id - 1);
                }

                @Override
                public void encode(@NotNull ByteBuf buffer, @NotNull ResourceLocation value) {
                    int id = StatRegistry.getId(value);
                    if (id != -1) {
                        ByteBufCodecs.VAR_INT.encode(buffer, id + 1);
                    } else {
                        ByteBufCodecs.VAR_INT.encode(buffer, 0);
                        ResourceLocation.STREAM_CODEC.encode(buffer, value);
                    }
                }
            }).map(s -> s, HashSet::new), TrackerMap::sealed,
            TrackerMap::new
    );

    public TrackerMap(Map<ResourceLocation, Long> stats) {
        this(stats, Map.of());
    }

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

    public TrackerMap seal(ResourceLocation trackerId) {
        if (sealed.contains(trackerId)) return this;
        Set<ResourceLocation> newSealed = new HashSet<>(this.sealed);
        newSealed.add(trackerId);
        return new TrackerMap(this.stats, this.counts, newSealed);
    }

    public TrackerMap update(ResourceLocation trackerId, long value, BiFunction<Long, Long, Long> remappingFunction) {
        if (sealed.contains(trackerId)) {
            return this;
        }

        long currentValue = stats.getOrDefault(trackerId, 0L);
        long newValue = remappingFunction.apply(currentValue, value);

        if (newValue == 0) {
            if (!stats.containsKey(trackerId)) return this;
            Map<ResourceLocation, Long> newStats = new HashMap<>(this.stats);
            Map<ResourceLocation, Integer> newCounts = new HashMap<>(this.counts);
            newStats.remove(trackerId);
            newCounts.remove(trackerId);
            return new TrackerMap(newStats, newCounts, this.sealed);
        }

        if (currentValue == newValue && stats.containsKey(trackerId)) {
            return this;
        }

        if (!stats.containsKey(trackerId) && stats.size() >= MAX_STATS) {
            return this;
        }

        Map<ResourceLocation, Long> newStats = new HashMap<>(this.stats);
        Map<ResourceLocation, Integer> newCounts = new HashMap<>(this.counts);

        newStats.put(trackerId, newValue);
        newCounts.merge(trackerId, 1, Integer::sum);

        return new TrackerMap(newStats, newCounts, this.sealed);
    }
}