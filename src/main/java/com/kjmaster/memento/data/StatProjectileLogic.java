package com.kjmaster.memento.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceLocation;

public record StatProjectileLogic(
        ResourceLocation stat,
        long minInfo,               // Threshold
        double velocityMultiplier,  // 1.0 = Normal, 1.5 = 50% Faster
        double inaccuracyModifier,  // Multiplier for spread (0.5 = Half Spread/More Accurate)
        double damageMultiplier     // 1.0 = Normal, 1.2 = 20% More Damage
) {
    public static final Codec<StatProjectileLogic> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ResourceLocation.CODEC.fieldOf("stat").forGetter(StatProjectileLogic::stat),
            Codec.LONG.fieldOf("min_value").forGetter(StatProjectileLogic::minInfo),
            Codec.DOUBLE.optionalFieldOf("velocity_multiplier", 1.0).forGetter(StatProjectileLogic::velocityMultiplier),
            Codec.DOUBLE.optionalFieldOf("inaccuracy_modifier", 1.0).forGetter(StatProjectileLogic::inaccuracyModifier),
            Codec.DOUBLE.optionalFieldOf("damage_multiplier", 1.0).forGetter(StatProjectileLogic::damageMultiplier)
    ).apply(instance, StatProjectileLogic::new));
}