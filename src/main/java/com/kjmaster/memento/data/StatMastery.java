package com.kjmaster.memento.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceLocation;

public record StatMastery(
        ResourceLocation stat,
        long value,             // The threshold required for mastery
        boolean preventDamage   // If true, the item stops taking durability damage
) {
    public static final Codec<StatMastery> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ResourceLocation.CODEC.fieldOf("stat").forGetter(StatMastery::stat),
            Codec.LONG.fieldOf("value").forGetter(StatMastery::value),
            Codec.BOOL.optionalFieldOf("prevent_damage", true).forGetter(StatMastery::preventDamage)
    ).apply(instance, StatMastery::new));
}