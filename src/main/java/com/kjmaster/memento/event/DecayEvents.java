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

    // --- Death Triggers ---

    @SubscribeEvent
    public static void onPlayerDrops(LivingDropsEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer)) return;

        // Scenario 1: Items are dropping
        applyDecayToCollection(event.getDrops().stream().map(ItemEntity::getItem).toList(), StatDecayRule.Trigger.DEATH);
    }

    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event) {
        if (!event.isWasDeath()) return;

        // Scenario 2: Items kept (KeepInventory or Grave mods that restore immediately)
        // We iterate the NEW player's inventory
        Player newPlayer = event.getEntity();
        SlotHelper.forEachWornItem(newPlayer, ctx ->
                applyDecay(ctx.stack(), StatDecayRule.Trigger.DEATH)
        );

        // Don't forget main inventory which SlotHelper.forEachWornItem might skip if it only does equipment
        for (ItemStack stack : newPlayer.getInventory().items) {
            applyDecay(stack, StatDecayRule.Trigger.DEATH);
        }
    }

    // --- Repair Triggers ---

    @SubscribeEvent
    public static void onAnvilRepair(AnvilRepairEvent event) {
        // Output is the result item
        applyDecay(event.getOutput(), StatDecayRule.Trigger.REPAIR);
    }

    // --- Tick Triggers ---

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        // Run every 200 ticks (10 seconds) to align with Buffer Flush for performance
        if (player.tickCount % 200 != 0) return;

        long gameTime = player.level().getGameTime();
        List<StatDecayRule> rules = StatDecayManager.getRules(StatDecayRule.Trigger.TICK);
        if (rules.isEmpty()) return;

        // Helper to check if a specific rule should run this cycle
        List<StatDecayRule> activeRules = rules.stream()
                .filter(r -> r.frequency().isPresent() && gameTime % r.frequency().get() == 0)
                .toList();

        if (activeRules.isEmpty()) return;

        // Apply
        SlotHelper.forEachWornItem(player, ctx -> {
            for (StatDecayRule rule : activeRules) {
                applySingleRule(ctx.stack(), rule);
            }
        });

        for (ItemStack stack : player.getInventory().items) {
            for (StatDecayRule rule : activeRules) {
                applySingleRule(stack, rule);
            }
        }
    }

    // --- Logic ---

    private static void applyDecayToCollection(Collection<ItemStack> items, StatDecayRule.Trigger trigger) {
        List<StatDecayRule> rules = StatDecayManager.getRules(trigger);
        if (rules.isEmpty()) return;

        for (ItemStack stack : items) {
            for (StatDecayRule rule : rules) {
                applySingleRule(stack, rule);
            }
        }
    }

    private static void applyDecay(ItemStack stack, StatDecayRule.Trigger trigger) {
        List<StatDecayRule> rules = StatDecayManager.getRules(trigger);
        for (StatDecayRule rule : rules) {
            applySingleRule(stack, rule);
        }
    }

    private static void applySingleRule(ItemStack stack, StatDecayRule rule) {
        if (stack.isEmpty()) return;

        // Check Filter
        if (rule.itemFilter().isPresent() && !rule.itemFilter().get().test(stack)) return;

        // Calculate
        long currentVal = MementoAPI.getStat(stack, rule.stat());
        if (currentVal <= 0) return;

        long newVal = currentVal;
        if (rule.operation() == StatDecayRule.Operation.SUBTRACT) {
            newVal = Math.max(0, currentVal - (long) rule.value());
        } else if (rule.operation() == StatDecayRule.Operation.MULTIPLY) {
            newVal = (long) (currentVal * rule.value());
        }

        if (newVal != currentVal) {
            // Update Stat (No special merge function needed, just overwrite)
            MementoAPI.updateStat(null, stack, rule.stat(), newVal, (old, n) -> n, true);
        }
    }
}