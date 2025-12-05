package com.kjmaster.memento.event;

import com.kjmaster.memento.api.event.StatChangeEvent;
import com.kjmaster.memento.data.StatAttribute;
import com.kjmaster.memento.data.StatAttributeManager;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.neoforged.bus.api.SubscribeEvent;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AttributeEventHandler {

    // Optimization: Only update if the value changes by at least this amount.
    // This prevents spamming updates for high-frequency stats (like distance) that scale linearly.
    private static final double UPDATE_THRESHOLD = 0.001;

    @SubscribeEvent
    public static void onStatChange(StatChangeEvent.Post event) {
        List<StatAttribute> rules = StatAttributeManager.getRulesFor(event.getStatId());
        if (rules.isEmpty()) return;

        ItemStack stack = event.getItem();
        ItemAttributeModifiers currentModifiers = stack.getOrDefault(DataComponents.ATTRIBUTE_MODIFIERS, ItemAttributeModifiers.EMPTY);
        long statValue = event.getNewValue();

        // 1. Snapshot existing values for "Dirty Check" optimization
        Map<String, Double> existingValues = new HashMap<>();
        for (ItemAttributeModifiers.Entry entry : currentModifiers.modifiers()) {
            existingValues.put(entry.modifier().id().toString(), entry.modifier().amount());
        }

        boolean needsUpdate = false;

        // 2. Check if any rule requires an update
        for (StatAttribute rule : rules) {
            double newBonus = calculateBonus(statValue, rule);
            if (newBonus > rule.maxBonus()) newBonus = rule.maxBonus();

            String modId = rule.modifierId().toString();

            if (existingValues.containsKey(modId)) {
                double currentBonus = existingValues.get(modId);
                // Only update if difference is significant
                if (Math.abs(newBonus - currentBonus) >= UPDATE_THRESHOLD) {
                    needsUpdate = true;
                    break;
                }
            } else {
                // Modifier doesn't exist yet, apply it if non-zero
                if (Math.abs(newBonus) > 0.0001) {
                    needsUpdate = true;
                    break;
                }
            }
        }

        if (!needsUpdate) return;

        // 3. Rebuild Modifier List
        ItemAttributeModifiers.Builder newModifiers = ItemAttributeModifiers.builder();

        // Keep modifiers NOT managed by the current stat rules
        for (ItemAttributeModifiers.Entry entry : currentModifiers.modifiers()) {
            boolean isManagedByMemento = rules.stream().anyMatch(r -> r.modifierId().equals(entry.modifier().id()));
            if (!isManagedByMemento) {
                newModifiers.add(entry.attribute(), entry.modifier(), entry.slot());
            }
        }

        // Add new calculated modifiers
        for (StatAttribute rule : rules) {
            double bonus = calculateBonus(statValue, rule);
            if (bonus > rule.maxBonus()) bonus = rule.maxBonus();

            if (Math.abs(bonus) > 0.0001) {
                AttributeModifier modifier = new AttributeModifier(
                        rule.modifierId(),
                        bonus,
                        rule.operation()
                );

                newModifiers.add(
                        BuiltInRegistries.ATTRIBUTE.wrapAsHolder(rule.attribute()),
                        modifier,
                        rule.slots()
                );
            }
        }

        stack.set(DataComponents.ATTRIBUTE_MODIFIERS, newModifiers.build());
    }

    private static double calculateBonus(long statValue, StatAttribute rule) {
        return switch (rule.scalingFunction()) {
            // y = m * x
            case LINEAR -> statValue * rule.valuePerStat();
            // y = m * ln(x + 1)
            case LOGARITHMIC -> rule.valuePerStat() * Math.log(statValue + 1);
            // y = m * (x ^ k)
            case EXPONENTIAL -> rule.valuePerStat() * Math.pow(statValue, rule.exponent());
        };
    }
}