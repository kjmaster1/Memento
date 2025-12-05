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

public class StatProjectileLogicManager extends SimpleJsonResourceReloadListener {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    // Global rules (linear scan required)
    private static final List<StatProjectileLogic> GLOBAL_RULES = new ArrayList<>();

    // Indexed rules (O(1) lookup by Item)
    private static final Map<Item, List<StatProjectileLogic>> INDEXED_RULES = new HashMap<>();

    public StatProjectileLogicManager() {
        super(GSON, "stat_projectile_logic");
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> object, @NotNull ResourceManager resourceManager, @NotNull ProfilerFiller profiler) {
        GLOBAL_RULES.clear();
        INDEXED_RULES.clear();
        int count = 0;

        for (Map.Entry<ResourceLocation, JsonElement> entry : object.entrySet()) {
            StatProjectileLogic.CODEC.parse(JsonOps.INSTANCE, entry.getValue())
                    .resultOrPartial(err -> Memento.LOGGER.error("Failed to parse projectile logic rule {}: {}", entry.getKey(), err))
                    .ifPresent(rule -> {
                        if (rule.optimizedItems().isPresent() && !rule.optimizedItems().get().isEmpty()) {
                            // Index this rule
                            for (ResourceLocation itemId : rule.optimizedItems().get()) {
                                Item item = BuiltInRegistries.ITEM.get(itemId);
                                INDEXED_RULES.computeIfAbsent(item, k -> new ArrayList<>()).add(rule);
                            }
                        } else {
                            // Fallback to global
                            GLOBAL_RULES.add(rule);
                        }
                    });
            count++;
        }
        Memento.LOGGER.info("Loaded {} projectile logic rules", count);
    }

    public static List<StatProjectileLogic> getRules(ItemStack stack) {
        List<StatProjectileLogic> results = new ArrayList<>(GLOBAL_RULES);

        if (!stack.isEmpty()) {
            List<StatProjectileLogic> indexed = INDEXED_RULES.get(stack.getItem());
            if (indexed != null) {
                results.addAll(indexed);
            }
        }
        return results;
    }

    public static List<StatProjectileLogic> getAllRules() {
        List<StatProjectileLogic> all = new ArrayList<>(GLOBAL_RULES);
        INDEXED_RULES.values().forEach(all::addAll);
        return all;
    }
}