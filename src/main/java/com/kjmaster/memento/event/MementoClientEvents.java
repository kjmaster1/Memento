package com.kjmaster.memento.event;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.kjmaster.memento.api.MementoAPI;
import com.kjmaster.memento.client.ClientInputHandler;
import com.kjmaster.memento.client.StatDefinition;
import com.kjmaster.memento.client.StatDefinitionManager;
import com.kjmaster.memento.client.tooltip.StatBarComponent;
import com.kjmaster.memento.client.tooltip.StatIconComponent;
import com.kjmaster.memento.component.ItemMetadata;
import com.kjmaster.memento.component.TrackerMap;
import com.kjmaster.memento.data.StatEchoManager;
import com.kjmaster.memento.data.StatEchoRule;
import com.kjmaster.memento.registry.ModDataComponents;
import com.kjmaster.memento.registry.ModStats;
import com.mojang.datafixers.util.Either;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.RenderTooltipEvent;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

public class MementoClientEvents {

    // Cache to prevent recalculating tooltips every render frame.
    private static final Cache<TrackerMap, List<MutableComponent>> TOOLTIP_CACHE = CacheBuilder.newBuilder()
            .maximumSize(1000)
            .expireAfterAccess(5, TimeUnit.MINUTES)
            .build();

    public static void clearCache() {
        TOOLTIP_CACHE.invalidateAll();
    }

