package com.kjmaster.memento.api;

import com.kjmaster.memento.api.event.StatChangeEvent;
import com.kjmaster.memento.component.TrackerMap;
import com.kjmaster.memento.data.StatBehavior;
import com.kjmaster.memento.data.StatBehaviorManager;
import com.kjmaster.memento.data.StatMastery;
import com.kjmaster.memento.data.StatMasteryManager;
import com.kjmaster.memento.network.StatUpdatePayload;
import com.kjmaster.memento.registry.ModDataComponents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.BiFunction;

public class MementoAPI {

    private record RecursionKey(UUID stackUuid, ResourceLocation statId) {
    }

    private static final ThreadLocal<Set<RecursionKey>> RECURSION_GUARD = ThreadLocal.withInitial(HashSet::new);

    // --- Standard Methods (Default: fireEvents = true) ---

    public static void incrementStat(LivingEntity entity, ItemStack stack, ResourceLocation statId, long amount) {
        updateStat(entity, stack, statId, amount, Long::sum, true);
    }

    public static void maximizeStat(LivingEntity entity, ItemStack stack, ResourceLocation statId, long value) {
        updateStat(entity, stack, statId, value, Math::max, true);
    }

    public static void updateStat(LivingEntity entity, ItemStack stack, ResourceLocation statId, long value, BiFunction<Long, Long, Long> mergeFunction) {
        updateStat(entity, stack, statId, value, mergeFunction, true);
    }

    // --- Overloads with Control Flags ---

    public static void incrementStat(LivingEntity entity, ItemStack stack, ResourceLocation statId, long amount, boolean fireEvents) {
        updateStat(entity, stack, statId, amount, Long::sum, fireEvents);
    }

    public static void maximizeStat(LivingEntity entity, ItemStack stack, ResourceLocation statId, long value, boolean fireEvents) {
        updateStat(entity, stack, statId, value, Math::max, fireEvents);
    }

    /**
     * Updates a stat on the item.
     */
    public static void updateStat(LivingEntity entity, ItemStack stack, ResourceLocation statId, long value, BiFunction<Long, Long, Long> mergeFunction, boolean fireEvents) {
        if (stack.isEmpty() || stack.getMaxStackSize() > 1) return;

        // Check if Sealed (Optimization: prevents firing events for locked stats)
        if (isSealed(stack, statId)) return;

        // Ensure UUID exists for recursion guard identity, even if provider is external
        if (!stack.has(ModDataComponents.ITEM_UUID)) {
            stack.set(ModDataComponents.ITEM_UUID, UUID.randomUUID());
        }
        UUID itemUuid = stack.get(ModDataComponents.ITEM_UUID);

        // 1. GET (Delegated)
        IStatProvider provider = StatProviderRegistry.getProvider(stack, statId);
        long oldValue = provider.getStat(stack, statId);

        // 2. CALC
        long newValue = mergeFunction.apply(oldValue, value);

        if (newValue == oldValue) return;

        RecursionKey key = new RecursionKey(itemUuid, statId);
        boolean isRecursive = RECURSION_GUARD.get().contains(key);

        boolean shouldFire = fireEvents && !isRecursive;

        if (shouldFire) {
            RECURSION_GUARD.get().add(key);
        }

        try {
            // 3. Fire PRE Event (Cancellable)
            if (shouldFire) {
                StatChangeEvent.Pre preEvent = new StatChangeEvent.Pre(entity, stack, statId, oldValue, newValue);
                if (NeoForge.EVENT_BUS.post(preEvent).isCanceled()) {
                    return;
                }
            }

            // 4. SET (Delegated)
            // Note: We re-fetch provider in case the item state changed significantly, but usually it's the same
            provider.setStat(stack, statId, newValue);

            // 5. Fire POST Event (Logic Hooks)
            if (shouldFire) {
                NeoForge.EVENT_BUS.post(new StatChangeEvent.Post(entity, stack, statId, oldValue, newValue));
            }
        } finally {
            if (shouldFire) {
                RECURSION_GUARD.get().remove(key);
            }
        }
    }

