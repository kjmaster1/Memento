package com.kjmaster.memento.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceLocation;

import java.util.List;
import java.util.Optional;

public record StatEnchantment(
        ResourceLocation stat,
        ResourceLocation enchantment,
        int level,
        long minInfo,
        boolean removeIfBelow,
        Optional<List<ResourceLocation>> items
) {
    public static final Codec<StatEnchantment> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ResourceLocation.CODEC.fieldOf("stat").forGetter(StatEnchantment::stat),
            ResourceLocation.CODEC.fieldOf("enchantment").forGetter(StatEnchantment::enchantment),
            Codec.INT.fieldOf("level").forGetter(StatEnchantment::level),
            Codec.LONG.fieldOf("min_value").forGetter(StatEnchantment::minInfo),
            Codec.BOOL.optionalFieldOf("remove_if_below", false).forGetter(StatEnchantment::removeIfBelow),
            ResourceLocation.CODEC.listOf().optionalFieldOf("items").forGetter(StatEnchantment::items)
    ).apply(instance, StatEnchantment::new));
}