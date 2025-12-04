package com.kjmaster.memento.component;

import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public record UnlockedSynergies(Set<ResourceLocation> unlocked) {
    public static final UnlockedSynergies EMPTY = new UnlockedSynergies(Set.of());

    public static final Codec<UnlockedSynergies> CODEC = ResourceLocation.CODEC.listOf()
            .xmap(list -> new UnlockedSynergies(new HashSet<>(list)), result -> List.copyOf(result.unlocked));

    public static final StreamCodec<ByteBuf, UnlockedSynergies> STREAM_CODEC = ResourceLocation.STREAM_CODEC
            .apply(ByteBufCodecs.list())
            .map(list -> new UnlockedSynergies(new HashSet<>(list)), result -> List.copyOf(result.unlocked));

    public boolean hasUnlocked(ResourceLocation synergyId) {
        return unlocked.contains(synergyId);
    }

    public UnlockedSynergies add(ResourceLocation synergyId) {
        if (unlocked.contains(synergyId)) return this;
        Set<ResourceLocation> newSet = new HashSet<>(this.unlocked);
        newSet.add(synergyId);
        return new UnlockedSynergies(newSet);
    }
}