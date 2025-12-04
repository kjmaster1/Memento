package com.kjmaster.memento.event;

import com.kjmaster.memento.api.MementoAPI;
import com.kjmaster.memento.data.StatUsageSpeed;
import com.kjmaster.memento.data.StatUsageSpeedManager;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;

public class ItemUseEventHandler {

    @SubscribeEvent
    public static void onUseItemStart(LivingEntityUseItemEvent.Start event) {
        ItemStack stack = event.getItem();
        if (stack.isEmpty()) return;

        int originalDuration = event.getDuration();
        if (originalDuration <= 0) return;

        double multiplier = 1.0;
        boolean changed = false;

        for (StatUsageSpeed rule : StatUsageSpeedManager.getAllRules()) {
            long val = MementoAPI.getStat(stack, rule.stat());
            if (val >= rule.minInfo()) {
                multiplier *= rule.multiplier();
                changed = true;
            }
        }

        if (changed) {
            int newDuration = (int) Math.max(1, originalDuration * multiplier);
            event.setDuration(newDuration);
        }
    }
}