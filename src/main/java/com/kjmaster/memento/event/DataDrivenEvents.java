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

        List<StatTrigger> triggers = StatTriggerManager.get(StatTrigger.TriggerType.BLOCK_BREAK);
        if (triggers.isEmpty()) return;

        ItemStack stack = player.getMainHandItem();
        // Construct the context for Block Checks
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

        List<StatTrigger> triggers = StatTriggerManager.get(StatTrigger.TriggerType.ENTITY_KILL);
        if (triggers.isEmpty()) return;

        ItemStack stack = player.getMainHandItem();
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

        List<StatTrigger> triggers = StatTriggerManager.get(StatTrigger.TriggerType.ITEM_USE);
        if (triggers.isEmpty()) return;

        ItemStack usedStack = event.getItem(); // The specific item (e.g., Steak)

        // Scan both hands to find the "Memento" item that wants to record this event
        for (ItemStack heldStack : player.getHandSlots()) {
            if (heldStack.isEmpty()) continue;

            for (StatTrigger trigger : triggers) {
                // 1. Check Receiver (The item getting the stat)
                // If the trigger specifies an item (e.g. "Gluttony Charm"), our held stack must match.
                if (trigger.item().isPresent() && !trigger.item().get().test(heldStack)) continue;

                // 2. Check Subject (The item being used)
                // If the trigger specifies a subject (e.g. "Steak"), the used stack must match.
                if (trigger.subjectItem().isPresent() && !trigger.subjectItem().get().test(usedStack)) continue;

                MementoAPI.incrementStat(player, heldStack, trigger.stat(), trigger.amount());
            }
        }
    }

    @SubscribeEvent
    public static void onBlockPlace(BlockEvent.EntityPlaceEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        List<StatTrigger> triggers = StatTriggerManager.get(StatTrigger.TriggerType.BLOCK_PLACE);
        if (triggers.isEmpty()) return;

        // For placement, we assume the item used is in the main hand or offhand.
        // We check Main Hand first.
        ItemStack stack = player.getMainHandItem();
        BlockInWorld blockCtx = new BlockInWorld(event.getLevel(), event.getPos(), true);

        for (StatTrigger trigger : triggers) {
            // Check Block (The one just placed)
            if (trigger.block().isPresent() && !trigger.block().get().matches(blockCtx)) continue;

            // Check Item (Held)
            // If main hand doesn't match, check offhand, as the player might have placed from offhand.
            if (trigger.item().isPresent()) {
                if (!trigger.item().get().test(stack)) {
                    // Try Offhand
                    stack = player.getOffhandItem();
                    if (!trigger.item().get().test(stack)) {
                        continue; // Neither hand matched the required item
                    }
                }
            }

            MementoAPI.incrementStat(player, stack, trigger.stat(), trigger.amount());
        }
    }

    @SubscribeEvent
    public static void onToolModification(BlockEvent.BlockToolModificationEvent event) {
        if (!(event.getPlayer() instanceof ServerPlayer player)) return;
        if (event.isSimulated()) return; // Don't count simulation

        List<StatTrigger> triggers = StatTriggerManager.get(StatTrigger.TriggerType.TOOL_MODIFICATION);
        if (triggers.isEmpty()) return;

        ItemStack stack = event.getHeldItemStack();
        BlockInWorld blockCtx = new BlockInWorld(event.getContext().getLevel(), event.getContext().getClickedPos(), true);

        for (StatTrigger trigger : triggers) {
            if (trigger.item().isPresent() && !trigger.item().get().test(stack)) continue;
            if (trigger.block().isPresent() && !trigger.block().get().matches(blockCtx)) continue;

            MementoAPI.incrementStat(player, stack, trigger.stat(), trigger.amount());
        }
    }
}