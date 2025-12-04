package com.kjmaster.memento.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.kjmaster.memento.Memento;
import com.mojang.serialization.JsonOps;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class StatTriggerManager extends SimpleJsonResourceReloadListener {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    // Global triggers (linear scan required)
    private static final Map<StatTrigger.TriggerType, List<StatTrigger>> GLOBAL_TRIGGERS = new HashMap<>();

    // Indexed triggers (O(1) lookup by Item)
    private static final Map<StatTrigger.TriggerType, Map<Item, List<StatTrigger>>> INDEXED_TRIGGERS = new HashMap<>();

    public StatTriggerManager() {
        super(GSON, "stat_triggers");
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> object, @NotNull ResourceManager resourceManager, @NotNull ProfilerFiller profiler) {
        GLOBAL_TRIGGERS.clear();
        INDEXED_TRIGGERS.clear();

        int count = 0;

        for (Map.Entry<ResourceLocation, JsonElement> entry : object.entrySet()) {
            StatTrigger.CODEC.parse(JsonOps.INSTANCE, entry.getValue())
                    .resultOrPartial(err -> Memento.LOGGER.error("Failed to parse trigger {}: {}", entry.getKey(), err))
                    .ifPresent(trigger -> {
                        if (trigger.optimizedItems().isPresent() && !trigger.optimizedItems().get().isEmpty()) {
                            // Index this trigger for each specified item
                            Map<Item, List<StatTrigger>> typeMap = INDEXED_TRIGGERS.computeIfAbsent(trigger.type(), k -> new HashMap<>());

                            for (ResourceLocation itemId : trigger.optimizedItems().get()) {
                                Item item = BuiltInRegistries.ITEM.get(itemId);
                                // Note: We don't check for AIR here because strictly invalid items might just be mod-missing items,
                                // essentially harmless keys in a map.
                                typeMap.computeIfAbsent(item, k -> new ArrayList<>()).add(trigger);
                            }
                            // Also add to global? No, if it's indexed, we only want it from index.
                        } else {
                            // Fallback to global list
                            GLOBAL_TRIGGERS.computeIfAbsent(trigger.type(), k -> new ArrayList<>()).add(trigger);
                        }
                    });
            count++;
        }
        Memento.LOGGER.info("Loaded {} stat triggers ({} indexed)", count, count - GLOBAL_TRIGGERS.values().stream().mapToInt(List::size).sum());
    }

    /**
     * Retrieves all triggers applicable to the given context.
     * Combines O(1) indexed triggers with O(N) global triggers.
     */
    public static List<StatTrigger> getTriggers(StatTrigger.TriggerType type, ItemStack stack) {
        List<StatTrigger> results = new ArrayList<>();

        // 1. Add Global Triggers
        List<StatTrigger> globals = GLOBAL_TRIGGERS.get(type);
        if (globals != null) {
            results.addAll(globals);
        }

        // 2. Add Indexed Triggers
        if (!stack.isEmpty()) {
            Map<Item, List<StatTrigger>> typeIndex = INDEXED_TRIGGERS.get(type);
            if (typeIndex != null) {
                List<StatTrigger> indexed = typeIndex.get(stack.getItem());
                if (indexed != null) {
                    results.addAll(indexed);
                }
            }
        }

        return results;
    }
}