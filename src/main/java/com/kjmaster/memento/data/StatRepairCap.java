package com.kjmaster.memento.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceLocation;

public record StatRepairCap(
        ResourceLocation stat,
        long minInfo,           // Minimum stat value to trigger this cap
        int cap                 // The max repair cost allowed (e.g. 15 or 39)
) {
    public static final Codec<StatRepairCap> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ResourceLocation.CODEC.fieldOf("stat").forGetter(StatRepairCap::stat),
            Codec.LONG.fieldOf("min_value").forGetter(StatRepairCap::minInfo),
            Codec.INT.optionalFieldOf("repair_cost_cap", 15).forGetter(StatRepairCap::cap)
    ).apply(instance, StatRepairCap::new));
}