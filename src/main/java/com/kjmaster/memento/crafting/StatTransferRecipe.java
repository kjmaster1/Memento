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

public class StatTransferRecipe implements CraftingRecipe {

    public StatTransferRecipe() {
    }

    @Override
    public boolean matches(CraftingInput input, @NotNull Level level) {
        boolean hasCrystal = false;
        boolean hasTool = false;

        for (int i = 0; i < input.size(); i++) {
            ItemStack stack = input.getItem(i);
            if (stack.isEmpty()) continue;

            if (stack.getItem() instanceof MementoCrystalItem) {
                if (hasCrystal) return false;
                hasCrystal = true;
            } else if (stack.has(ModDataComponents.TRACKER_MAP) || stack.isDamageableItem()) {
                if (hasTool) return false;
                hasTool = true;
            } else {
                return false;
            }
        }
        return hasCrystal && hasTool;
    }

    @Override
    public @NotNull ItemStack assemble(CraftingInput input, HolderLookup.Provider registries) {
        ItemStack crystal = ItemStack.EMPTY;
        ItemStack tool = ItemStack.EMPTY;

        for (int i = 0; i < input.size(); i++) {
            ItemStack stack = input.getItem(i);
            if (stack.isEmpty()) continue;
            if (stack.getItem() instanceof MementoCrystalItem) crystal = stack;
            else tool = stack;
        }

        if (crystal.isEmpty() || tool.isEmpty()) return ItemStack.EMPTY;

        TrackerMap crystalStats = crystal.getOrDefault(ModDataComponents.TRACKER_MAP, TrackerMap.EMPTY);
        TrackerMap toolStats = tool.getOrDefault(ModDataComponents.TRACKER_MAP, TrackerMap.EMPTY);

        // MODE 1: SIPHON (Tool -> Crystal)
        if (crystalStats.stats().isEmpty() && !toolStats.stats().isEmpty()) {
            ItemStack resultCrystal = crystal.copy();
            Map<ResourceLocation, Long> filteredStats = new HashMap<>();

            ItemStack finalTool = tool;
            toolStats.stats().forEach((stat, val) -> {
                if (StatTransferFilterManager.isAllowed(finalTool, stat)) {
                    filteredStats.put(stat, val);
                }
            });

            if (filteredStats.isEmpty()) return ItemStack.EMPTY;

            resultCrystal.set(ModDataComponents.TRACKER_MAP, new TrackerMap(filteredStats));
            return resultCrystal;
        }

        // MODE 2: APPLY (Crystal -> Tool)
        if (!crystalStats.stats().isEmpty()) {
            ItemStack resultTool = tool.copy();

            // Check filters against DESTINATION tool
            for (ResourceLocation stat : crystalStats.stats().keySet()) {
                if (!StatTransferFilterManager.isAllowed(resultTool, stat)) return ItemStack.EMPTY;
            }

            // Merge logic using Central API
            resultTool.update(ModDataComponents.TRACKER_MAP, TrackerMap.EMPTY, currentMap ->
                    MementoAPI.mergeStats(currentMap, crystalStats)
            );

            return resultTool;
        }

        return ItemStack.EMPTY;
    }

    @Override
    public @NotNull NonNullList<ItemStack> getRemainingItems(CraftingInput input) {
        NonNullList<ItemStack> remaining = NonNullList.withSize(input.size(), ItemStack.EMPTY);

        ItemStack crystal = ItemStack.EMPTY;
        ItemStack tool = ItemStack.EMPTY;
        int crystalIdx = -1;
        int toolIdx = -1;

        for (int i = 0; i < input.size(); i++) {
            ItemStack stack = input.getItem(i);
            if (stack.isEmpty()) continue;
            if (stack.getItem() instanceof MementoCrystalItem) {
                crystal = stack;
                crystalIdx = i;
            } else {
                tool = stack;
                toolIdx = i;
            }
        }

        TrackerMap crystalStats = crystal.getOrDefault(ModDataComponents.TRACKER_MAP, TrackerMap.EMPTY);
        TrackerMap toolStats = tool.getOrDefault(ModDataComponents.TRACKER_MAP, TrackerMap.EMPTY);

        // SIPHON: Tool -> Crystal. Result is Crystal. Tool stays (wiped).
        if (crystalStats.stats().isEmpty() && !toolStats.stats().isEmpty()) {
            ItemStack wipedTool = tool.copy();
            wipedTool.remove(ModDataComponents.TRACKER_MAP);
            wipedTool.setCount(1);
            remaining.set(toolIdx, wipedTool);
        }
        // APPLY: Crystal -> Tool. Result is Tool. Crystal stays (empty).
        else if (!crystalStats.stats().isEmpty()) {
            ItemStack emptyCrystal = crystal.copy();
            emptyCrystal.remove(ModDataComponents.TRACKER_MAP);
            emptyCrystal.setCount(1);
            remaining.set(crystalIdx, emptyCrystal);
        }

        return remaining;
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

    // Serializer Boilerplate
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