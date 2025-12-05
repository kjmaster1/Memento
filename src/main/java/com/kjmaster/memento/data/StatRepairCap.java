package com.kjmaster.memento.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceLocation;

import java.util.List;
import java.util.Optional;

public record StatRepairCap(
        ResourceLocation stat,
        long minInfo,
        int cap,
        Optional<List<ResourceLocation>> items
) {
    public static final Codec<StatRepairCap> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ResourceLocation.CODEC.fieldOf("stat").forGetter(StatRepairCap::stat),
            Codec.LONG.fieldOf("min_value").forGetter(StatRepairCap::minInfo),
            Codec.INT.optionalFieldOf("repair_cost_cap", 15).forGetter(StatRepairCap::cap),
            ResourceLocation.CODEC.listOf().optionalFieldOf("items").forGetter(StatRepairCap::items)
    ).apply(instance, StatRepairCap::new));
}