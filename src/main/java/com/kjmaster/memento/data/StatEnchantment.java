package com.kjmaster.memento.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.RegistryCodecs;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;

import java.util.Optional;

public record StatEnchantment(
        ResourceLocation stat,
        ResourceLocation enchantment,
        int level,
        long minInfo,
        boolean removeIfBelow,
        Optional<HolderSet<Item>> items
) {
    public static final Codec<StatEnchantment> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ResourceLocation.CODEC.fieldOf("stat").forGetter(StatEnchantment::stat),
            ResourceLocation.CODEC.fieldOf("enchantment").forGetter(StatEnchantment::enchantment),
            Codec.INT.fieldOf("level").forGetter(StatEnchantment::level),
            Codec.LONG.fieldOf("min_value").forGetter(StatEnchantment::minInfo),
            Codec.BOOL.optionalFieldOf("remove_if_below", false).forGetter(StatEnchantment::removeIfBelow),
            RegistryCodecs.homogeneousList(Registries.ITEM).optionalFieldOf("items").forGetter(StatEnchantment::items)
    ).apply(instance, StatEnchantment::new));
}