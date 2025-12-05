package com.kjmaster.memento.event;

import com.kjmaster.memento.api.MementoAPI;
import com.kjmaster.memento.data.StatTrigger;
import com.kjmaster.memento.data.StatTriggerManager;
import com.kjmaster.memento.registry.ModTags;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
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
        if (event.getState().is(ModTags.STAT_BLACKLIST_BLOCKS)) return;

        ItemStack stack = player.getMainHandItem();
        List<StatTrigger> triggers = StatTriggerManager.getTriggers(StatTrigger.TriggerType.BLOCK_BREAK, stack);
        if (triggers.isEmpty()) return;

        BlockInWorld blockCtx = new BlockInWorld(event.getLevel(), event.getPos(), true);

        for (StatTrigger trigger : triggers) {
            if (trigger.item().isPresent() && !trigger.item().get().test(stack)) continue;
            if (trigger.block().isPresent() && !trigger.block().get().matches(blockCtx)) continue;

            // Environment Check
            if (!checkEnvironment(trigger, (ServerLevel) event.getLevel(), event.getPos())) continue;

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
            if (trigger.item().isPresent() && !trigger.item().get().test(stack)) continue;

            if (trigger.target().isPresent()) {
                if (!trigger.target().get().matches(player.serverLevel(), target.position(), target)) {
                    continue;
                }
            }

            // Environment Check (Use Player's position)
            if (!checkEnvironment(trigger, player.serverLevel(), player.blockPosition())) continue;

            MementoAPI.incrementStat(player, stack, trigger.stat(), trigger.amount());
        }
    }

    @SubscribeEvent
    public static void onItemUse(LivingEntityUseItemEvent.Finish event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        ItemStack usedStack = event.getItem();

        for (ItemStack heldStack : player.getHandSlots()) {
            if (heldStack.isEmpty()) continue;

            List<StatTrigger> triggers = StatTriggerManager.getTriggers(StatTrigger.TriggerType.ITEM_USE, heldStack);

            for (StatTrigger trigger : triggers) {
                if (trigger.item().isPresent() && !trigger.item().get().test(heldStack)) continue;
                if (trigger.subjectItem().isPresent() && !trigger.subjectItem().get().test(usedStack)) continue;

                // Environment Check
                if (!checkEnvironment(trigger, player.serverLevel(), player.blockPosition())) continue;

                MementoAPI.incrementStat(player, heldStack, trigger.stat(), trigger.amount());
            }
        }
    }

    @SubscribeEvent
    public static void onBlockPlace(BlockEvent.EntityPlaceEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (!(event.getLevel() instanceof ServerLevel serverLevel)) return;

        ItemStack stack = player.getMainHandItem();
        processPlaceTriggers(player, stack, event, serverLevel);

        ItemStack offStack = player.getOffhandItem();
        if (!offStack.isEmpty() && offStack != stack) {
            processPlaceTriggers(player, offStack, event, serverLevel);
        }
    }

    private static void processPlaceTriggers(ServerPlayer player, ItemStack stack, BlockEvent.EntityPlaceEvent event, ServerLevel level) {
        List<StatTrigger> triggers = StatTriggerManager.getTriggers(StatTrigger.TriggerType.BLOCK_PLACE, stack);
        if (triggers.isEmpty()) return;

        BlockInWorld blockCtx = new BlockInWorld(event.getLevel(), event.getPos(), true);

        for (StatTrigger trigger : triggers) {
            if (trigger.block().isPresent() && !trigger.block().get().matches(blockCtx)) continue;
            if (trigger.item().isPresent() && !trigger.item().get().test(stack)) continue;

            // Environment Check
            if (!checkEnvironment(trigger, level, event.getPos())) continue;

            MementoAPI.incrementStat(player, stack, trigger.stat(), trigger.amount());
        }
    }

    @SubscribeEvent
    public static void onToolModification(BlockEvent.BlockToolModificationEvent event) {
        if (!(event.getPlayer() instanceof ServerPlayer player)) return;
        if (event.isSimulated()) return;
        if (!(event.getContext().getLevel() instanceof ServerLevel serverLevel)) return;

        ItemStack stack = event.getHeldItemStack();
        List<StatTrigger> triggers = StatTriggerManager.getTriggers(StatTrigger.TriggerType.TOOL_MODIFICATION, stack);
        if (triggers.isEmpty()) return;

        BlockInWorld blockCtx = new BlockInWorld(event.getContext().getLevel(), event.getContext().getClickedPos(), true);

        for (StatTrigger trigger : triggers) {
            if (trigger.item().isPresent() && !trigger.item().get().test(stack)) continue;
            if (trigger.block().isPresent() && !trigger.block().get().matches(blockCtx)) continue;

            // Environment Check
            if (!checkEnvironment(trigger, serverLevel, event.getContext().getClickedPos())) continue;

            MementoAPI.incrementStat(player, stack, trigger.stat(), trigger.amount());
        }
    }

    /**
     * Validates the Location and Weather predicates.
     */
    private static boolean checkEnvironment(StatTrigger trigger, ServerLevel level, BlockPos pos) {
        // 1. Location Predicate (Dimension, Biome, Y-Level, Structure, etc.)
        if (trigger.location().isPresent()) {
            if (!trigger.location().get().matches(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5)) {
                return false;
            }
        }

        // 2. Weather Check
        if (trigger.weather().isPresent()) {
            String w = trigger.weather().get();
            boolean isRaining = level.isRaining();
            boolean isThundering = level.isThundering();

            if (w.equalsIgnoreCase("rain") && !isRaining) return false;
            if (w.equalsIgnoreCase("thunder") && !isThundering) return false;
            if (w.equalsIgnoreCase("clear") && isRaining) return false;
        }

        return true;
    }
}