package com.kjmaster.memento.crafting;

import com.kjmaster.memento.api.MementoAPI;
import com.kjmaster.memento.component.TrackerMap;
import com.kjmaster.memento.data.StatTransferFilterManager;
import com.kjmaster.memento.item.MementoCrystalItem;
import com.kjmaster.memento.registry.ModDataComponents;
import com.kjmaster.memento.registry.ModRecipes;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class StatTransferRecipe implements CraftingRecipe {

    public StatTransferRecipe() {
    }

    /**
     * Helper record to identify valid inputs from the crafting grid.
     */
    private record TransferContext(ItemStack crystal, int crystalIdx, ItemStack tool, int toolIdx) {
        public boolean isValid() {
            return !crystal.isEmpty() && !tool.isEmpty();
        }
    }

    /**
     * Scans the grid to find a unique Memento Crystal and a unique Tool.
     * Returns an invalid context if duplicates or extra items are found.
     */
    private TransferContext findInputs(CraftingInput input) {
        ItemStack crystal = ItemStack.EMPTY;
        int crystalIdx = -1;
        ItemStack tool = ItemStack.EMPTY;
        int toolIdx = -1;
        int itemCount = 0;

        for (int i = 0; i < input.size(); i++) {
            ItemStack stack = input.getItem(i);
            if (stack.isEmpty()) continue;
            itemCount++;

            if (stack.getItem() instanceof MementoCrystalItem) {
                if (!crystal.isEmpty()) return new TransferContext(ItemStack.EMPTY, -1, ItemStack.EMPTY, -1); // Duplicate crystal
                crystal = stack;
                crystalIdx = i;
            } else if (stack.has(ModDataComponents.TRACKER_MAP) || stack.isDamageableItem()) {
                if (!tool.isEmpty()) return new TransferContext(ItemStack.EMPTY, -1, ItemStack.EMPTY, -1); // Duplicate tool
                tool = stack;
                toolIdx = i;
            } else {
                return new TransferContext(ItemStack.EMPTY, -1, ItemStack.EMPTY, -1); // Foreign item
            }
        }

        if (itemCount != 2 || crystal.isEmpty() || tool.isEmpty()) {
            return new TransferContext(ItemStack.EMPTY, -1, ItemStack.EMPTY, -1);
        }

        return new TransferContext(crystal, crystalIdx, tool, toolIdx);
    }

    /**
     * Holds the calculated output and the remaining items in the grid.
     */
    private record RecipeResult(ItemStack result, NonNullList<ItemStack> remaining) {}

    /**
     * Central logic for both matching and assembly.
     * Calculates what the result WOULD be, checking all filters and conditions.
     */
    private Optional<RecipeResult> calculateResult(CraftingInput input) {
        TransferContext ctx = findInputs(input);
        if (!ctx.isValid()) return Optional.empty();

        ItemStack crystal = ctx.crystal;
        ItemStack tool = ctx.tool;

        TrackerMap crystalStats = crystal.getOrDefault(ModDataComponents.TRACKER_MAP, TrackerMap.EMPTY);
        TrackerMap toolStats = tool.getOrDefault(ModDataComponents.TRACKER_MAP, TrackerMap.EMPTY);

        // Prepare remaining list (defaults to all empty)
        NonNullList<ItemStack> remaining = NonNullList.withSize(input.size(), ItemStack.EMPTY);

        // --- MODE 1: SIPHON (Tool -> Crystal) ---
        // Requirement: Crystal must be empty, Tool must have stats.
        if (crystalStats.stats().isEmpty() && !toolStats.stats().isEmpty()) {
            Map<ResourceLocation, Long> filteredStats = new HashMap<>();

            // Check filters: Only transfer allowed stats
            toolStats.stats().forEach((stat, val) -> {
                if (StatTransferFilterManager.isAllowed(tool, stat)) {
                    filteredStats.put(stat, val);
                }
            });

            // STRICT CHECK: If filters blocked everything, the recipe fails.
            if (filteredStats.isEmpty()) return Optional.empty();

            // 1. Output: Filled Crystal
            ItemStack resultCrystal = crystal.copy();
            resultCrystal.setCount(1);
            resultCrystal.set(ModDataComponents.TRACKER_MAP, new TrackerMap(filteredStats));

            // 2. Remaining: Wiped Tool (stays in grid)
            ItemStack wipedTool = tool.copy();
            wipedTool.setCount(1);
            wipedTool.remove(ModDataComponents.TRACKER_MAP);

            remaining.set(ctx.toolIdx, wipedTool);

            return Optional.of(new RecipeResult(resultCrystal, remaining));
        }

        // --- MODE 2: APPLY (Crystal -> Tool) ---
        // Requirement: Crystal must have stats.
        if (!crystalStats.stats().isEmpty()) {
            // STRICT CHECK: Ensure ALL stats in the crystal are allowed on the target tool.
            for (ResourceLocation stat : crystalStats.stats().keySet()) {
                if (!StatTransferFilterManager.isAllowed(tool, stat)) return Optional.empty();
            }

            // 1. Output: Enhanced Tool
            ItemStack resultTool = tool.copy();
            resultTool.setCount(1);

            // Merge logic using Central API
            resultTool.update(ModDataComponents.TRACKER_MAP, TrackerMap.EMPTY, currentMap ->
                    MementoAPI.mergeStats(currentMap, crystalStats)
            );

            // 2. Remaining: Empty Crystal (stays in grid)
            ItemStack emptyCrystal = crystal.copy();
            emptyCrystal.setCount(1);
            emptyCrystal.remove(ModDataComponents.TRACKER_MAP);

            remaining.set(ctx.crystalIdx, emptyCrystal);

            return Optional.of(new RecipeResult(resultTool, remaining));
        }

        return Optional.empty();
    }

    @Override
    public boolean matches(@NotNull CraftingInput input, @NotNull Level level) {
        // Strict matching: Only return true if the calculation succeeds (filters pass, valid context)
        return calculateResult(input).isPresent();
    }

    @Override
    public @NotNull ItemStack assemble(@NotNull CraftingInput input, HolderLookup.@NotNull Provider registries) {
        return calculateResult(input).map(RecipeResult::result).orElse(ItemStack.EMPTY);
    }

    @Override
    public @NotNull NonNullList<ItemStack> getRemainingItems(@NotNull CraftingInput input) {
        // Return the safe remaining items calculated during the logic pass
        return calculateResult(input).map(RecipeResult::remaining)
                .orElseGet(() -> NonNullList.withSize(input.size(), ItemStack.EMPTY));
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width * height >= 2;
    }

    @Override
    public @NotNull ItemStack getResultItem(HolderLookup.@NotNull Provider registries) {
        return ItemStack.EMPTY; // Dynamic result
    }

    @Override
    public @NotNull RecipeSerializer<?> getSerializer() {
        return ModRecipes.STAT_TRANSFER.get();
    }

    @Override
    public @NotNull RecipeType<?> getType() {
        return RecipeType.CRAFTING;
    }

    @Override
    public @NotNull CraftingBookCategory category() {
        return CraftingBookCategory.MISC;
    }

    public static class Serializer implements RecipeSerializer<StatTransferRecipe> {
        public static final MapCodec<StatTransferRecipe> CODEC = MapCodec.unit(StatTransferRecipe::new);
        public static final StreamCodec<RegistryFriendlyByteBuf, StatTransferRecipe> STREAM_CODEC = StreamCodec.unit(new StatTransferRecipe());

        @Override
        public @NotNull MapCodec<StatTransferRecipe> codec() {
            return CODEC;
        }

        @Override
        public @NotNull StreamCodec<RegistryFriendlyByteBuf, StatTransferRecipe> streamCodec() {
            return STREAM_CODEC;
        }
    }
}