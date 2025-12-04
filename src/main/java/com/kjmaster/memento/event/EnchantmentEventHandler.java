package com.kjmaster.memento.event;

import com.kjmaster.memento.api.event.StatChangeEvent;
import com.kjmaster.memento.data.StatEnchantment;
import com.kjmaster.memento.data.StatEnchantmentManager;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.neoforged.bus.api.SubscribeEvent;

import java.util.List;

public class EnchantmentEventHandler {

    @SubscribeEvent
    public static void onStatChange(StatChangeEvent.Post event) {
        List<StatEnchantment> rules = StatEnchantmentManager.getRulesFor(event.getStatId());
        if (rules.isEmpty()) return;

        ItemStack stack = event.getItem();
        long statValue = event.getNewValue();

        ItemEnchantments currentEnchants = stack.getTagEnchantments();
        ItemEnchantments.Mutable newEnchants = new ItemEnchantments.Mutable(currentEnchants);
        boolean changed = false;

        var registryAccess = event.getEntity().registryAccess();

        for (StatEnchantment rule : rules) {
            var enchantRegistry = registryAccess.lookupOrThrow(Registries.ENCHANTMENT);
            var enchantKey = ResourceKey.create(Registries.ENCHANTMENT, rule.enchantment());
            var enchantHolder = enchantRegistry.get(enchantKey);

            if (enchantHolder.isEmpty()) continue;

            if (statValue >= rule.minInfo()) {
                int currentLevel = newEnchants.getLevel(enchantHolder.get());
                if (currentLevel < rule.level()) {
                    newEnchants.set(enchantHolder.get(), rule.level());
                    changed = true;
                }
            }
            else if (rule.removeIfBelow()) {
                int currentLevel = newEnchants.getLevel(enchantHolder.get());
                if (currentLevel == rule.level()) {
                    newEnchants.set(enchantHolder.get(), 0);
                    changed = true;
                }
            }
        }

        if (changed) {
            EnchantmentHelper.setEnchantments(stack, newEnchants.toImmutable());
        }
    }
}