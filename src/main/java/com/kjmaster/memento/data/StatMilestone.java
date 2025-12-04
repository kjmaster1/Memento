package com.kjmaster.memento.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.advancements.critereon.ItemPredicate;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.Optional;

public record StatMilestone(
        ResourceLocation statId,
        long targetValue,
        Optional<ItemPredicate> itemRequirement, // Only triggers if item matches this
        Optional<Component> titleName,
        Optional<String> soundId,
        List<String> rewards,
        Optional<ItemStack> replacementItem,     // The item to transform into
        boolean keepStats                        // Should we copy the stats to the new item?
) {
    public static final Codec<StatMilestone> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ResourceLocation.CODEC.fieldOf("stat").forGetter(StatMilestone::statId),
            Codec.LONG.fieldOf("value").forGetter(StatMilestone::targetValue),
            ItemPredicate.CODEC.optionalFieldOf("item_requirement").forGetter(StatMilestone::itemRequirement),
            ComponentSerialization.CODEC.optionalFieldOf("title").forGetter(StatMilestone::titleName),
            Codec.STRING.optionalFieldOf("sound").forGetter(StatMilestone::soundId),
            Codec.STRING.listOf().optionalFieldOf("rewards", List.of()).forGetter(StatMilestone::rewards),
            ItemStack.CODEC.optionalFieldOf("transform_into").forGetter(StatMilestone::replacementItem),
            Codec.BOOL.optionalFieldOf("keep_stats", true).forGetter(StatMilestone::keepStats)
    ).apply(instance, StatMilestone::new));
}