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

public class StatMasteryManager extends SimpleJsonResourceReloadListener {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private static final List<StatMastery> GLOBAL_RULES = new ArrayList<>();
    private static final Map<Item, List<StatMastery>> INDEXED_RULES = new HashMap<>();

    public StatMasteryManager() {
        super(GSON, "stat_mastery");
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> object, @NotNull ResourceManager resourceManager, @NotNull ProfilerFiller profiler) {
        GLOBAL_RULES.clear();
        INDEXED_RULES.clear();
        int count = 0;

        for (Map.Entry<ResourceLocation, JsonElement> entry : object.entrySet()) {
            StatMastery.CODEC.parse(JsonOps.INSTANCE, entry.getValue())
                    .resultOrPartial(err -> Memento.LOGGER.error("Failed to parse mastery rule {}: {}", entry.getKey(), err))
                    .ifPresent(rule -> {
                        if (rule.items().isPresent() && !rule.items().get().isEmpty()) {
                            for (ResourceLocation itemId : rule.items().get()) {
                                Item item = BuiltInRegistries.ITEM.get(itemId);
                                INDEXED_RULES.computeIfAbsent(item, k -> new ArrayList<>()).add(rule);
                            }
                        } else {
                            GLOBAL_RULES.add(rule);
                        }
                    });
            count++;
        }
        Memento.LOGGER.info("Loaded {} stat mastery rules", count);
    }

    public static List<StatMastery> getRules(ItemStack stack) {
        List<StatMastery> results = new ArrayList<>(GLOBAL_RULES);
        if (!stack.isEmpty()) {
            List<StatMastery> indexed = INDEXED_RULES.get(stack.getItem());
            if (indexed != null) results.addAll(indexed);
        }
        return results;
    }

    public static List<StatMastery> getAllRules() {
        List<StatMastery> all = new ArrayList<>(GLOBAL_RULES);
        INDEXED_RULES.values().forEach(all::addAll);
        return all;
    }
}