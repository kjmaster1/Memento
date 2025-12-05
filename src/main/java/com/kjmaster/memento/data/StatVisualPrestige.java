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
        Optional<Rarity> rarity,
        Optional<Boolean> glint,
        Optional<List<ResourceLocation>> items // Renamed from optimizedItems
) {
    public static final Codec<StatVisualPrestige> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ResourceLocation.CODEC.fieldOf("stat").forGetter(StatVisualPrestige::stat),
            Codec.LONG.fieldOf("min_value").forGetter(StatVisualPrestige::minInfo),
            Rarity.CODEC.optionalFieldOf("rarity").forGetter(StatVisualPrestige::rarity),
            Codec.BOOL.optionalFieldOf("glint").forGetter(StatVisualPrestige::glint),
            ResourceLocation.CODEC.listOf().optionalFieldOf("items").forGetter(StatVisualPrestige::items)
    ).apply(instance, StatVisualPrestige::new));
}