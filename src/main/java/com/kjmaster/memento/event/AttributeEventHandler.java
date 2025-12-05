package com.kjmaster.memento.event;

import com.kjmaster.memento.api.event.StatChangeEvent;
import com.kjmaster.memento.data.StatAttribute;
import com.kjmaster.memento.data.StatAttributeManager;
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
    private static final double UPDATE_THRESHOLD = 0.001;

    @SubscribeEvent
    public static void onStatChange(StatChangeEvent.Post event) {
        List<StatAttribute> rules = StatAttributeManager.getRulesFor(event.getStatId());
        if (rules.isEmpty()) return;

        ItemStack stack = event.getItem();
        ItemAttributeModifiers currentModifiers = stack.getOrDefault(DataComponents.ATTRIBUTE_MODIFIERS, ItemAttributeModifiers.EMPTY);
        long statValue = event.getNewValue();

        Map<String, Double> existingValues = new HashMap<>();
        for (ItemAttributeModifiers.Entry entry : currentModifiers.modifiers()) {
            existingValues.put(entry.modifier().id().toString(), entry.modifier().amount());
        }

        boolean needsUpdate = false;

        for (StatAttribute rule : rules) {
            // FIX: Check Item Filter
            if (rule.items().isPresent()) {
                if (!rule.items().get().contains(BuiltInRegistries.ITEM.getKey(stack.getItem()))) {
                    continue;
                }
            }

            double newBonus = calculateBonus(statValue, rule);
            if (newBonus > rule.maxBonus()) newBonus = rule.maxBonus();

            String modId = rule.modifierId().toString();

            if (existingValues.containsKey(modId)) {
                if (Math.abs(newBonus - existingValues.get(modId)) >= UPDATE_THRESHOLD) {
                    needsUpdate = true;
                    break;
                }
            } else {
                if (Math.abs(newBonus) > 0.0001) {
                    needsUpdate = true;
                    break;
                }
            }
        }

        if (!needsUpdate) return;

        ItemAttributeModifiers.Builder newModifiers = ItemAttributeModifiers.builder();

        // 1. Keep modifiers NOT managed by our rules
        for (ItemAttributeModifiers.Entry entry : currentModifiers.modifiers()) {
            // Only remove if it matches a rule AND the item restriction matches
            boolean isManagedAndMatching = rules.stream().anyMatch(r ->
                    r.modifierId().equals(entry.modifier().id()) &&
                            (r.items().isEmpty() || r.items().get().contains(BuiltInRegistries.ITEM.getKey(stack.getItem())))
            );

            if (!isManagedAndMatching) {
                newModifiers.add(entry.attribute(), entry.modifier(), entry.slot());
            }
        }

        // 2. Add new modifiers
        for (StatAttribute rule : rules) {
            // FIX: Check Item Filter
            if (rule.items().isPresent()) {
                if (!rule.items().get().contains(BuiltInRegistries.ITEM.getKey(stack.getItem()))) {
                    continue;
                }
            }

            double bonus = calculateBonus(statValue, rule);
            if (bonus > rule.maxBonus()) bonus = rule.maxBonus();

            if (Math.abs(bonus) > 0.0001) {
                newModifiers.add(
                        BuiltInRegistries.ATTRIBUTE.wrapAsHolder(rule.attribute()),
                        new AttributeModifier(rule.modifierId(), bonus, rule.operation()),
                        rule.slots()
                );
            }
        }

        stack.set(DataComponents.ATTRIBUTE_MODIFIERS, newModifiers.build());
    }

    private static double calculateBonus(long statValue, StatAttribute rule) {
        return switch (rule.scalingFunction()) {
            case LINEAR -> statValue * rule.valuePerStat();
            case LOGARITHMIC -> rule.valuePerStat() * Math.log(statValue + 1);
            case EXPONENTIAL -> rule.valuePerStat() * Math.pow(statValue, rule.exponent());
        };
    }
}