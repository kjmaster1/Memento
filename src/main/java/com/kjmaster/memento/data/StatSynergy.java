package com.kjmaster.memento.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.resources.ResourceLocation;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public record StatSynergy(
        ResourceLocation id, // Unique ID for the synergy (from filename/json)
        Map<ResourceLocation, Long> requirements,
        Optional<Component> title,
        Optional<Component> description,
        Optional<String> sound,
        List<String> rewards // Commands
) {
    public static final Codec<StatSynergy> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ResourceLocation.CODEC.fieldOf("id").forGetter(StatSynergy::id),
            Codec.unboundedMap(ResourceLocation.CODEC, Codec.LONG).fieldOf("requirements").forGetter(StatSynergy::requirements),
            ComponentSerialization.CODEC.optionalFieldOf("title").forGetter(StatSynergy::title),
            ComponentSerialization.CODEC.optionalFieldOf("description").forGetter(StatSynergy::description),
            Codec.STRING.optionalFieldOf("sound").forGetter(StatSynergy::sound),
            Codec.STRING.listOf().optionalFieldOf("rewards", List.of()).forGetter(StatSynergy::rewards)
    ).apply(instance, StatSynergy::new));
}