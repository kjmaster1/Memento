package com.kjmaster.memento.client;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.kjmaster.memento.Memento;
import com.kjmaster.memento.component.TrackerMap;
import com.kjmaster.memento.data.StatLoreManager;
import com.kjmaster.memento.data.StatLoreRule;
import com.kjmaster.memento.registry.ModDataComponents;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

public class ClientLoreEvents {

    // Cache key: TrackerMap content + Item Display Name (to handle renaming via Anvil)
    private record LoreCacheKey(TrackerMap stats, String itemName) {
    }

    private static final Cache<LoreCacheKey, CachedLoreResult> LORE_CACHE = CacheBuilder.newBuilder()
            .maximumSize(1000)
            .expireAfterAccess(5, TimeUnit.MINUTES)
            .build();

    private record CachedLoreResult(List<Component> loreLines, Component modifiedName) {
    }

    @SubscribeEvent
    public static void onItemTooltip(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();
        if (!stack.has(ModDataComponents.TRACKER_MAP)) return;

        TrackerMap trackers = stack.get(ModDataComponents.TRACKER_MAP);
        String currentName = stack.getHoverName().getString();

        try {
            LoreCacheKey key = new LoreCacheKey(trackers, currentName);
            CachedLoreResult result = LORE_CACHE.get(key, () -> computeLore(stack, trackers, event.getItemStack().getHoverName()));

            // 1. Apply Lore Lines
            if (!result.loreLines.isEmpty()) {
                event.getToolTip().addAll(result.loreLines);
            }

            // 2. Apply Name Changes (Visual Only)
            // We replace the first line of the tooltip, which represents the item name
            if (result.modifiedName != null && !event.getToolTip().isEmpty()) {
                event.getToolTip().set(0, result.modifiedName);
            }

        } catch (ExecutionException e) {
            Memento.LOGGER.error("Failed to compute dynamic lore", e);
        }
    }

    private static CachedLoreResult computeLore(ItemStack stack, TrackerMap trackers, Component originalName) {
        List<Component> loreToAdd = new ArrayList<>();
        MutableComponent newName = originalName.copy();
        boolean nameModified = false;

        for (StatLoreRule rule : StatLoreManager.getAllRules()) {

            if (rule.items().isPresent()) {
                if (!rule.items().get().contains(stack.getItemHolder())) {
                    continue;
                }
            }

            boolean conditionMet = true;

            // Check all conditions
            for (StatLoreRule.Condition cond : rule.conditions()) {
                if (trackers.getValue(cond.stat()) < cond.min()) {
                    conditionMet = false;
                    break;
                }
            }

            if (conditionMet) {
                // Unlock: Show Lore & Modify Name
                loreToAdd.addAll(rule.loreLines());

                if (rule.namePrefix().isPresent()) {
                    newName = rule.namePrefix().get().copy().append(" ").append(newName);
                    nameModified = true;
                }
                if (rule.nameSuffix().isPresent()) {
                    newName = newName.append(" ").append(rule.nameSuffix().get());
                    nameModified = true;
                }

            } else if (rule.hidden()) {
                // Locked but Hidden: Show "???"
                loreToAdd.add(Component.literal("???").withStyle(ChatFormatting.OBFUSCATED, ChatFormatting.DARK_GRAY));
            }
        }

        return new CachedLoreResult(loreToAdd, nameModified ? newName : null);
    }

    public static void clearCache() {
        LORE_CACHE.invalidateAll();
    }
}