package com.kjmaster.memento.data;

import com.kjmaster.memento.Memento;
import com.kjmaster.memento.api.MementoAPI;
import com.kjmaster.memento.registry.ModDataAttachments;
import com.kjmaster.memento.registry.ModDataComponents;
import com.kjmaster.memento.util.SlotHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
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

        // Track UUIDs encountered in this pass to detect duplicates (e.g. from Creative Pick Block)
        Set<UUID> seenUuids = new HashSet<>();

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
            if (pending.containsKey(uuid)) {
                applyPending(player, stack, pending);
            }
        };

        // 1. Scan Inventory (Main, Armor, Offhand)
        Inventory inv = player.getInventory();
        inv.items.forEach(processor);
        inv.armor.forEach(processor);
        inv.offhand.forEach(processor);

        // 2. Scan Curios
        // Uses the optimized helper to avoid double-scanning Vanilla slots
        SlotHelper.processCurios(player, (stack, slotIndex) -> processor.accept(stack));
    }

    private static void applyPending(ServerPlayer player, ItemStack stack, Map<UUID, Map<ResourceLocation, Long>> pending) {
        // Double check UUID existence (though processor checked it)
        if (!stack.has(ModDataComponents.ITEM_UUID)) return;
        UUID uuid = stack.get(ModDataComponents.ITEM_UUID);

        if (pending.containsKey(uuid)) {
            Map<ResourceLocation, Long> stats = pending.get(uuid);

            // Apply all buffered stats
            stats.forEach((statId, amount) ->
                    MementoAPI.incrementStat(player, stack, statId, amount)
            );

            // Critical: Remove from buffer to prevent "ghost" applications if another item
            // with this UUID (which somehow evaded detection) is found later,
            // or simply to clear the buffer.
            pending.remove(uuid);
        }
    }

    // --- Events ---

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            // Flush every 10 seconds (200 ticks)
            if (player.tickCount % 200 == 0) {
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
}