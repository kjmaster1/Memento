package com.kjmaster.memento.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.advancements.critereon.ItemPredicate;
import net.minecraft.core.HolderSet;
import net.minecraft.core.RegistryCodecs;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;

import java.util.List;
import java.util.Optional;

public record StatTransferFilter(
        ItemPredicate itemMatcher,
        Optional<List<ResourceLocation>> allowedStats,
        Optional<List<ResourceLocation>> bannedStats,
        Optional<HolderSet<Item>> items
) {
    public static final Codec<StatTransferFilter> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ItemPredicate.CODEC.fieldOf("item").forGetter(StatTransferFilter::itemMatcher),
            ResourceLocation.CODEC.listOf().optionalFieldOf("allowed_stats").forGetter(StatTransferFilter::allowedStats),
            ResourceLocation.CODEC.listOf().optionalFieldOf("banned_stats").forGetter(StatTransferFilter::bannedStats),
            RegistryCodecs.homogeneousList(Registries.ITEM).optionalFieldOf("items").forGetter(StatTransferFilter::items)
    ).apply(instance, StatTransferFilter::new));
}