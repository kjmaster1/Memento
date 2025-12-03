package com.kjmaster.memento.milestone;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.resources.ResourceLocation;

import java.util.Optional;

public record Milestone(
        ResourceLocation statId,        // The stat to track (e.g., memento:blocks_broken)
        long targetValue,               // The value needed (e.g., 100)
        Optional<Component> titleName,  // The new name to give the item (optional)
        Optional<String> soundId        // Sound to play (optional)
) {
    public static final Codec<Milestone> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ResourceLocation.CODEC.fieldOf("stat").forGetter(Milestone::statId),
            Codec.LONG.fieldOf("value").forGetter(Milestone::targetValue),
            ComponentSerialization.CODEC.optionalFieldOf("title").forGetter(Milestone::titleName),
            Codec.STRING.optionalFieldOf("sound").forGetter(Milestone::soundId)
    ).apply(instance, Milestone::new));
}