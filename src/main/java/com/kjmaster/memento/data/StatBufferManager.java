package com.kjmaster.memento.data;

import com.kjmaster.memento.Memento;
import com.kjmaster.memento.api.MementoAPI;
import com.kjmaster.memento.registry.ModDataAttachments;
import com.kjmaster.memento.registry.ModDataComponents;
import com.kjmaster.memento.util.SlotHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.item.ItemTossEvent;
import net.neoforged.neoforge.event.entity.player.PlayerContainerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.*;
import java.util.function.Consumer;

@EventBusSubscriber(modid = Memento.MODID)
public class StatBufferManager {

    /**
     * Buffers a stat update for later application.
     * Use this for high-frequency updates (e.g. every tick).
     */
    public static void bufferStat(ServerPlayer player, ItemStack stack, ResourceLocation statId, long amount) {
        if (stack.isEmpty()) return;

        // Ensure the item has a UUID so we can track it in the buffer
        if (!stack.has(ModDataComponents.ITEM_UUID)) {
            stack.set(ModDataComponents.ITEM_UUID, UUID.randomUUID());
        }
        UUID itemUuid = stack.get(ModDataComponents.ITEM_UUID);

        // Add to pending stats
        Map<UUID, Map<ResourceLocation, Long>> pending = player.getData(ModDataAttachments.PENDING_STATS);
        pending.computeIfAbsent(itemUuid, k -> new HashMap<>())
                .merge(statId, amount, Long::sum);
    }

    /**
     * Flushes buffered stats to the actual items.
     * Runs periodically or on save/logout.
     */
    public static void flush(ServerPlayer player) {
        Map<UUID, Map<ResourceLocation, Long>> pending = player.getData(ModDataAttachments.PENDING_STATS);
        if (pending.isEmpty()) return;

        // Track UUIDs encountered in this pass to detect duplicates
        Set<UUID> seenUuids = new HashSet<>();
        // Track UUIDs that were successfully applied so we can clean up orphans
        Set<UUID> appliedUuids = new HashSet<>();

        Consumer<ItemStack> processor = stack -> {
            if (stack.isEmpty()) return;
            // Only care about items that actually HAVE a Memento UUID
            if (!stack.has(ModDataComponents.ITEM_UUID)) return;

            UUID uuid = stack.get(ModDataComponents.ITEM_UUID);

            // 1. DUPLICATE CHECK [Identity Problem Fix]
            if (seenUuids.contains(uuid)) {
                // Identity Crisis: This item shares a UUID with one we already processed this tick.
                // Action: Re-roll this item's UUID to sever the link.
                stack.set(ModDataComponents.ITEM_UUID, UUID.randomUUID());
                // We do NOT apply pending stats to this duplicate.
                return;
            }
            seenUuids.add(uuid);

            // 2. BUFFER FLUSH
            // We rely solely on the UUID. If the UUID matches, it is the correct item.
            // This avoids issues where the game creates copies (new object instances) of the same item.
            if (pending.containsKey(uuid)) {
                applyPending(player, stack, pending);
                appliedUuids.add(uuid);
            }
        };

        // Scan 1: Container Slots (Covers Inventory, Armor, Offhand, AND Open Chests/Anvils)
        // This handles cases where the player has the item in an open UI.
        if (player.containerMenu != null) {
            for (Slot slot : player.containerMenu.slots) {
                processor.accept(slot.getItem());
            }
        }

        // Scan 2: Inventory Explicitly (Redundant but safe fallback if containerMenu behaves oddly)
        Inventory inv = player.getInventory();
        inv.items.forEach(processor);
        inv.armor.forEach(processor);
        inv.offhand.forEach(processor);

        // Scan 3: Curios
        SlotHelper.processCurios(player, (stack, slotIndex) -> processor.accept(stack));

        // 4. ORPHAN CLEANUP
        // Any stats remaining in 'pending' were not applied (Target item missing).
        // We clear them to prevent "ghost" stats applying to a future copy of the item.
        // Note: applyPending removes entries, so 'pending' now only contains orphans.
        pending.clear();
    }

    private static void applyPending(ServerPlayer player, ItemStack stack, Map<UUID, Map<ResourceLocation, Long>> pending) {
        // Double check UUID existence
        if (!stack.has(ModDataComponents.ITEM_UUID)) return;
        UUID uuid = stack.get(ModDataComponents.ITEM_UUID);

        if (pending.containsKey(uuid)) {
            Map<ResourceLocation, Long> stats = pending.get(uuid);

            // Apply all buffered stats
            stats.forEach((statId, amount) ->
                    MementoAPI.incrementStat(player, stack, statId, amount)
            );

            // Critical: Remove from buffer
            pending.remove(uuid);
        }
    }

    // --- Events ---

    @SubscribeEvent
    public static void onItemToss(ItemTossEvent event) {
        if (event.getPlayer() instanceof ServerPlayer player) {
            // CRITICAL: Apply stats immediately to the tossed item so they persist in the world entity.
            // If we wait for the next tick flush, the item will be gone from inventory and stats would be orphaned.
            ItemStack stack = event.getEntity().getItem();

            Map<UUID, Map<ResourceLocation, Long>> pending = player.getData(ModDataAttachments.PENDING_STATS);

            applyPending(player, stack, pending);
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            // Flush every 1 second (20 ticks) to minimize data loss risk from GC
            if (player.tickCount % 20 == 0) {
                flush(player);
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            flush(player);
        }
    }

    @SubscribeEvent
    public static void onPlayerSave(PlayerEvent.SaveToFile event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            flush(player);
        }
    }

    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event) {
        // Persist the buffer across death (so flight data isn't lost if you crash)
        if (event.isWasDeath()) {
            var originalData = event.getOriginal().getData(ModDataAttachments.PENDING_STATS);
            event.getEntity().setData(ModDataAttachments.PENDING_STATS, originalData);
        }
    }

    // --- Aggressive Flushes for "Ship of Theseus" Identity Safety ---

    @SubscribeEvent
    public static void onContainerOpen(PlayerContainerEvent.Open event) {
        // Flush before interaction to save stats to the current item instances.
        if (event.getEntity() instanceof ServerPlayer player) {
            flush(player);
        }
    }

    @SubscribeEvent
    public static void onContainerClose(PlayerContainerEvent.Close event) {
        // Flush after interaction to catch any immediate changes or swaps
        // before the player moves on.
        if (event.getEntity() instanceof ServerPlayer player) {
            flush(player);
        }
    }
}