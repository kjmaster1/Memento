package com.kjmaster.memento.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.RegistryCodecs;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;

import java.util.Optional;

public record StatUsageSpeed(
        ResourceLocation stat,
        long minInfo,
        double multiplier,
        Optional<HolderSet<Item>> items
) {
    public static final Codec<StatUsageSpeed> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ResourceLocation.CODEC.fieldOf("stat").forGetter(StatUsageSpeed::stat),
            Codec.LONG.fieldOf("min_value").forGetter(StatUsageSpeed::minInfo),
            Codec.DOUBLE.fieldOf("multiplier").forGetter(StatUsageSpeed::multiplier),
            RegistryCodecs.homogeneousList(Registries.ITEM).optionalFieldOf("items").forGetter(StatUsageSpeed::items)
    ).apply(instance, StatUsageSpeed::new));
}