    public static void sealStat(ItemStack stack, ResourceLocation statId) {
        if (stack.isEmpty()) return;

        // Currently, sealing is only supported on the native TrackerMap implementation.
        // Custom providers would need their own logic, which we can't enforce via IStatProvider yet.
        if (stack.has(ModDataComponents.TRACKER_MAP)) {
            stack.update(ModDataComponents.TRACKER_MAP, TrackerMap.EMPTY, map -> map.seal(statId));
        }
    }

    public static boolean isSealed(ItemStack stack, ResourceLocation statId) {
        if (stack.isEmpty()) return false;
        // Native support
        if (stack.has(ModDataComponents.TRACKER_MAP)) {
            return stack.get(ModDataComponents.TRACKER_MAP).isSealed(statId);
        }
        return false;
    }

    /**
     * Sends a lightweight packet to update a single stat on the client.
     * Useful for high-frequency updates (e.g., flight distance) where you want the visual feedback
     * immediately, but defer the server-side persistence (and full component sync) to a Buffer Manager.
     */
    public static void sendPartialUpdate(ServerPlayer player, ItemStack stack, ResourceLocation statId, long value) {
        if (stack.isEmpty() || !stack.has(ModDataComponents.ITEM_UUID)) return;

        PacketDistributor.sendToPlayer(player, new StatUpdatePayload(
                stack.get(ModDataComponents.ITEM_UUID),
                statId,
                value
        ));
    }

    /**
     * Merges two TrackerMaps according to the registered StatBehaviors (SUM, MAX, MIN, AVERAGE).
     * @param base The original map (e.g., the item being kept).
     * @param incoming The map being merged in (e.g., the item being sacrificed).
     * @return A new TrackerMap containing the combined stats.
     */
    public static TrackerMap mergeStats(TrackerMap base, TrackerMap incoming) {
        if (incoming.stats().isEmpty()) return base;

        Map<ResourceLocation, Long> newStats = new HashMap<>(base.stats());
        Map<ResourceLocation, Integer> newCounts = new HashMap<>(base.counts());
        Set<ResourceLocation> newSealed = new HashSet<>(base.sealed());

        // Merge sealed status (if it was sealed on EITHER item, it remains sealed)
        newSealed.addAll(incoming.sealed());

        for (Map.Entry<ResourceLocation, Long> entry : incoming.stats().entrySet()) {
            ResourceLocation stat = entry.getKey();

            // If the stat is sealed on the base item, we do NOT merge changes into it.
            if (base.isSealed(stat)) continue;

            long incomingValue = entry.getValue();
            long baseValue = base.getValue(stat);

            // Get Counts (Default to 1 if data exists but count is missing)
            int incomingCount = incoming.getCount(stat);
            if (incomingCount == 0 && incomingValue != 0) incomingCount = 1;

            int baseCount = base.getCount(stat);
            if (baseCount == 0 && baseValue != 0) baseCount = 1;

            StatBehavior.MergeStrategy strategy = StatBehaviorManager.getStrategy(stat);

            long finalValue;
            int finalCount = baseCount + incomingCount;

            switch (strategy) {
                case MAX -> finalValue = Math.max(baseValue, incomingValue);
                case MIN -> finalValue = (baseValue == 0) ? incomingValue : Math.min(baseValue, incomingValue);
                case AVERAGE -> {
                    if (finalCount == 0) {
                        finalValue = 0;
                    } else {
                        double total = (baseValue * baseCount) + (incomingValue * incomingCount);
                        finalValue = (long) (total / finalCount);
                    }
                }
                case SUM -> finalValue = baseValue + incomingValue;
                default -> finalValue = baseValue + incomingValue;
            }

            newStats.put(stat, finalValue);
            newCounts.put(stat, finalCount);
        }

        return new TrackerMap(newStats, newCounts, newSealed);
    }

    public static long getStat(ItemStack stack, ResourceLocation statId) {
        // Delegate to Registry
        return StatProviderRegistry.getProvider(stack, statId).getStat(stack, statId);
    }

    public static boolean hasUnlockedMilestone(ItemStack stack, ResourceLocation statId, long milestoneValue) {
        if (stack.isEmpty() || !stack.has(ModDataComponents.MILESTONES)) return false;
        String key = statId.toString() + "/" + milestoneValue;
        return stack.get(ModDataComponents.MILESTONES).hasUnlocked(key);
    }

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