    @SubscribeEvent
    public static void onTooltip(ItemTooltipEvent event) {
        // 1. ECHO TOOLTIPS (Always visible if active, regardless of Memento data presence)
        // This allows items to show potential or active effects even before they track stats,
        // or if they are just special items.
        addEchoTooltips(event.getItemStack(), event.getToolTip());

        if (!event.getItemStack().has(ModDataComponents.TRACKER_MAP) && !event.getItemStack().has(ModDataComponents.ITEM_METADATA)) {
            return;
        }

        if (Screen.hasShiftDown()) {
            event.getToolTip().add(Component.translatable("tooltip.memento.header")
                    .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));

            // Metadata Section
            if (event.getItemStack().has(ModDataComponents.ITEM_METADATA)) {
                ItemMetadata meta = event.getItemStack().get(ModDataComponents.ITEM_METADATA);
                if (meta != null) {
                    // 1. Original Name (if different)
                    String currentName = event.getItemStack().getHoverName().getString();
                    if (!meta.originalName().isEmpty() && !currentName.equals(meta.originalName())) {
                        event.getToolTip().add(Component.translatable("tooltip.memento.original_name", meta.originalName())
                                .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
                    }

                    // 2. Creator
                    if (!meta.creatorName().isEmpty()) {
                        event.getToolTip().add(Component.translatable("tooltip.memento.created_by", meta.creatorName(), meta.createdOnWorldDay())
                                .withStyle(ChatFormatting.DARK_PURPLE));
                    }

                    // 3. Previous Owners
                    for (ItemMetadata.OwnerEntry owner : meta.wieldedBy()) {
                        event.getToolTip().add(Component.translatable("tooltip.memento.wielded_by", owner.ownerName(), owner.dayWielded())
                                .withStyle(ChatFormatting.LIGHT_PURPLE));
                    }
                }
            }

            // Stats Section
            TrackerMap trackers = event.getItemStack().get(ModDataComponents.TRACKER_MAP);
            if (trackers == null || trackers.stats().isEmpty()) return;

            // IF COMPACT MODE: Do NOT add text lines here. We will add Icons in GatherTooltipComponents.
            if (ClientInputHandler.isCompactMode) {
                event.getToolTip().add(Component.translatable("tooltip.memento.compact_mode_active")
                        .withStyle(ChatFormatting.DARK_AQUA, ChatFormatting.ITALIC));
                return;
            }

            // IF LORE MODE: Add text lines normally.
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

    private static void addEchoTooltips(ItemStack stack, List<Component> tooltip) {
        if (stack.isEmpty()) return;

        for (StatEchoRule.Trigger trigger : StatEchoRule.Trigger.values()) {
            List<StatEchoRule> rules = StatEchoManager.getRules(trigger);
            if (rules == null || rules.isEmpty()) continue;

            for (StatEchoRule rule : rules) {
                if (isRuleActive(stack, rule)) {
                    tooltip.add(formatEchoTooltip(rule));
                }
            }
        }
    }

    private static boolean isRuleActive(ItemStack stack, StatEchoRule rule) {
        // 1. Check Item Filter (Optimized Items)
        if (rule.items().isPresent()) {
            if (!rule.items().get().contains(stack.getItemHolder())) {
                return false;
            }
        }

        // 2. Check Stat Conditions
        for (StatEchoRule.Condition condition : rule.conditions()) {
            long val = MementoAPI.getStat(stack, condition.stat());
            if (val < condition.min()) {
                return false;
            }
        }

        return true;
    }

    private static Component formatEchoTooltip(StatEchoRule rule) {
        // Resolve Action Text
        MutableComponent actionText;
        if (rule.action() == StatEchoRule.Action.SPAWN_ENTITY && rule.parameters().id().isPresent()) {
            // Special case for Entity: "Spawns Zombie"
            ResourceLocation entityId = rule.parameters().id().get();
            String entityName = BuiltInRegistries.ENTITY_TYPE.get(entityId).getDescription().getString();
            actionText = Component.translatable("memento.echo.action.spawn_entity", entityName);
        } else {
            actionText = Component.translatable("memento.echo.action." + rule.action().getSerializedName());
        }

        // Resolve Trigger Text
        Component triggerText = Component.translatable("memento.echo.trigger." + rule.trigger().getSerializedName());

        // Combine: "Passive: [Action] [Trigger]"
        return Component.translatable("memento.echo.passive", actionText, triggerText)
                .withStyle(ChatFormatting.AQUA); // Blue/Magic style
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

        // Sort just like text mode for consistency
        List<Map.Entry<ResourceLocation, Long>> sortedEntries = new ArrayList<>(trackers.stats().entrySet());
        sortedEntries.sort(Map.Entry.<ResourceLocation, Long>comparingByValue().reversed());

        // Limit Compact View too? Maybe let's show more since they are small.
        // Let's stick to the same logic: Top 5 unless Ctrl is held.
        List<Map.Entry<ResourceLocation, Long>> entriesToShow;
        if (Screen.hasControlDown()) {
            entriesToShow = sortedEntries;
        } else {
            entriesToShow = sortedEntries.subList(0, Math.min(sortedEntries.size(), 5));
        }

        for (Map.Entry<ResourceLocation, Long> entry : entriesToShow) {
            ResourceLocation statId = entry.getKey();
            Long rawValue = entry.getValue();

            StatDefinition def = StatDefinitionManager.get(statId);

            // 1. Handle BAR Mode (Always renders regardless of Compact/Text)
            if (def.displayMode().orElse("text").equals("bar")) {
                double max = def.maxValue().orElse(100.0);
                double processedValue = rawValue * def.factor().orElse(1.0);
                float progress = (float) (processedValue / max);
                Integer colorVal = def.color().orElse(ChatFormatting.GREEN).getColor();
                int finalColor = (colorVal != null) ? colorVal : 0x55FF55;
                event.getTooltipElements().add(Either.right(new StatBarComponent(progress, finalColor)));
                continue;
            }

            // 2. Handle COMPACT Mode (Icons)
            if (ClientInputHandler.isCompactMode) {
                ItemStack iconStack = getIconForStat(statId, def);
                String valueString = getValueString(rawValue, def);
                Integer colorVal = def.color().orElse(ChatFormatting.WHITE).getColor();
                int finalColor = (colorVal != null) ? colorVal : 0xFFFFFF;

                event.getTooltipElements().add(Either.right(new StatIconComponent(iconStack, valueString, finalColor)));
            }
        }
    }

    private static ItemStack getIconForStat(ResourceLocation statId, StatDefinition def) {
        // 1. Explicit Icon from JSON
        if (def.icon().isPresent()) {
            return BuiltInRegistries.ITEM.get(def.icon().get()).getDefaultInstance();
        }

        // 2. Hardcoded Defaults for Memento Core Stats
        if (statId.equals(ModStats.BLOCKS_BROKEN)) return new ItemStack(Items.IRON_PICKAXE);
        if (statId.equals(ModStats.ENTITIES_KILLED)) return new ItemStack(Items.IRON_SWORD);
        if (statId.equals(ModStats.DISTANCE_FLOWN)) return new ItemStack(Items.ELYTRA);
        if (statId.equals(ModStats.DAMAGE_TAKEN)) return new ItemStack(Items.SHIELD); // Shield or Golden Apple?
        if (statId.equals(ModStats.CROPS_HARVESTED)) return new ItemStack(Items.IRON_HOE);
        if (statId.equals(ModStats.DAMAGE_BLOCKED)) return new ItemStack(Items.SHIELD);
        if (statId.equals(ModStats.SHOTS_FIRED)) return new ItemStack(Items.BOW);
        if (statId.equals(ModStats.LONGEST_SHOT)) return new ItemStack(Items.SPYGLASS);
        if (statId.equals(ModStats.ITEMS_CAUGHT)) return new ItemStack(Items.FISHING_ROD);
        if (statId.equals(ModStats.FIRES_STARTED)) return new ItemStack(Items.FLINT_AND_STEEL);
        if (statId.equals(ModStats.MOBS_SHEARED)) return new ItemStack(Items.SHEARS);

        // 3. Fallback
        return new ItemStack(Items.PAPER);
    }
}