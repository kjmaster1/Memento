package com.kjmaster.memento.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.advancements.critereon.ItemPredicate;
import net.minecraft.resources.ResourceLocation;

import java.util.List;
import java.util.Optional;

public record StatTransferFilter(
        ItemPredicate itemMatcher,               // Which items does this rule apply to?
        Optional<List<ResourceLocation>> allowedStats, // If present, ONLY these stats can be moved to/from this item
        Optional<List<ResourceLocation>> bannedStats   // If present, these stats CANNOT be moved
) {
    public static final Codec<StatTransferFilter> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ItemPredicate.CODEC.fieldOf("item").forGetter(StatTransferFilter::itemMatcher),
            ResourceLocation.CODEC.listOf().optionalFieldOf("allowed_stats").forGetter(StatTransferFilter::allowedStats),
            ResourceLocation.CODEC.listOf().optionalFieldOf("banned_stats").forGetter(StatTransferFilter::bannedStats)
    ).apply(instance, StatTransferFilter::new));
}