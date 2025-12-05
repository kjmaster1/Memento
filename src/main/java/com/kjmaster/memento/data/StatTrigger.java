package com.kjmaster.memento.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.advancements.critereon.BlockPredicate;
import net.minecraft.advancements.critereon.EntityPredicate;
import net.minecraft.advancements.critereon.ItemPredicate;
import net.minecraft.advancements.critereon.LocationPredicate;
import net.minecraft.core.HolderSet;
import net.minecraft.core.RegistryCodecs;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;

import java.util.Optional;

public record StatTrigger(
        TriggerType type,
        ResourceLocation stat,
        long amount,
        Optional<ItemPredicate> item,
        Optional<ItemPredicate> subjectItem,
        Optional<BlockPredicate> block,
        Optional<EntityPredicate> target,
        Optional<HolderSet<Item>> items,
        Optional<LocationPredicate> location,
        Optional<String> weather
) {
    public static final Codec<StatTrigger> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            TriggerType.CODEC.fieldOf("type").forGetter(StatTrigger::type),
            ResourceLocation.CODEC.fieldOf("stat").forGetter(StatTrigger::stat),
            Codec.LONG.optionalFieldOf("amount", 1L).forGetter(StatTrigger::amount),
            ItemPredicate.CODEC.optionalFieldOf("item").forGetter(StatTrigger::item),
            ItemPredicate.CODEC.optionalFieldOf("subject_item").forGetter(StatTrigger::subjectItem),
            BlockPredicate.CODEC.optionalFieldOf("block").forGetter(StatTrigger::block),
            EntityPredicate.CODEC.optionalFieldOf("target").forGetter(StatTrigger::target),
            RegistryCodecs.homogeneousList(Registries.ITEM).optionalFieldOf("items").forGetter(StatTrigger::items),
            LocationPredicate.CODEC.optionalFieldOf("location").forGetter(StatTrigger::location),
            Codec.STRING.optionalFieldOf("weather").forGetter(StatTrigger::weather)
    ).apply(instance, StatTrigger::new));

    public enum TriggerType {
        BLOCK_BREAK, ENTITY_KILL, ITEM_USE, TOOL_MODIFICATION, BLOCK_PLACE;
        public static final Codec<TriggerType> CODEC = Codec.STRING.xmap(
                s -> TriggerType.valueOf(s.toUpperCase()), t -> t.name().toLowerCase());
    }
}