package com.kjmaster.memento.compat.kubejs;

import com.kjmaster.memento.api.MementoAPI;
import dev.latvian.mods.kubejs.util.ID;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

public class MementoKubeJSWrapper {

    /**
     * Gets the value of a stat for the given item.
     * Usage in JS: Memento.getStat(item, 'memento:entities_killed')
     */
    public static long getStat(ItemStack stack, Object statId) {
        return MementoAPI.getStat(stack, ID.mc(statId));
    }

    /**
     * Increments a stat on an item.
     * Usage in JS: Memento.incrementStat(player, item, 'memento:blocks_broken', 1)
     */
    public static void incrementStat(LivingEntity entity, ItemStack stack, Object statId, long amount) {
        MementoAPI.incrementStat(entity, stack, ID.mc(statId), amount);
    }

    public static void maximizeStat(LivingEntity entity, ItemStack stack, Object statId, long value) {
        MementoAPI.updateStat(entity, stack, ID.mc(statId), value, Math::max);
    }

    /**
     * Sets/Updates a stat to a specific value.
     * Usage in JS: Memento.setStat(player, item, 'memento:damage_taken', 500)
     */
    public static void setStat(LivingEntity entity, ItemStack stack, Object statId, long value) {
        // Uses the updateStat with an overwrite merge function
        MementoAPI.updateStat(entity, stack, ID.mc(statId), value, (old, newVal) -> newVal);
    }

    /**
     * Checks if a stat has been sealed on an item.
     */
    public static boolean isSealed(ItemStack stack, Object statId) {
        return MementoAPI.isSealed(stack, ID.mc(statId));
    }

    /**
     * Seals a stat on an item, preventing further changes.
     */
    public static void sealStat(ItemStack stack, Object statId) {
        MementoAPI.sealStat(stack, ID.mc(statId));
    }

    public static boolean hasUnlockedMilestone(ItemStack stack, Object statId, long milestoneValue) {
        return MementoAPI.hasUnlockedMilestone(stack, ID.mc(statId), milestoneValue);
    }

    public static boolean isMastered(ItemStack stack) {
        return MementoAPI.isMastered(stack);
    }
}