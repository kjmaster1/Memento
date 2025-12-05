package com.kjmaster.memento.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceLocation;

import java.util.List;
import java.util.Optional;

public record StatProjectileLogic(
        ResourceLocation stat,
        long minInfo,
        double velocityMultiplier,
        double inaccuracyModifier,
        double damageMultiplier,
        Optional<List<ResourceLocation>> items // Renamed from optimizedItems
) {
    public static final Codec<StatProjectileLogic> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ResourceLocation.CODEC.fieldOf("stat").forGetter(StatProjectileLogic::stat),
            Codec.LONG.fieldOf("min_value").forGetter(StatProjectileLogic::minInfo),
            Codec.DOUBLE.optionalFieldOf("velocity_multiplier", 1.0).forGetter(StatProjectileLogic::velocityMultiplier),
            Codec.DOUBLE.optionalFieldOf("inaccuracy_modifier", 1.0).forGetter(StatProjectileLogic::inaccuracyModifier),
            Codec.DOUBLE.optionalFieldOf("damage_multiplier", 1.0).forGetter(StatProjectileLogic::damageMultiplier),
            ResourceLocation.CODEC.listOf().optionalFieldOf("items").forGetter(StatProjectileLogic::items)
    ).apply(instance, StatProjectileLogic::new));
}