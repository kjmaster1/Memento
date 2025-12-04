package com.kjmaster.memento.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceLocation;

public record StatUsageSpeed(
        ResourceLocation stat,
        long minInfo,           // Stat value required
        double multiplier       // < 1.0 is faster, > 1.0 is slower
) {
    public static final Codec<StatUsageSpeed> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ResourceLocation.CODEC.fieldOf("stat").forGetter(StatUsageSpeed::stat),
            Codec.LONG.fieldOf("min_value").forGetter(StatUsageSpeed::minInfo),
            Codec.DOUBLE.fieldOf("multiplier").forGetter(StatUsageSpeed::multiplier)
    ).apply(instance, StatUsageSpeed::new));
}