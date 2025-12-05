package com.kjmaster.memento.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.RegistryCodecs;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;

import java.util.Optional;

public record StatMastery(
        ResourceLocation stat,
        long value,
        boolean preventDamage,
        Optional<HolderSet<Item>> items
) {
    public static final Codec<StatMastery> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ResourceLocation.CODEC.fieldOf("stat").forGetter(StatMastery::stat),
            Codec.LONG.fieldOf("value").forGetter(StatMastery::value),
            Codec.BOOL.optionalFieldOf("prevent_damage", true).forGetter(StatMastery::preventDamage),
            RegistryCodecs.homogeneousList(Registries.ITEM).optionalFieldOf("items").forGetter(StatMastery::items)
    ).apply(instance, StatMastery::new));
}