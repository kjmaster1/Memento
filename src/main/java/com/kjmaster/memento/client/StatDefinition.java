package com.kjmaster.memento.client;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.ChatFormatting;

import java.util.Optional;

public record StatDefinition(
        Optional<String> formatType,  // "decimal", "integer", "time"
        Optional<Double> factor,      // e.g., 0.01 to convert cm to m
        Optional<String> suffix,      // e.g., "m" or " HP"
        Optional<ChatFormatting> color, // e.g., "red", "gold"
        Optional<String> displayMode, // "text" (default) or "bar"
        Optional<Double> maxValue     // Required for bar: What is 100%?
) {
    public static final Codec<StatDefinition> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.optionalFieldOf("type").forGetter(StatDefinition::formatType),
            Codec.DOUBLE.optionalFieldOf("factor").forGetter(StatDefinition::factor),
            Codec.STRING.optionalFieldOf("suffix").forGetter(StatDefinition::suffix),
            ChatFormatting.CODEC.optionalFieldOf("color").forGetter(StatDefinition::color),
            Codec.STRING.optionalFieldOf("display_mode").forGetter(StatDefinition::displayMode),
            Codec.DOUBLE.optionalFieldOf("max_value").forGetter(StatDefinition::maxValue)
    ).apply(instance, StatDefinition::new));

    // Default definition
    public static final StatDefinition DEFAULT = new StatDefinition(
            Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(),
            Optional.empty(), Optional.empty()
    );
}