package com.kjmaster.memento.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Rarity;

import java.util.List;
import java.util.Optional;

public record StatVisualPrestige(
        ResourceLocation stat,
        long minInfo,
        Optional<Rarity> rarity, // The rarity to apply (e.g. EPIC)
        Optional<Boolean> glint, // true = force glint, false = no change
        Optional<List<ResourceLocation>> optimizedItems // Explicit list of items for O(1) lookup
) {
    public static final Codec<StatVisualPrestige> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ResourceLocation.CODEC.fieldOf("stat").forGetter(StatVisualPrestige::stat),
            Codec.LONG.fieldOf("min_value").forGetter(StatVisualPrestige::minInfo),
            Rarity.CODEC.optionalFieldOf("rarity").forGetter(StatVisualPrestige::rarity),
            Codec.BOOL.optionalFieldOf("glint").forGetter(StatVisualPrestige::glint),
            ResourceLocation.CODEC.listOf().optionalFieldOf("optimized_items").forGetter(StatVisualPrestige::optimizedItems)
    ).apply(instance, StatVisualPrestige::new));
}