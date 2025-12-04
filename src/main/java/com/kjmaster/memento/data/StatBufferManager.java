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
import java.util.Map;
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
        if (pending.isEmpty()) return;

        // Reuse the consumer to avoid allocation
        Consumer<ItemStack> applyFunction = stack -> applyPending(player, stack, pending);

        // 1. Scan Inventory (Main, Armor, Offhand)
        Inventory inv = player.getInventory();
        inv.items.forEach(applyFunction);
        inv.armor.forEach(applyFunction);
        inv.offhand.forEach(applyFunction);

        // 2. Scan Curios
        // OPTIMIZATION: Previously called SlotHelper.getAllWornItems(), which allocated a List
        // AND re-scanned Armor/Offhand. Now we exclusively scan Curios slots.
        SlotHelper.processCurios(player, (stack, slotIndex) -> applyFunction.accept(stack));

        // Note: Any stats remaining in 'pending' belong to items that are currently
        // not in the player's inventory (e.g. moved to a chest).
        // We leave them in the buffer so they apply if the player picks the item back up later.
    }

    private static void applyPending(ServerPlayer player, ItemStack stack, Map<UUID, Map<ResourceLocation, Long>> pending) {
        if (stack.isEmpty() || !stack.has(ModDataComponents.ITEM_UUID)) return;

        UUID uuid = stack.get(ModDataComponents.ITEM_UUID);
        if (pending.containsKey(uuid)) {
            Map<ResourceLocation, Long> stats = pending.get(uuid);

            // Apply all buffered stats
            stats.forEach((statId, amount) ->
                    MementoAPI.incrementStat(player, stack, statId, amount)
            );

            // Remove from buffer to prevent double application
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