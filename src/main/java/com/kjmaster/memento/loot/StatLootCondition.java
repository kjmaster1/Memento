package com.kjmaster.memento.loot;

import com.kjmaster.memento.component.TrackerMap;
import com.kjmaster.memento.registry.ModDataComponents;
import com.kjmaster.memento.registry.ModLootConditionTypes;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.advancements.critereon.MinMaxBounds;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraft.world.level.storage.loot.predicates.LootItemConditionType;
import org.jetbrains.annotations.NotNull;

public record StatLootCondition(ResourceLocation stat, MinMaxBounds.Ints range) implements LootItemCondition {

    public static final MapCodec<StatLootCondition> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            ResourceLocation.CODEC.fieldOf("stat").forGetter(StatLootCondition::stat),
            MinMaxBounds.Ints.CODEC.fieldOf("range").forGetter(StatLootCondition::range)
    ).apply(instance, StatLootCondition::new));

    @Override
    public @NotNull LootItemConditionType getType() {
        return ModLootConditionTypes.STAT_CHECK.get();
    }

    @Override
    public boolean test(LootContext context) {
        ItemStack tool = context.getParamOrNull(LootContextParams.TOOL);

        long value = 0L;
        // If a tool is present, fetch the actual stat value.
        // If tool is null (e.g. empty hand, environmental damage), value remains 0.
        if (tool != null && !tool.isEmpty()) {
            TrackerMap trackers = tool.getOrDefault(ModDataComponents.TRACKER_MAP, TrackerMap.EMPTY);
            value = trackers.getValue(stat);
        }

        // Check if the value (0 or actual) lies within the required range.
        // This allows conditions like "max: 0" or "min: 0" to pass for non-tool kills.
        return range.matches((int) value);
    }

    // Helper builder for Java code usage
    public static LootItemCondition.Builder check(ResourceLocation stat, MinMaxBounds.Ints range) {
        return () -> new StatLootCondition(stat, range);
    }
}