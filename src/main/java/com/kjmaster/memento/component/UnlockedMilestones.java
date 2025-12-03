package com.kjmaster.memento.component;

import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

// A wrapper for the set of unlocked milestones (String IDs)
public record UnlockedMilestones(Set<String> milestones) {
    public static final UnlockedMilestones EMPTY = new UnlockedMilestones(Set.of());

    public static final Codec<UnlockedMilestones> CODEC = Codec.STRING.listOf()
            .xmap(list -> new UnlockedMilestones(new HashSet<>(list)), result -> List.copyOf(result.milestones));

    public static final StreamCodec<ByteBuf, UnlockedMilestones> STREAM_CODEC = ByteBufCodecs.STRING_UTF8
            .apply(ByteBufCodecs.list())
            .map(list -> new UnlockedMilestones(new HashSet<>(list)), result -> List.copyOf(result.milestones));

    public boolean hasUnlocked(String milestoneId) {
        return milestones.contains(milestoneId);
    }

    public UnlockedMilestones add(String milestoneId) {
        if (milestones.contains(milestoneId)) return this;
        Set<String> newSet = new HashSet<>(this.milestones);
        newSet.add(milestoneId);
        return new UnlockedMilestones(newSet);
    }
}
