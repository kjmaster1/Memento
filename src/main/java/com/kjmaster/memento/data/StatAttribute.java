package com.kjmaster.memento.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import org.jetbrains.annotations.NotNull;

public record StatAttribute(
        ResourceLocation stat,          // The stat to check
        Attribute attribute,            // The attribute to modify
        ResourceLocation modifierId,    // Unique ID for this modifier
        AttributeModifier.Operation operation,
        double valuePerStat,            // Coefficient (Multiplier)
        double maxBonus,                // Cap the bonus
        EquipmentSlotGroup slots,       // Mainhand, offhand, etc.
        ScalingFunction scalingFunction,// Linear, Log, Exp
        double exponent                 // Used for Exponential scaling (default 1.0)
) {
    public enum ScalingFunction implements StringRepresentable {
        LINEAR,
        LOGARITHMIC,
        EXPONENTIAL;

        public static final Codec<ScalingFunction> CODEC = StringRepresentable.fromEnum(ScalingFunction::values);

        @Override
        public @NotNull String getSerializedName() {
            return name().toLowerCase();
        }
    }

    public static final Codec<StatAttribute> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ResourceLocation.CODEC.fieldOf("stat").forGetter(StatAttribute::stat),
            BuiltInRegistries.ATTRIBUTE.byNameCodec().fieldOf("attribute").forGetter(StatAttribute::attribute),
            ResourceLocation.CODEC.fieldOf("modifier_id").forGetter(StatAttribute::modifierId),
            AttributeModifier.Operation.CODEC.fieldOf("operation").forGetter(StatAttribute::operation),
            Codec.DOUBLE.fieldOf("value_per_stat").forGetter(StatAttribute::valuePerStat),
            Codec.DOUBLE.optionalFieldOf("max_bonus", Double.MAX_VALUE).forGetter(StatAttribute::maxBonus),
            EquipmentSlotGroup.CODEC.optionalFieldOf("slots", EquipmentSlotGroup.ANY).forGetter(StatAttribute::slots),
            ScalingFunction.CODEC.optionalFieldOf("scaling_function", ScalingFunction.LINEAR).forGetter(StatAttribute::scalingFunction),
            Codec.DOUBLE.optionalFieldOf("exponent", 1.0).forGetter(StatAttribute::exponent)
    ).apply(instance, StatAttribute::new));
}