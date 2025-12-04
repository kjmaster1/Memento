package com.kjmaster.memento.event;

import com.kjmaster.memento.api.event.StatChangeEvent;
import com.kjmaster.memento.data.StatAttribute;
import com.kjmaster.memento.data.StatAttributeManager;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.neoforged.bus.api.SubscribeEvent;

import java.util.List;

public class AttributeEventHandler {

    @SubscribeEvent
    public void onStatChange(StatChangeEvent.Post event) {
        List<StatAttribute> rules = StatAttributeManager.getRulesFor(event.getStatId());
        if (rules.isEmpty()) return;

        ItemStack stack = event.getItem();
        ItemAttributeModifiers currentModifiers = stack.getOrDefault(DataComponents.ATTRIBUTE_MODIFIERS, ItemAttributeModifiers.EMPTY);
        ItemAttributeModifiers.Builder newModifiers = ItemAttributeModifiers.builder();

        // 1. Copy existing modifiers, BUT filter out the ones we are about to update
        // (We identify our modifiers by their specific ResourceLocation ID)
        for (ItemAttributeModifiers.Entry entry : currentModifiers.modifiers()) {
            boolean isManagedByMemento = rules.stream().anyMatch(r -> r.modifierId().equals(entry.modifier().id()));
            if (!isManagedByMemento) {
                newModifiers.add(entry.attribute(), entry.modifier(), entry.slot());
            }
        }

        // 2. Add recalculated Memento modifiers
        boolean changed = false;
        long statValue = event.getNewValue();

        for (StatAttribute rule : rules) {
            double bonus = statValue * rule.valuePerStat();
            if (bonus > rule.maxBonus()) bonus = rule.maxBonus();

            // Only apply if bonus is significant
            if (Math.abs(bonus) > 0.0001) {
                AttributeModifier modifier = new AttributeModifier(
                        rule.modifierId(),
                        bonus,
                        rule.operation()
                );

                newModifiers.add(
                        Holder.direct(rule.attribute()),
                        modifier,
                        rule.slots()
                );
                changed = true;
            }
        }

        if (changed) {
            stack.set(DataComponents.ATTRIBUTE_MODIFIERS, newModifiers.build());
        }
    }
}