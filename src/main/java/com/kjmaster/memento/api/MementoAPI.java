package com.kjmaster.memento.api;

import com.kjmaster.memento.api.event.StatChangeEvent;
import com.kjmaster.memento.component.TrackerMap;
import com.kjmaster.memento.data.StatMastery;
import com.kjmaster.memento.data.StatMasteryManager;
import com.kjmaster.memento.registry.ModDataComponents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.NeoForge;

import java.util.function.BiFunction;

public class MementoAPI {

    // --- Standard Methods (Default: fireEvents = true) ---

    public static void incrementStat(ServerPlayer player, ItemStack stack, ResourceLocation statId, long amount) {
        updateStat(player, stack, statId, amount, Long::sum, true);
    }

    public static void maximizeStat(ServerPlayer player, ItemStack stack, ResourceLocation statId, long value) {
        updateStat(player, stack, statId, value, Math::max, true);
    }

    public static void updateStat(ServerPlayer player, ItemStack stack, ResourceLocation statId, long value, BiFunction<Long, Long, Long> mergeFunction) {
        updateStat(player, stack, statId, value, mergeFunction, true);
    }

    // --- Overloads with Control Flags ---

    public static void incrementStat(ServerPlayer player, ItemStack stack, ResourceLocation statId, long amount, boolean fireEvents) {
        updateStat(player, stack, statId, amount, Long::sum, fireEvents);
    }

    public static void maximizeStat(ServerPlayer player, ItemStack stack, ResourceLocation statId, long value, boolean fireEvents) {
        updateStat(player, stack, statId, value, Math::max, fireEvents);
    }

    /**
     * Updates a stat on the item.
     * @param fireEvents If false, skips firing NeoForge events. WARNING: This will bypass Milestones and Logic hooks!
     */
    public static void updateStat(ServerPlayer player, ItemStack stack, ResourceLocation statId, long value, BiFunction<Long, Long, Long> mergeFunction, boolean fireEvents) {
        if (stack.isEmpty() || stack.getMaxStackSize() > 1) return;

        TrackerMap currentStats = stack.getOrDefault(ModDataComponents.TRACKER_MAP, TrackerMap.EMPTY);
        long oldValue = currentStats.getValue(statId);

        // 1. Calculate tentative new value (Dry Run) WITHOUT allocation [Optimization 3.1]
        // We manually apply the merge function to see if the value effectively changes
        long newValue = mergeFunction.apply(oldValue, value);

        if (newValue == oldValue) return; // No change, exit before allocating new objects

        // 2. Fire PRE Event (Cancellable)
        if (fireEvents) {
            StatChangeEvent.Pre preEvent = new StatChangeEvent.Pre(player, stack, statId, oldValue, newValue);
            if (NeoForge.EVENT_BUS.post(preEvent).isCanceled()) {
                return;
            }
        }

        // 3. Apply Change
        // We use a simple replacer function because we already calculated 'newValue'
        TrackerMap tentativeStats = currentStats.update(statId, newValue, (old, n) -> n);
        stack.set(ModDataComponents.TRACKER_MAP, tentativeStats);

        // 4. Fire POST Event (Logic Hooks)
        if (fireEvents) {
            NeoForge.EVENT_BUS.post(new StatChangeEvent.Post(player, stack, statId, oldValue, newValue));
        }
    }

    /**
     * Retrieves the current value of a stat. Returns 0 if not present.
     */
    public static long getStat(ItemStack stack, ResourceLocation statId) {
        if (stack.isEmpty() || !stack.has(ModDataComponents.TRACKER_MAP)) return 0L;

        TrackerMap trackers = stack.get(ModDataComponents.TRACKER_MAP);
        return trackers.getValue(statId);
    }

    /**
     * Checks if a specific milestone (by value) has been unlocked.
     */
    public static boolean hasUnlockedMilestone(ItemStack stack, ResourceLocation statId, long milestoneValue) {
        if (stack.isEmpty() || !stack.has(ModDataComponents.MILESTONES)) return false;

        String key = statId.toString() + "/" + milestoneValue;
        return stack.get(ModDataComponents.MILESTONES).hasUnlocked(key);
    }

    /**
     * Checks if the item has met any "Mastery" criteria that prevents damage.
     * Useful for client-side rendering (e.g. golden borders) or other logic.
     */
    public static boolean isMastered(ItemStack stack) {
        if (stack.isEmpty()) return false;

        for (StatMastery rule : StatMasteryManager.getAllRules()) {
            if (rule.preventDamage()) {
                long val = getStat(stack, rule.stat());
                if (val >= rule.value()) {
                    return true;
                }
            }
        }
        return false;
    }
}