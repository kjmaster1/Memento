package com.kjmaster.memento.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.advancements.critereon.ItemPredicate;
import net.minecraft.core.HolderSet;
import net.minecraft.core.RegistryCodecs;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.Optional;

public record StatMilestone(
        ResourceLocation statId,
        long targetValue,
        Optional<ItemPredicate> itemRequirement,
        Optional<Component> titleName,
        Optional<String> soundId,
        List<String> rewards,
        Optional<ItemStack> replacementItem,
        boolean keepStats,
        Optional<HolderSet<Item>> items
) {
    public static final Codec<StatMilestone> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ResourceLocation.CODEC.fieldOf("stat").forGetter(StatMilestone::statId),
            Codec.LONG.fieldOf("value").forGetter(StatMilestone::targetValue),
            ItemPredicate.CODEC.optionalFieldOf("item_requirement").forGetter(StatMilestone::itemRequirement),
            ComponentSerialization.CODEC.optionalFieldOf("title").forGetter(StatMilestone::titleName),
            Codec.STRING.optionalFieldOf("sound").forGetter(StatMilestone::soundId),
            Codec.STRING.listOf().optionalFieldOf("rewards", List.of()).forGetter(StatMilestone::rewards),
            ItemStack.CODEC.optionalFieldOf("transform_into").forGetter(StatMilestone::replacementItem),
            Codec.BOOL.optionalFieldOf("keep_stats", true).forGetter(StatMilestone::keepStats),
            RegistryCodecs.homogeneousList(Registries.ITEM).optionalFieldOf("items").forGetter(StatMilestone::items)
    ).apply(instance, StatMilestone::new));
}