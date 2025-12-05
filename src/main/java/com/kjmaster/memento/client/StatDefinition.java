package com.kjmaster.memento.client;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.resources.ResourceLocation;

import java.util.Optional;

public record StatDefinition(
        Optional<String> formatType,  // "decimal", "integer", "time"
        Optional<Double> factor,      // e.g., 0.01 to convert cm to m
        Optional<String> suffix,      // e.g., "m" or " HP"
        Optional<ChatFormatting> color, // e.g., "red", "gold"
        Optional<String> displayMode, // "text" (default) or "bar"
        Optional<Double> maxValue,    // Required for bar: What is 100%?
        Optional<ResourceLocation> icon // The item ID to use as an icon in Compact Mode
) {
    public static final Codec<StatDefinition> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.optionalFieldOf("type").forGetter(StatDefinition::formatType),
            Codec.DOUBLE.optionalFieldOf("factor").forGetter(StatDefinition::factor),
            Codec.STRING.optionalFieldOf("suffix").forGetter(StatDefinition::suffix),
            ChatFormatting.CODEC.optionalFieldOf("color").forGetter(StatDefinition::color),
            Codec.STRING.optionalFieldOf("display_mode").forGetter(StatDefinition::displayMode),
            Codec.DOUBLE.optionalFieldOf("max_value").forGetter(StatDefinition::maxValue),
            ResourceLocation.CODEC.optionalFieldOf("icon").forGetter(StatDefinition::icon)
    ).apply(instance, StatDefinition::new));

    // Default definition
    public static final StatDefinition DEFAULT = new StatDefinition(
            Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(),
            Optional.empty(), Optional.empty(), Optional.empty()
    );
}