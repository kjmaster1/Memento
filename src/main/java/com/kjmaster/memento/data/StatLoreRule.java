package com.kjmaster.memento.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.resources.ResourceLocation;

import java.util.List;
import java.util.Optional;

public record StatLoreRule(
        List<Condition> conditions,
        List<Component> loreLines,
        Optional<Component> namePrefix,
        Optional<Component> nameSuffix,
        int priority,
        boolean hidden,
        Optional<List<ResourceLocation>> items
) {
    public static final Codec<StatLoreRule> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Condition.CODEC.listOf().fieldOf("conditions").forGetter(StatLoreRule::conditions),
            ComponentSerialization.CODEC.listOf().fieldOf("lore_lines").forGetter(StatLoreRule::loreLines),
            ComponentSerialization.CODEC.optionalFieldOf("name_prefix").forGetter(StatLoreRule::namePrefix),
            ComponentSerialization.CODEC.optionalFieldOf("name_suffix").forGetter(StatLoreRule::nameSuffix),
            Codec.INT.optionalFieldOf("priority", 0).forGetter(StatLoreRule::priority),
            Codec.BOOL.optionalFieldOf("hidden", false).forGetter(StatLoreRule::hidden),
            ResourceLocation.CODEC.listOf().optionalFieldOf("items").forGetter(StatLoreRule::items)
    ).apply(instance, StatLoreRule::new));

    public record Condition(ResourceLocation stat, long min) {
        public static final Codec<Condition> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                ResourceLocation.CODEC.fieldOf("stat").forGetter(Condition::stat),
                Codec.LONG.fieldOf("min").forGetter(Condition::min)
        ).apply(instance, Condition::new));
    }
}