package com.kjmaster.memento.event;

import com.kjmaster.memento.component.ItemMetadata;
import com.kjmaster.memento.component.TrackerMap;
import com.kjmaster.memento.registry.ModDataComponents;
import com.kjmaster.memento.registry.ModStats;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

import java.util.Map;

public class MementoClientEvents {

    @SubscribeEvent
    public static void onTooltip(ItemTooltipEvent event) {

        // 1. Check if the item has TrackerMap component or ItemMetadata component
        if (!event.getItemStack().has(ModDataComponents.TRACKER_MAP) && !event.getItemStack().has(ModDataComponents.ITEM_METADATA)) {
            return;
        }

        // 2. logic for "Hold Shift" vs "Show Stats"
        if (Screen.hasShiftDown()) {
            event.getToolTip().add(Component.translatable("tooltip.memento.header")
                    .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));

            if (event.getItemStack().has(ModDataComponents.ITEM_METADATA)) {
                ItemMetadata meta = event.getItemStack().get(ModDataComponents.ITEM_METADATA);
                if (meta != null && !meta.creatorName().isEmpty()) {
                    event.getToolTip().add(Component.translatable("tooltip.memento.created_by", meta.creatorName(), meta.createdOnWorldDay())
                            .withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.BOLD));
                }
            }

            TrackerMap trackers = event.getItemStack().get(ModDataComponents.TRACKER_MAP);
            if (trackers == null || trackers.stats().isEmpty()) {
                return;
            }

            // 3. Iterate over the stats map and display them
            for (Map.Entry<ResourceLocation, Long> entry : trackers.stats().entrySet()) {
                ResourceLocation statId = entry.getKey();
                Long value = entry.getValue();
                String translationKey = "stat." + statId.getNamespace() + "." + statId.getPath();
                String valueString = getValueString(statId, value);

                MutableComponent line = Component.translatable(translationKey)
                        .withStyle(ChatFormatting.GRAY)
                        .append(Component.literal(": "))
                        .append(Component.literal(valueString).withStyle(ChatFormatting.WHITE));

                event.getToolTip().add(line);
            }
        } else {
            event.getToolTip().add(Component.translatable("tooltip.memento.hold_shift")
                    .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
        }
    }

    private static String getValueString(ResourceLocation statId, Long value) {
        String valueString;

        if (statId.equals(ModStats.DISTANCE_FLOWN)) {
            double meters = value / 100.0;
            // Format to 2 decimal places (e.g., "150.50m")
            valueString = String.format("%.2fm", meters);
        } else if (statId.equals(ModStats.DAMAGE_TAKEN)) {
            // Convert scaled damage back to real health points
            double damage = value / 100.0;
            valueString = String.format("%.1f", damage);
        } else {
            valueString = String.valueOf(value);
        }
        return valueString;
    }
}
