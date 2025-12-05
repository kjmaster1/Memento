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

public class AddStatFunction extends LootItemConditionalFunction {

    public static final MapCodec<AddStatFunction> CODEC = RecordCodecBuilder.mapCodec(instance -> commonFields(instance).and(instance.group(
            ResourceLocation.CODEC.fieldOf("stat").forGetter(fn -> fn.stat),
            NumberProviders.CODEC.fieldOf("value").forGetter(fn -> fn.value)
    )).apply(instance, AddStatFunction::new));

    private final ResourceLocation stat;
    private final NumberProvider value;

    protected AddStatFunction(List<LootItemCondition> conditions, ResourceLocation stat, NumberProvider value) {
        super(conditions);
        this.stat = stat;
        this.value = value;
    }

    @Override
    protected @NotNull ItemStack run(@NotNull ItemStack stack, @NotNull LootContext context) {
        // 1. Calculate random value from the provider (Uniform, Constant, Binomial, etc.)
        long amount = value.getInt(context);
        if (amount == 0) return stack;

        // 2. Try to find a LivingEntity context to trigger events
        Entity entity = context.getParamOrNull(LootContextParams.THIS_ENTITY);

        if (entity instanceof LivingEntity living) {
            // Use API with SUM merge function
            MementoAPI.updateStat(living, stack, stat, amount, Long::sum);
        } else {
            // Fallback: Manual Silent Update (Additive)
            if (!stack.has(ModDataComponents.ITEM_UUID)) {
                stack.set(ModDataComponents.ITEM_UUID, UUID.randomUUID());
            }

            TrackerMap map = stack.getOrDefault(ModDataComponents.TRACKER_MAP, TrackerMap.EMPTY);
            // Use Long::sum here as well
            TrackerMap newMap = map.update(stat, amount, Long::sum);
            stack.set(ModDataComponents.TRACKER_MAP, newMap);
        }

        return stack;
    }

    @Override
    public @NotNull LootItemFunctionType<AddStatFunction> getType() {
        return ModLootFunctionTypes.ADD_STAT.get();
    }

    public static LootItemConditionalFunction.Builder<?> addStat(ResourceLocation stat, NumberProvider value) {
        return simpleBuilder(conditions -> new AddStatFunction(conditions, stat, value));
    }
}