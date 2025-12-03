package com.kjmaster.memento.event;

import com.kjmaster.memento.api.MementoAPI;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.level.BlockEvent;

import static com.kjmaster.memento.registry.ModStats.*;

public class MementoEvents {

    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        if (event.getPlayer() instanceof ServerPlayer player) {
            ItemStack heldItem = player.getMainHandItem();
            if (!heldItem.isEmpty()) {
                MementoAPI.incrementStat(player, heldItem, BLOCKS_BROKEN, 1);
            }
        }
    }

    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        if (event.getSource().getEntity() instanceof ServerPlayer player) {
            ItemStack heldItem = player.getMainHandItem();
            if (!heldItem.isEmpty()) {
                MementoAPI.incrementStat(player, heldItem, ENTITIES_KILLED, 1);
            }
        }
    }
}