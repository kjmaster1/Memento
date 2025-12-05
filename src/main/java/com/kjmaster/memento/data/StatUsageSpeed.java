package com.kjmaster.memento.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceLocation;

import java.util.List;
import java.util.Optional;

public record StatUsageSpeed(
        ResourceLocation stat,
        long minInfo,
        double multiplier,
        Optional<List<ResourceLocation>> items
) {
    public static final Codec<StatUsageSpeed> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ResourceLocation.CODEC.fieldOf("stat").forGetter(StatUsageSpeed::stat),
            Codec.LONG.fieldOf("min_value").forGetter(StatUsageSpeed::minInfo),
            Codec.DOUBLE.fieldOf("multiplier").forGetter(StatUsageSpeed::multiplier),
            ResourceLocation.CODEC.listOf().optionalFieldOf("items").forGetter(StatUsageSpeed::items)
    ).apply(instance, StatUsageSpeed::new));
}