package com.kjmaster.memento.event;

import com.kjmaster.memento.api.MementoAPI;
import com.kjmaster.memento.data.StatTrigger;
import com.kjmaster.memento.data.StatTriggerManager;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.pattern.BlockInWorld;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import net.neoforged.neoforge.event.level.BlockEvent;

import java.util.List;

public class DataDrivenEvents {

    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        if (!(event.getPlayer() instanceof ServerPlayer player)) return;

        ItemStack stack = player.getMainHandItem();
        // OPTIMIZATION: Use getTriggers with stack context
        List<StatTrigger> triggers = StatTriggerManager.getTriggers(StatTrigger.TriggerType.BLOCK_BREAK, stack);
        if (triggers.isEmpty()) return;

        BlockInWorld blockCtx = new BlockInWorld(event.getLevel(), event.getPos(), true);

        for (StatTrigger trigger : triggers) {
            // 1. Check Item (Held)
            if (trigger.item().isPresent() && !trigger.item().get().test(stack)) continue;

            // 2. Check Block
            if (trigger.block().isPresent() && !trigger.block().get().matches(blockCtx)) continue;

            // Apply Stat
            MementoAPI.incrementStat(player, stack, trigger.stat(), trigger.amount());
        }
    }

    @SubscribeEvent
    public static void onEntityKill(LivingDeathEvent event) {
        if (!(event.getSource().getEntity() instanceof ServerPlayer player)) return;

        ItemStack stack = player.getMainHandItem();
        List<StatTrigger> triggers = StatTriggerManager.getTriggers(StatTrigger.TriggerType.ENTITY_KILL, stack);
        if (triggers.isEmpty()) return;

        Entity target = event.getEntity();

        for (StatTrigger trigger : triggers) {
            // 1. Check Item
            if (trigger.item().isPresent() && !trigger.item().get().test(stack)) continue;

            // 2. Check Entity (Target)
            if (trigger.target().isPresent()) {
                if (!trigger.target().get().matches(player.serverLevel(), target.position(), target)) {
                    continue;
                }
            }

            // Apply Stat
            MementoAPI.incrementStat(player, stack, trigger.stat(), trigger.amount());
        }
    }

    @SubscribeEvent
    public static void onItemUse(LivingEntityUseItemEvent.Finish event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        ItemStack usedStack = event.getItem(); // The specific item (e.g., Steak)

        // Scan both hands to find the "Memento" item that wants to record this event
        for (ItemStack heldStack : player.getHandSlots()) {
            if (heldStack.isEmpty()) continue;

            // Use heldStack for lookup, as triggers are usually bound to the item HOLDING the stat
            List<StatTrigger> triggers = StatTriggerManager.getTriggers(StatTrigger.TriggerType.ITEM_USE, heldStack);

            for (StatTrigger trigger : triggers) {
                // 1. Check Receiver (The item getting the stat)
                if (trigger.item().isPresent() && !trigger.item().get().test(heldStack)) continue;

                // 2. Check Subject (The item being used)
                if (trigger.subjectItem().isPresent() && !trigger.subjectItem().get().test(usedStack)) continue;

                MementoAPI.incrementStat(player, heldStack, trigger.stat(), trigger.amount());
            }
        }
    }

    @SubscribeEvent
    public static void onBlockPlace(BlockEvent.EntityPlaceEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        ItemStack stack = player.getMainHandItem();

        processPlaceTriggers(player, stack, event);

        ItemStack offStack = player.getOffhandItem();
        if (!offStack.isEmpty() && offStack != stack) {
            processPlaceTriggers(player, offStack, event);
        }
    }

    private static void processPlaceTriggers(ServerPlayer player, ItemStack stack, BlockEvent.EntityPlaceEvent event) {
        List<StatTrigger> triggers = StatTriggerManager.getTriggers(StatTrigger.TriggerType.BLOCK_PLACE, stack);
        if (triggers.isEmpty()) return;

        BlockInWorld blockCtx = new BlockInWorld(event.getLevel(), event.getPos(), true);

        for (StatTrigger trigger : triggers) {
            // Check Block (The one just placed)
            if (trigger.block().isPresent() && !trigger.block().get().matches(blockCtx)) continue;

            // Check Item (Held)
            if (trigger.item().isPresent() && !trigger.item().get().test(stack)) continue;

            MementoAPI.incrementStat(player, stack, trigger.stat(), trigger.amount());
        }
    }

    @SubscribeEvent
    public static void onToolModification(BlockEvent.BlockToolModificationEvent event) {
        if (!(event.getPlayer() instanceof ServerPlayer player)) return;
        if (event.isSimulated()) return; // Don't count simulation

        ItemStack stack = event.getHeldItemStack();
        List<StatTrigger> triggers = StatTriggerManager.getTriggers(StatTrigger.TriggerType.TOOL_MODIFICATION, stack);
        if (triggers.isEmpty()) return;

        BlockInWorld blockCtx = new BlockInWorld(event.getContext().getLevel(), event.getContext().getClickedPos(), true);

        for (StatTrigger trigger : triggers) {
            if (trigger.item().isPresent() && !trigger.item().get().test(stack)) continue;
            if (trigger.block().isPresent() && !trigger.block().get().matches(blockCtx)) continue;

            MementoAPI.incrementStat(player, stack, trigger.stat(), trigger.amount());
        }
    }
}