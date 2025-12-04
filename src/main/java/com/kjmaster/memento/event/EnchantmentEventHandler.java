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
    public void onStatChange(StatChangeEvent.Post event) {
        // 1. Get rules relevant to the stat that just changed
        List<StatEnchantment> rules = StatEnchantmentManager.getRulesFor(event.getStatId());
        if (rules.isEmpty()) return;

        ItemStack stack = event.getItem();
        long statValue = event.getNewValue();

        // 2. Prepare mutable enchantments list
        ItemEnchantments currentEnchants = stack.getTagEnchantments();
        ItemEnchantments.Mutable newEnchants = new ItemEnchantments.Mutable(currentEnchants);
        boolean changed = false;

        // 3. Iterate rules
        for (StatEnchantment rule : rules) {
            // Resolve the Enchantment from the Registry
            var registryAccess = event.getPlayer().registryAccess();
            var enchantRegistry = registryAccess.lookupOrThrow(Registries.ENCHANTMENT);
            var enchantKey = ResourceKey.create(Registries.ENCHANTMENT, rule.enchantment());
            var enchantHolder = enchantRegistry.get(enchantKey);

            if (enchantHolder.isEmpty()) continue;

            // Logic: Upgrade
            if (statValue >= rule.minInfo()) {
                int currentLevel = newEnchants.getLevel(enchantHolder.get());
                // Only upgrade if the new level is higher than what we currently have.
                // This prevents downgrading an item the user manually enchanted to a higher level.
                if (currentLevel < rule.level()) {
                    newEnchants.set(enchantHolder.get(), rule.level());
                    changed = true;
                }
            }
            // Logic: Downgrade/Remove (e.g. if stat was reset or item copied but stat lost)
            else if (rule.removeIfBelow()) {
                int currentLevel = newEnchants.getLevel(enchantHolder.get());
                // Only remove if the level matches EXACTLY what this rule would have given.
                // This is a safety check so we don't wipe a user's legitimate Fortune III just because
                // they haven't reached the "Fortune I" stat milestone yet.
                if (currentLevel == rule.level()) {
                    newEnchants.set(enchantHolder.get(), 0); // 0 removes the enchantment
                    changed = true;
                }
            }
        }

        // 4. Apply changes to ItemStack
        if (changed) {
            EnchantmentHelper.setEnchantments(stack, newEnchants.toImmutable());
        }
    }
}