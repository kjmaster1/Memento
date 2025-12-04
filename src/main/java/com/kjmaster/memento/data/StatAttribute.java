package com.kjmaster.memento.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;

public record StatAttribute(
        ResourceLocation stat,          // The stat to check (e.g. memento:entities_killed)
        Attribute attribute,            // The attribute to modify (e.g. generic.attack_damage)
        ResourceLocation modifierId,    // Unique ID for this modifier
        AttributeModifier.Operation operation,
        double valuePerStat,            // Multiplier: (statValue * valuePerStat)
        double maxBonus,                // Cap the bonus
        EquipmentSlotGroup slots        // Mainhand, offhand, etc.
) {
    public static final Codec<StatAttribute> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ResourceLocation.CODEC.fieldOf("stat").forGetter(StatAttribute::stat),
            BuiltInRegistries.ATTRIBUTE.byNameCodec().fieldOf("attribute").forGetter(StatAttribute::attribute),
            ResourceLocation.CODEC.fieldOf("modifier_id").forGetter(StatAttribute::modifierId),
            AttributeModifier.Operation.CODEC.fieldOf("operation").forGetter(StatAttribute::operation),
            Codec.DOUBLE.fieldOf("value_per_stat").forGetter(StatAttribute::valuePerStat),
            Codec.DOUBLE.optionalFieldOf("max_bonus", Double.MAX_VALUE).forGetter(StatAttribute::maxBonus),
            EquipmentSlotGroup.CODEC.optionalFieldOf("slots", EquipmentSlotGroup.ANY).forGetter(StatAttribute::slots)
    ).apply(instance, StatAttribute::new));
}