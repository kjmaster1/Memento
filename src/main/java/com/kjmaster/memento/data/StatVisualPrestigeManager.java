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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class StatVisualPrestigeManager extends SimpleJsonResourceReloadListener {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    private static final List<StatVisualPrestige> GLOBAL_RULES = new ArrayList<>();
    private static final Map<Item, List<StatVisualPrestige>> INDEXED_RULES = new HashMap<>();

    public StatVisualPrestigeManager() {
        super(GSON, "stat_visual_prestige");
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> object, ResourceManager resourceManager, ProfilerFiller profiler) {
        GLOBAL_RULES.clear();
        INDEXED_RULES.clear();
        int count = 0;

        for (Map.Entry<ResourceLocation, JsonElement> entry : object.entrySet()) {
            StatVisualPrestige.CODEC.parse(JsonOps.INSTANCE, entry.getValue())
                    .resultOrPartial(err -> Memento.LOGGER.error("Failed to parse visual prestige rule {}: {}", entry.getKey(), err))
                    .ifPresent(rule -> {
                        if (rule.optimizedItems().isPresent() && !rule.optimizedItems().get().isEmpty()) {
                            for (ResourceLocation itemId : rule.optimizedItems().get()) {
                                Item item = BuiltInRegistries.ITEM.get(itemId);
                                INDEXED_RULES.computeIfAbsent(item, k -> new ArrayList<>()).add(rule);
                            }
                        } else {
                            GLOBAL_RULES.add(rule);
                        }
                    });
            count++;
        }
        Memento.LOGGER.info("Loaded {} visual prestige rules", count);
    }

    public static List<StatVisualPrestige> getRules(ItemStack stack) {
        List<StatVisualPrestige> results = new ArrayList<>(GLOBAL_RULES);

        if (!stack.isEmpty()) {
            List<StatVisualPrestige> indexed = INDEXED_RULES.get(stack.getItem());
            if (indexed != null) {
                results.addAll(indexed);
            }
        }
        return results;
    }

    // Kept for backward compatibility if needed, though getRules(stack) is preferred
    public static List<StatVisualPrestige> getAllRules() {
        List<StatVisualPrestige> all = new ArrayList<>(GLOBAL_RULES);
        INDEXED_RULES.values().forEach(all::addAll);
        return all;
    }
}