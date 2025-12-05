package com.kjmaster.memento.event;

import com.kjmaster.memento.api.MementoAPI;
import com.kjmaster.memento.data.StatDecayManager;
import com.kjmaster.memento.data.StatDecayRule;
import com.kjmaster.memento.util.SlotHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.entity.player.AnvilRepairEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.Collection;
import java.util.List;

public class DecayEvents {

    @SubscribeEvent
    public static void onPlayerDrops(LivingDropsEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer)) return;
        applyDecayToCollection(event.getDrops().stream().map(ItemEntity::getItem).toList(), StatDecayRule.Trigger.DEATH);
    }

    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event) {
        if (!event.isWasDeath()) return;
        Player newPlayer = event.getEntity();
        SlotHelper.forEachWornItem(newPlayer, ctx -> applyDecay(ctx.stack(), StatDecayRule.Trigger.DEATH));
        for (ItemStack stack : newPlayer.getInventory().items) {
            applyDecay(stack, StatDecayRule.Trigger.DEATH);
        }
    }

    @SubscribeEvent
    public static void onAnvilRepair(AnvilRepairEvent event) {
        applyDecay(event.getOutput(), StatDecayRule.Trigger.REPAIR);
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (player.tickCount % 200 != 0) return;

        long gameTime = player.level().getGameTime();

        // We can't easily pre-filter for TICK frequency without iterating, so we do it in apply
        SlotHelper.forEachWornItem(player, ctx -> applyTickDecay(ctx.stack(), gameTime));
        for (ItemStack stack : player.getInventory().items) {
            applyTickDecay(stack, gameTime);
        }
    }

    private static void applyDecayToCollection(Collection<ItemStack> items, StatDecayRule.Trigger trigger) {
        for (ItemStack stack : items) applyDecay(stack, trigger);
    }

    private static void applyDecay(ItemStack stack, StatDecayRule.Trigger trigger) {
        List<StatDecayRule> rules = StatDecayManager.getRules(trigger, stack);
        for (StatDecayRule rule : rules) {
            applySingleRule(stack, rule);
        }
    }

    private static void applyTickDecay(ItemStack stack, long gameTime) {
        List<StatDecayRule> rules = StatDecayManager.getRules(StatDecayRule.Trigger.TICK, stack);
        for (StatDecayRule rule : rules) {
            if (rule.frequency().isPresent() && gameTime % rule.frequency().get() == 0) {
                applySingleRule(stack, rule);
            }
        }
    }

    private static void applySingleRule(ItemStack stack, StatDecayRule rule) {
        if (stack.isEmpty()) return;
        if (rule.itemFilter().isPresent() && !rule.itemFilter().get().test(stack)) return;

        long currentVal = MementoAPI.getStat(stack, rule.stat());
        if (currentVal <= 0) return;

        long newVal = currentVal;
        if (rule.operation() == StatDecayRule.Operation.SUBTRACT) {
            newVal = Math.max(0, currentVal - (long) rule.value());
        } else if (rule.operation() == StatDecayRule.Operation.MULTIPLY) {
            newVal = (long) (currentVal * rule.value());
        }

        if (newVal != currentVal) {
            MementoAPI.updateStat(null, stack, rule.stat(), newVal, (old, n) -> n, true);
        }
    }
}