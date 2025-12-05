package com.kjmaster.memento.api;

import com.kjmaster.memento.api.event.StatChangeEvent;
import com.kjmaster.memento.component.ItemMetadata;
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

import java.util.*;
import java.util.function.BiFunction;

public class MementoAPI {

    private record RecursionKey(UUID stackUuid, ResourceLocation statId) {
    }

    private static final ThreadLocal<Set<RecursionKey>> RECURSION_GUARD = ThreadLocal.withInitial(HashSet::new);

    public static void incrementStat(LivingEntity entity, ItemStack stack, ResourceLocation statId, long amount) {
        updateStat(entity, stack, statId, amount, Long::sum, true);
    }

    public static void maximizeStat(LivingEntity entity, ItemStack stack, ResourceLocation statId, long value) {
        updateStat(entity, stack, statId, value, Math::max, true);
    }

    public static void updateStat(LivingEntity entity, ItemStack stack, ResourceLocation statId, long value, BiFunction<Long, Long, Long> mergeFunction) {
        updateStat(entity, stack, statId, value, mergeFunction, true);
    }

    public static void incrementStat(LivingEntity entity, ItemStack stack, ResourceLocation statId, long amount, boolean fireEvents) {
        updateStat(entity, stack, statId, amount, Long::sum, fireEvents);
    }

    public static void maximizeStat(LivingEntity entity, ItemStack stack, ResourceLocation statId, long value, boolean fireEvents) {
        updateStat(entity, stack, statId, value, Math::max, fireEvents);
    }

    public static void updateStat(LivingEntity entity, ItemStack stack, ResourceLocation statId, long value, BiFunction<Long, Long, Long> mergeFunction, boolean fireEvents) {
        if (stack.isEmpty() || stack.getMaxStackSize() > 1) return;

        if (isSealed(stack, statId)) return;

        if (!stack.has(ModDataComponents.ITEM_UUID)) {
            stack.set(ModDataComponents.ITEM_UUID, UUID.randomUUID());
        }

        if (entity instanceof ServerPlayer) {
            updateOwnership(entity, stack);
        }

        UUID itemUuid = stack.get(ModDataComponents.ITEM_UUID);

        IStatProvider provider = StatProviderRegistry.getProvider(stack, statId);
        long oldValue = provider.getStat(stack, statId);

        long newValue = mergeFunction.apply(oldValue, value);

        if (newValue == oldValue) return;

        RecursionKey key = new RecursionKey(itemUuid, statId);
        boolean isRecursive = RECURSION_GUARD.get().contains(key);

        boolean shouldFire = fireEvents && !isRecursive;

        if (shouldFire) {
            RECURSION_GUARD.get().add(key);
        }

        try {
            if (shouldFire) {
                StatChangeEvent.Pre preEvent = new StatChangeEvent.Pre(entity, stack, statId, oldValue, newValue);
                if (NeoForge.EVENT_BUS.post(preEvent).isCanceled()) {
                    return;
                }
            }

            provider.setStat(stack, statId, newValue);

            if (shouldFire) {
                NeoForge.EVENT_BUS.post(new StatChangeEvent.Post(entity, stack, statId, oldValue, newValue));
            }
        } finally {
            if (shouldFire) {
                RECURSION_GUARD.get().remove(key);
            }
        }
    }

    private static void updateOwnership(LivingEntity entity, ItemStack stack) {
        if (!stack.has(ModDataComponents.ITEM_METADATA)) {
            String playerName = entity.getName().getString();
            long worldDay = entity.level().getDayTime() / 24000L;
            String originalName = stack.getHoverName().getString();

            stack.set(ModDataComponents.ITEM_METADATA, new ItemMetadata(playerName, worldDay, originalName, List.of()));
            return;
        }

        ItemMetadata meta = stack.get(ModDataComponents.ITEM_METADATA);
        String currentPlayer = entity.getName().getString();

        if (meta.creatorName().equals(currentPlayer)) return;

        if (!meta.wieldedBy().isEmpty()) {
            if (meta.wieldedBy().getLast().ownerName().equals(currentPlayer)) return;
        }

        long worldDay = entity.level().getDayTime() / 24000L;
        List<ItemMetadata.OwnerEntry> newHistory = new ArrayList<>(meta.wieldedBy());
        newHistory.add(new ItemMetadata.OwnerEntry(currentPlayer, worldDay));

        stack.set(ModDataComponents.ITEM_METADATA, new ItemMetadata(
                meta.creatorName(),
                meta.createdOnWorldDay(),
                meta.originalName(),
                newHistory
        ));
    }

    public static void sealStat(ItemStack stack, ResourceLocation statId) {
        if (stack.isEmpty()) return;

        if (stack.has(ModDataComponents.TRACKER_MAP)) {
            stack.update(ModDataComponents.TRACKER_MAP, TrackerMap.EMPTY, map -> map.seal(statId));
        }
    }

    public static boolean isSealed(ItemStack stack, ResourceLocation statId) {
        if (stack.isEmpty()) return false;
        if (stack.has(ModDataComponents.TRACKER_MAP)) {
            return stack.get(ModDataComponents.TRACKER_MAP).isSealed(statId);
        }
        return false;
    }

    public static void sendPartialUpdate(ServerPlayer player, ItemStack stack, ResourceLocation statId, long value) {
        if (stack.isEmpty() || !stack.has(ModDataComponents.ITEM_UUID)) return;

        PacketDistributor.sendToPlayer(player, new StatUpdatePayload(
                stack.get(ModDataComponents.ITEM_UUID),
                statId,
                value
        ));
    }

    public static TrackerMap mergeStats(TrackerMap base, TrackerMap incoming) {
        if (incoming.stats().isEmpty()) return base;

        Map<ResourceLocation, Long> newStats = new HashMap<>(base.stats());
        Map<ResourceLocation, Integer> newCounts = new HashMap<>(base.counts());
        Set<ResourceLocation> newSealed = new HashSet<>(base.sealed());

        newSealed.addAll(incoming.sealed());

        for (Map.Entry<ResourceLocation, Long> entry : incoming.stats().entrySet()) {
            ResourceLocation stat = entry.getKey();

            if (base.isSealed(stat)) continue;

            long incomingValue = entry.getValue();
            long baseValue = base.getValue(stat);

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
        return StatProviderRegistry.getProvider(stack, statId).getStat(stack, statId);
    }

    public static boolean hasUnlockedMilestone(ItemStack stack, ResourceLocation statId, long milestoneValue) {
        if (stack.isEmpty() || !stack.has(ModDataComponents.MILESTONES)) return false;
        String key = statId.toString() + "/" + milestoneValue;
        return stack.get(ModDataComponents.MILESTONES).hasUnlocked(key);
    }

    public static boolean isMastered(ItemStack stack) {
        if (stack.isEmpty()) return false;

        for (StatMastery rule : StatMasteryManager.getRules(stack)) {
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