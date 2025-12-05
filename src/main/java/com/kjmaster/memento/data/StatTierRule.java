package com.kjmaster.memento.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.advancements.critereon.ItemPredicate;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.Tiers;

import java.util.List;
import java.util.Optional;

public record StatTierRule(
        ItemPredicate item,
        ResourceLocation stat,
        long min,
        String tierName,
        Optional<List<ResourceLocation>> items // NEW: Optimization/Strict List
) {
    public static final Codec<StatTierRule> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ItemPredicate.CODEC.fieldOf("item").forGetter(StatTierRule::item),
            ResourceLocation.CODEC.fieldOf("stat").forGetter(StatTierRule::stat),
            Codec.LONG.fieldOf("threshold").forGetter(StatTierRule::min),
            Codec.STRING.fieldOf("tier_override").forGetter(StatTierRule::tierName),
            ResourceLocation.CODEC.listOf().optionalFieldOf("items").forGetter(StatTierRule::items)
    ).apply(instance, StatTierRule::new));

    public Tier resolveTier() {
        String name = tierName.replace("minecraft:", "").toUpperCase();
        try { return Tiers.valueOf(name); }
        catch (IllegalArgumentException e) { return Tiers.IRON; }
    }
}