package com.kjmaster.memento.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.advancements.critereon.BlockPredicate;
import net.minecraft.advancements.critereon.EntityPredicate;
import net.minecraft.advancements.critereon.ItemPredicate;
import net.minecraft.resources.ResourceLocation;

import java.util.List;
import java.util.Optional;

public record StatTrigger(
        TriggerType type,
        ResourceLocation stat,
        long amount,
        Optional<ItemPredicate> item,        // The item HOLDING the stat (Receiver)
        Optional<ItemPredicate> subjectItem, // The item BEING used/consumed (Subject) -- NEW
        Optional<BlockPredicate> block,      // The block being interacted with
        Optional<EntityPredicate> target,    // The entity being interacted with
        Optional<List<ResourceLocation>> optimizedItems // Explicit list of items for O(1) lookup
) {
    public static final Codec<StatTrigger> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            TriggerType.CODEC.fieldOf("type").forGetter(StatTrigger::type),
            ResourceLocation.CODEC.fieldOf("stat").forGetter(StatTrigger::stat),
            Codec.LONG.optionalFieldOf("amount", 1L).forGetter(StatTrigger::amount),
            ItemPredicate.CODEC.optionalFieldOf("item").forGetter(StatTrigger::item),
            ItemPredicate.CODEC.optionalFieldOf("subject_item").forGetter(StatTrigger::subjectItem),
            BlockPredicate.CODEC.optionalFieldOf("block").forGetter(StatTrigger::block),
            EntityPredicate.CODEC.optionalFieldOf("target").forGetter(StatTrigger::target),
            ResourceLocation.CODEC.listOf().optionalFieldOf("optimized_items").forGetter(StatTrigger::optimizedItems)
    ).apply(instance, StatTrigger::new));

    public enum TriggerType {
        BLOCK_BREAK,
        ENTITY_KILL,
        ITEM_USE,
        TOOL_MODIFICATION,
        BLOCK_PLACE;

        public static final Codec<TriggerType> CODEC = Codec.STRING.xmap(
                s -> TriggerType.valueOf(s.toUpperCase()),
                t -> t.name().toLowerCase()
        );
    }
}