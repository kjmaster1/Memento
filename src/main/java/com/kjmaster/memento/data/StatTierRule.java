package com.kjmaster.memento.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.advancements.critereon.ItemPredicate;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.Tiers;

public record StatTierRule(
        ItemPredicate item,
        ResourceLocation stat,
        long min,
        String tierName // e.g., "minecraft:iron" or just "iron"
) {
    public static final Codec<StatTierRule> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ItemPredicate.CODEC.fieldOf("item").forGetter(StatTierRule::item),
            ResourceLocation.CODEC.fieldOf("stat").forGetter(StatTierRule::stat),
            Codec.LONG.fieldOf("threshold").forGetter(StatTierRule::min),
            Codec.STRING.fieldOf("tier_override").forGetter(StatTierRule::tierName)
    ).apply(instance, StatTierRule::new));

    /**
     * Resolves the String tier name to a Vanilla Tier enum.
     * Supports "wood", "stone", "iron", "diamond", "gold", "netherite".
     * Fallback to IRON if unknown.
     */
    public Tier resolveTier() {
        String name = tierName.replace("minecraft:", "").toUpperCase();
        try {
            return Tiers.valueOf(name);
        } catch (IllegalArgumentException e) {
            // If modded tiers are needed, they would need a custom registry or lookup mechanism here.
            // For now, we fallback to a safe Vanilla tier.
            return Tiers.IRON;
        }
    }
}