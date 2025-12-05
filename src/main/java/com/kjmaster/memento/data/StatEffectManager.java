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

public class StatEffectManager extends SimpleJsonResourceReloadListener {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private static final Map<StatEffect.EffectContext, List<StatEffect>> GLOBAL_RULES = new HashMap<>();
    private static final Map<StatEffect.EffectContext, Map<Item, List<StatEffect>>> INDEXED_RULES = new HashMap<>();

    public StatEffectManager() {
        super(GSON, "stat_effects");
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> object, @NotNull ResourceManager resourceManager, @NotNull ProfilerFiller profiler) {
        GLOBAL_RULES.clear();
        INDEXED_RULES.clear();
        int count = 0;

        for (Map.Entry<ResourceLocation, JsonElement> entry : object.entrySet()) {
            StatEffect.CODEC.parse(JsonOps.INSTANCE, entry.getValue())
                    .resultOrPartial(err -> Memento.LOGGER.error("Failed to parse stat effect {}: {}", entry.getKey(), err))
                    .ifPresent(rule -> {
                        if (rule.items().isPresent() && !rule.items().get().isEmpty()) {
                            Map<Item, List<StatEffect>> contextMap = INDEXED_RULES.computeIfAbsent(rule.context(), k -> new HashMap<>());
                            for (ResourceLocation itemId : rule.items().get()) {
                                Item item = BuiltInRegistries.ITEM.get(itemId);
                                contextMap.computeIfAbsent(item, k -> new ArrayList<>()).add(rule);
                            }
                        } else {
                            GLOBAL_RULES.computeIfAbsent(rule.context(), k -> new ArrayList<>()).add(rule);
                        }
                    });
            count++;
        }
        Memento.LOGGER.info("Loaded {} stat effect rules", count);
    }

    public static List<StatEffect> getRules(StatEffect.EffectContext context, ItemStack stack) {
        List<StatEffect> results = new ArrayList<>();
        List<StatEffect> globals = GLOBAL_RULES.get(context);
        if (globals != null) results.addAll(globals);

        if (!stack.isEmpty()) {
            Map<Item, List<StatEffect>> contextMap = INDEXED_RULES.get(context);
            if (contextMap != null) {
                List<StatEffect> indexed = contextMap.get(stack.getItem());
                if (indexed != null) results.addAll(indexed);
            }
        }
        return results;
    }

    public static List<StatEffect> getAllRules() {
        List<StatEffect> all = new ArrayList<>();
        GLOBAL_RULES.values().forEach(all::addAll);
        INDEXED_RULES.values().forEach(map -> map.values().forEach(all::addAll));
        return all;
    }
}