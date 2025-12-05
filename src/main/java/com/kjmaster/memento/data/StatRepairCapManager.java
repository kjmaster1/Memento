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

public class StatRepairCapManager extends SimpleJsonResourceReloadListener {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private static final List<StatRepairCap> GLOBAL_RULES = new ArrayList<>();
    private static final Map<Item, List<StatRepairCap>> INDEXED_RULES = new HashMap<>();

    public StatRepairCapManager() {
        super(GSON, "stat_repair_caps");
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> object, @NotNull ResourceManager resourceManager, @NotNull ProfilerFiller profiler) {
        GLOBAL_RULES.clear();
        INDEXED_RULES.clear();
        int count = 0;

        for (Map.Entry<ResourceLocation, JsonElement> entry : object.entrySet()) {
            StatRepairCap.CODEC.parse(JsonOps.INSTANCE, entry.getValue())
                    .resultOrPartial(err -> Memento.LOGGER.error("Failed to parse repair cap rule {}: {}", entry.getKey(), err))
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
        Memento.LOGGER.info("Loaded {} repair cap rules", count);
    }

    public static List<StatRepairCap> getRules(ItemStack stack) {
        List<StatRepairCap> results = new ArrayList<>(GLOBAL_RULES);
        if (!stack.isEmpty()) {
            List<StatRepairCap> indexed = INDEXED_RULES.get(stack.getItem());
            if (indexed != null) results.addAll(indexed);
        }
        return results;
    }

    public static List<StatRepairCap> getAllRules() {
        List<StatRepairCap> all = new ArrayList<>(GLOBAL_RULES);
        INDEXED_RULES.values().forEach(all::addAll);
        return all;
    }
}