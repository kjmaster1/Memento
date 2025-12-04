package com.kjmaster.memento.loot;

import com.kjmaster.memento.api.MementoAPI;
import com.kjmaster.memento.component.TrackerMap;
import com.kjmaster.memento.registry.ModDataComponents;
import com.kjmaster.memento.registry.ModLootFunctionTypes;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.functions.LootItemConditionalFunction;
import net.minecraft.world.level.storage.loot.functions.LootItemFunctionType;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraft.world.level.storage.loot.providers.number.NumberProvider;
import net.minecraft.world.level.storage.loot.providers.number.NumberProviders;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.UUID;

public class SetStatFunction extends LootItemConditionalFunction {

    public static final MapCodec<SetStatFunction> CODEC = RecordCodecBuilder.mapCodec(instance -> commonFields(instance).and(instance.group(
            ResourceLocation.CODEC.fieldOf("stat").forGetter(fn -> fn.stat),
            NumberProviders.CODEC.fieldOf("value").forGetter(fn -> fn.value)
    )).apply(instance, SetStatFunction::new));

    private final ResourceLocation stat;
    private final NumberProvider value;

    protected SetStatFunction(List<LootItemCondition> conditions, ResourceLocation stat, NumberProvider value) {
        super(conditions);
        this.stat = stat;
        this.value = value;
    }

    @Override
    protected @NotNull ItemStack run(@NotNull ItemStack stack, @NotNull LootContext context) {
        // 1. Calculate value
        long amount = value.getInt(context);
        if (amount <= 0) return stack;

        // 2. Try to find a LivingEntity context to trigger events
        // THIS_ENTITY is usually the actor (e.g., the player in `/loot give` or the mob in mob drops).
        Entity entity = context.getParamOrNull(LootContextParams.THIS_ENTITY);

        if (entity instanceof LivingEntity living) {
            // Use API: This handles UUID generation AND fires StatChangeEvent.
            // This ensures Milestones/Advancements trigger immediately if a player is the context.
            MementoAPI.updateStat(living, stack, stat, amount, (old, newVal) -> newVal);
        } else {
            // Fallback: Manual Silent Update
            // This runs for entity-less contexts (e.g., Dungeon Chest generation).

            // Ensure UUID (Hero Item Identity)
            if (!stack.has(ModDataComponents.ITEM_UUID)) {
                stack.set(ModDataComponents.ITEM_UUID, UUID.randomUUID());
            }

            // Apply Stat directly
            TrackerMap map = stack.getOrDefault(ModDataComponents.TRACKER_MAP, TrackerMap.EMPTY);
            TrackerMap newMap = map.update(stat, amount, (oldVal, newVal) -> newVal);
            stack.set(ModDataComponents.TRACKER_MAP, newMap);
        }

        return stack;
    }

    @Override
    public @NotNull LootItemFunctionType<SetStatFunction> getType() {
        return ModLootFunctionTypes.SET_STAT.get();
    }

    public static LootItemConditionalFunction.Builder<?> setStat(ResourceLocation stat, NumberProvider value) {
        return simpleBuilder(conditions -> new SetStatFunction(conditions, stat, value));
    }
}