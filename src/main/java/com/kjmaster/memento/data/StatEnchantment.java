package com.kjmaster.memento.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceLocation;

public record StatEnchantment(
        ResourceLocation stat,
        ResourceLocation enchantment, // Registry Key for the enchantment
        int level,
        long minInfo,                 // Stat value required
        boolean removeIfBelow         // Should we remove the enchant if stat drops?
) {
    public static final Codec<StatEnchantment> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ResourceLocation.CODEC.fieldOf("stat").forGetter(StatEnchantment::stat),
            ResourceLocation.CODEC.fieldOf("enchantment").forGetter(StatEnchantment::enchantment),
            Codec.INT.fieldOf("level").forGetter(StatEnchantment::level),
            Codec.LONG.fieldOf("min_value").forGetter(StatEnchantment::minInfo),
            Codec.BOOL.optionalFieldOf("remove_if_below", false).forGetter(StatEnchantment::removeIfBelow)
    ).apply(instance, StatEnchantment::new));
}