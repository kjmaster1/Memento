package com.kjmaster.memento.crafting;

import com.kjmaster.memento.component.ItemMetadata;
import com.kjmaster.memento.component.TrackerMap;
import com.kjmaster.memento.component.UnlockedMilestones;
import com.kjmaster.memento.data.StatBehavior;
import com.kjmaster.memento.data.StatBehaviorManager;
import com.kjmaster.memento.registry.ModDataComponents;
import com.kjmaster.memento.registry.ModRecipes;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class PreserveStatsRecipe implements CraftingRecipe {
    private final CraftingRecipe delegate;

    public PreserveStatsRecipe(CraftingRecipe delegate) {
        this.delegate = delegate;
    }

    // --- Define the missing Codecs locally ---

    // 1. Create a Codec that ensures the recipe is a CraftingRecipe
    private static final Codec<CraftingRecipe> CRAFTING_RECIPE_CODEC = Recipe.CODEC.flatXmap(
            recipe -> recipe instanceof CraftingRecipe craftingRecipe
                    ? DataResult.success(craftingRecipe)
                    : DataResult.error(() -> "Recipe is not a crafting recipe: " + recipe),
            DataResult::success
    );

    // 2. Create a StreamCodec for networking
    // We cast safely because we trust the server only sends valid CraftingRecipes for this type
    private static final StreamCodec<RegistryFriendlyByteBuf, CraftingRecipe> CRAFTING_RECIPE_STREAM_CODEC =
            Recipe.STREAM_CODEC.map(
                    recipe -> (CraftingRecipe) recipe,
                    recipe -> (Recipe<?>) recipe
            ).cast();

    // ------------------------------------------

    @Override
    public boolean matches(@NotNull CraftingInput input, @NotNull Level level) {
        return delegate.matches(input, level);
    }

    @Override
    public @NotNull ItemStack assemble(@NotNull CraftingInput input, HolderLookup.@NotNull Provider registries) {
        ItemStack result = delegate.assemble(input, registries);

        // Mutable containers for merging
        Map<ResourceLocation, Long> mergedStats = new HashMap<>();
        Set<String> mergedMilestones = new HashSet<>();
        ItemMetadata primaryMeta = null;

        for (int i = 0; i < input.size(); i++) {
            ItemStack ingredient = input.getItem(i);

            // 1. Merge Stats
            if (ingredient.has(ModDataComponents.TRACKER_MAP)) {
                TrackerMap map = ingredient.get(ModDataComponents.TRACKER_MAP);
                if (map != null) {
                    for (Map.Entry<ResourceLocation, Long> entry : map.stats().entrySet()) {
                        ResourceLocation stat = entry.getKey();
                        long value = entry.getValue();

                        mergedStats.merge(stat, value, (oldVal, newVal) -> {
                            StatBehavior.MergeStrategy strategy = StatBehaviorManager.getStrategy(stat);
                            return switch (strategy) {
                                case MAX -> Math.max(oldVal, newVal);
                                case MIN -> Math.min(oldVal, newVal);
                                case SUM -> oldVal + newVal;
                            };
                        });
                    }
                }
            }

            // 2. Merge Milestones
            if (ingredient.has(ModDataComponents.MILESTONES)) {
                UnlockedMilestones milestones = ingredient.get(ModDataComponents.MILESTONES);
                if (milestones != null) {
                    mergedMilestones.addAll(milestones.milestones());
                }
            }

            // 3. Preserve Metadata (Keep from the first valid ingredient found)
            if (primaryMeta == null && ingredient.has(ModDataComponents.ITEM_METADATA)) {
                primaryMeta = ingredient.get(ModDataComponents.ITEM_METADATA);
            }
        }

        // Apply merged data to result
        if (!mergedStats.isEmpty()) {
            result.set(ModDataComponents.TRACKER_MAP, new TrackerMap(mergedStats));
        }

        if (!mergedMilestones.isEmpty()) {
            result.set(ModDataComponents.MILESTONES, new UnlockedMilestones(mergedMilestones));
        }

        if (primaryMeta != null) {
            result.set(ModDataComponents.ITEM_METADATA, primaryMeta);
        }

        return result;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return delegate.canCraftInDimensions(width, height);
    }

    @Override
    public @NotNull ItemStack getResultItem(HolderLookup.@NotNull Provider registries) {
        return delegate.getResultItem(registries);
    }

    @Override
    public @NotNull RecipeSerializer<?> getSerializer() {
        return ModRecipes.PRESERVE_STATS.get();
    }

    @Override
    public @NotNull RecipeType<?> getType() {
        return delegate.getType();
    }

    @Override
    public @NotNull CraftingBookCategory category() {
        return delegate.category();
    }

    public static class Serializer implements RecipeSerializer<PreserveStatsRecipe> {
        public static final MapCodec<PreserveStatsRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                CRAFTING_RECIPE_CODEC.fieldOf("recipe").forGetter(r -> r.delegate)
        ).apply(instance, PreserveStatsRecipe::new));

        public static final StreamCodec<RegistryFriendlyByteBuf, PreserveStatsRecipe> STREAM_CODEC = StreamCodec.composite(
                CRAFTING_RECIPE_STREAM_CODEC, r -> r.delegate,
                PreserveStatsRecipe::new
        );

        @Override
        public @NotNull MapCodec<PreserveStatsRecipe> codec() {
            return CODEC;
        }

        @Override
        public @NotNull StreamCodec<RegistryFriendlyByteBuf, PreserveStatsRecipe> streamCodec() {
            return STREAM_CODEC;
        }
    }
}