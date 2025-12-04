package com.kjmaster.memento.event;

import com.kjmaster.memento.Config;
import com.kjmaster.memento.data.StatBufferManager;
import com.kjmaster.memento.registry.ModDataAttachments;
import com.kjmaster.memento.registry.ModStats;
import com.kjmaster.memento.util.ItemContextHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

public class AviationEvents {

    // Threshold in cm (100 cm = 1 block/meter)
    private static final int UPDATE_THRESHOLD = 100;

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {

        if (!Config.isDefaultEnabled(ModStats.DISTANCE_FLOWN)) return;

        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        // 1. Get Vanilla Stat (Current Value)
        int currentStat = player.getStats().getValue(Stats.CUSTOM.get(Stats.AVIATE_ONE_CM));

        // 2. Get Last Known Value from Data Attachment
        int previousStat = player.getData(ModDataAttachments.LAST_AVIATE_VALUE);

        // 3. Compare
        if (currentStat > previousStat) {
            int difference = currentStat - previousStat;

            // PERFORMANCE: Throttle updates.
            if (difference < UPDATE_THRESHOLD) {
                return;
            }

            // Only count if the player is actually wearing an Elytra
            ItemStack chestItem = player.getItemBySlot(EquipmentSlot.CHEST);
            if (ItemContextHelper.isElytra(chestItem, player)) {
                // OPTIMIZATION: Use Buffer Manager instead of direct API call
                // This prevents re-creating the Data Component map on every meter flown.
                StatBufferManager.bufferStat(player, chestItem, ModStats.DISTANCE_FLOWN, difference);
            }

            // Update the attachment with the new current value
            player.setData(ModDataAttachments.LAST_AVIATE_VALUE, currentStat);
        } else if (currentStat < previousStat) {
            player.setData(ModDataAttachments.LAST_AVIATE_VALUE, currentStat);
        }
    }
}