package com.kjmaster.memento.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.RegistryCodecs;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;

import java.util.Optional;

public record StatProjectileLogic(
        ResourceLocation stat,
        long minInfo,
        double velocityMultiplier,
        double inaccuracyModifier,
        double damageMultiplier,
        Optional<HolderSet<Item>> items
) {
    public static final Codec<StatProjectileLogic> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ResourceLocation.CODEC.fieldOf("stat").forGetter(StatProjectileLogic::stat),
            Codec.LONG.fieldOf("min_value").forGetter(StatProjectileLogic::minInfo),
            Codec.DOUBLE.optionalFieldOf("velocity_multiplier", 1.0).forGetter(StatProjectileLogic::velocityMultiplier),
            Codec.DOUBLE.optionalFieldOf("inaccuracy_modifier", 1.0).forGetter(StatProjectileLogic::inaccuracyModifier),
            Codec.DOUBLE.optionalFieldOf("damage_multiplier", 1.0).forGetter(StatProjectileLogic::damageMultiplier),
            RegistryCodecs.homogeneousList(Registries.ITEM).optionalFieldOf("items").forGetter(StatProjectileLogic::items)
    ).apply(instance, StatProjectileLogic::new));
}