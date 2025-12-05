package com.kjmaster.memento.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.RegistryCodecs;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;

import java.util.Optional;

public record StatRepairCap(
        ResourceLocation stat,
        long minInfo,
        int cap,
        Optional<HolderSet<Item>> items
) {
    public static final Codec<StatRepairCap> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ResourceLocation.CODEC.fieldOf("stat").forGetter(StatRepairCap::stat),
            Codec.LONG.fieldOf("min_value").forGetter(StatRepairCap::minInfo),
            Codec.INT.optionalFieldOf("repair_cost_cap", 15).forGetter(StatRepairCap::cap),
            RegistryCodecs.homogeneousList(Registries.ITEM).optionalFieldOf("items").forGetter(StatRepairCap::items)
    ).apply(instance, StatRepairCap::new));
}