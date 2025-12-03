package com.kjmaster.memento.event;

import com.kjmaster.memento.api.MementoAPI;
import com.kjmaster.memento.registry.ModStats;
import com.kjmaster.memento.util.ItemContextHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class AviationEvents {

    private static final Map<UUID, Integer> lastAviateStats = new HashMap<>();

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        // 1. Get Vanilla Stat
        int currentStat = player.getStats().getValue(Stats.CUSTOM.get(Stats.AVIATE_ONE_CM));

        // 2. Initialize or Update Cache
        if (!lastAviateStats.containsKey(player.getUUID())) {
            lastAviateStats.put(player.getUUID(), currentStat);
            return;
        }

        int previousStat = lastAviateStats.get(player.getUUID());
        int difference = currentStat - previousStat;

        // 3. Update Item if flown
        if (difference > 0) {
            ItemStack chestItem = player.getItemBySlot(EquipmentSlot.CHEST);

            if (ItemContextHelper.isElytra(chestItem, player)) {
                MementoAPI.incrementStat(player, chestItem, ModStats.DISTANCE_FLOWN, difference);
            }
        }

        lastAviateStats.put(player.getUUID(), currentStat);
    }

    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        lastAviateStats.remove(event.getEntity().getUUID());
    }
}