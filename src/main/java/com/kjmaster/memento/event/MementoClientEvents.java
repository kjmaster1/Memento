package com.kjmaster.memento.event;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.kjmaster.memento.client.StatDefinition;
import com.kjmaster.memento.client.StatDefinitionManager;
import com.kjmaster.memento.client.tooltip.StatBarComponent;
import com.kjmaster.memento.component.ItemMetadata;
import com.kjmaster.memento.component.TrackerMap;
import com.kjmaster.memento.registry.ModDataComponents;
import com.mojang.datafixers.util.Either;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.RenderTooltipEvent;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

public class MementoClientEvents {

    // Cache to prevent recalculating tooltips every render frame.
    // TrackerMap is a Record, so it has a stable hashCode based on its content.
    private static final Cache<TrackerMap, List<MutableComponent>> TOOLTIP_CACHE = CacheBuilder.newBuilder()
            .maximumSize(1000)
            .expireAfterAccess(5, TimeUnit.MINUTES)
            .build();

    public static void clearCache() {
        TOOLTIP_CACHE.invalidateAll();
    }

    @SubscribeEvent
    public static void onTooltip(ItemTooltipEvent event) {
        if (!event.getItemStack().has(ModDataComponents.TRACKER_MAP) && !event.getItemStack().has(ModDataComponents.ITEM_METADATA)) {
            return;
        }

        if (Screen.hasShiftDown()) {
            event.getToolTip().add(Component.translatable("tooltip.memento.header")
                    .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));

            // Metadata Section
            if (event.getItemStack().has(ModDataComponents.ITEM_METADATA)) {
                ItemMetadata meta = event.getItemStack().get(ModDataComponents.ITEM_METADATA);
                if (meta != null && !meta.creatorName().isEmpty()) {
                    event.getToolTip().add(Component.translatable("tooltip.memento.created_by", meta.creatorName(), meta.createdOnWorldDay())
                            .withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.BOLD));
                }
            }

            // Stats Section (TEXT ONLY)
            TrackerMap trackers = event.getItemStack().get(ModDataComponents.TRACKER_MAP);
            if (trackers == null || trackers.stats().isEmpty()) return;

            try {
                // Use cache to avoid heavy string formatting and map lookups every frame
                List<MutableComponent> allLines = TOOLTIP_CACHE.get(trackers, () -> computeStatLines(trackers));

                // PAGINATION LOGIC
                // Ctrl+Shift = Show All
                // Shift = Show Top 5
                if (Screen.hasControlDown()) {
                    event.getToolTip().addAll(allLines);
                } else {
                    int limit = 5;
                    // Show top N
                    event.getToolTip().addAll(allLines.subList(0, Math.min(allLines.size(), limit)));

                    // Show Hint if truncated
                    if (allLines.size() > limit) {
                        int remaining = allLines.size() - limit;
                        event.getToolTip().add(Component.translatable("tooltip.memento.hold_ctrl_shift", remaining)
                                .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
                    }
                }

            } catch (ExecutionException e) {
                // Fallback in case of cache error
                event.getToolTip().addAll(computeStatLines(trackers));
            }

        } else {
            event.getToolTip().add(Component.translatable("tooltip.memento.hold_shift")
                    .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
        }
    }

    private static List<MutableComponent> computeStatLines(TrackerMap trackers) {
        List<MutableComponent> lines = new ArrayList<>();

        // 1. Sort stats by Value (Descending) to ensure "Top 5" are actually the biggest ones
        List<Map.Entry<ResourceLocation, Long>> sortedEntries = new ArrayList<>(trackers.stats().entrySet());
        sortedEntries.sort(Map.Entry.<ResourceLocation, Long>comparingByValue().reversed());

        for (Map.Entry<ResourceLocation, Long> entry : sortedEntries) {
            ResourceLocation statId = entry.getKey();
            Long rawValue = entry.getValue();

            StatDefinition def = StatDefinitionManager.get(statId);

            // SKIPPING BAR MODE: We handle bars in the GatherComponents event
            if (def.displayMode().orElse("text").equals("bar")) {
                continue;
            }

            String valueString = getValueString(rawValue, def);

            String translationKey = "stat." + statId.getNamespace() + "." + statId.getPath();
            ChatFormatting valueColor = def.color().orElse(ChatFormatting.WHITE);

            MutableComponent line = Component.translatable(translationKey)
                    .withStyle(ChatFormatting.GRAY)
                    .append(Component.literal(": "))
                    .append(Component.literal(valueString).withStyle(valueColor));

            lines.add(line);
        }
        return lines;
    }

    private static @NotNull String getValueString(Long rawValue, StatDefinition def) {
        double processedValue = rawValue * def.factor().orElse(1.0);
        String valueString;
        String type = def.formatType().orElse("integer");
        if (type.equals("decimal")) {
            valueString = String.format("%.2f", processedValue);
        } else {
            valueString = String.valueOf((long) processedValue);
        }
        if (def.suffix().isPresent()) {
            valueString += def.suffix().get();
        }
        return valueString;
    }

    @SubscribeEvent
    public static void onGatherTooltipComponents(RenderTooltipEvent.GatherComponents event) {
        if (!Screen.hasShiftDown()) return;

        if (!event.getItemStack().has(ModDataComponents.TRACKER_MAP)) return;

        TrackerMap trackers = event.getItemStack().get(ModDataComponents.TRACKER_MAP);
        if (trackers == null || trackers.stats().isEmpty()) return;

        for (Map.Entry<ResourceLocation, Long> entry : trackers.stats().entrySet()) {
            ResourceLocation statId = entry.getKey();
            Long rawValue = entry.getValue();

            StatDefinition def = StatDefinitionManager.get(statId);

            // ONLY HANDLE BAR MODE
            if (def.displayMode().orElse("text").equals("bar")) {
                double max = def.maxValue().orElse(100.0);
                // Apply factor if needed (e.g. converting cm to m before comparing to max)
                double processedValue = rawValue * def.factor().orElse(1.0);

                float progress = (float) (processedValue / max);

                Integer colorVal = def.color().orElse(ChatFormatting.GREEN).getColor();
                int finalColor = (colorVal != null) ? colorVal : 0x55FF55; // Default Green/White fallback

                // Add the Bar Component
                event.getTooltipElements().add(Either.right(new StatBarComponent(progress, finalColor)));
            }
        }
    }
}