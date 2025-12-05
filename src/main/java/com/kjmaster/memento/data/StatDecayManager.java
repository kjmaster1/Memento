package com.kjmaster.memento.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.kjmaster.memento.Memento;
import com.mojang.serialization.JsonOps;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.RegistryOps;
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

public class StatDecayManager extends SimpleJsonResourceReloadListener {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private static final Map<StatDecayRule.Trigger, List<StatDecayRule>> GLOBAL_RULES = new HashMap<>();
    private static final Map<StatDecayRule.Trigger, Map<Item, List<StatDecayRule>>> INDEXED_RULES = new HashMap<>();
    private final HolderLookup.Provider registries;

    public StatDecayManager(HolderLookup.Provider registries) {
        super(GSON, "stat_decay");
        this.registries = registries;
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> object, @NotNull ResourceManager resourceManager, @NotNull ProfilerFiller profiler) {
        GLOBAL_RULES.clear();
        INDEXED_RULES.clear();
        int count = 0;
        RegistryOps<JsonElement> registryOps = RegistryOps.create(JsonOps.INSTANCE, this.registries);

        for (Map.Entry<ResourceLocation, JsonElement> entry : object.entrySet()) {
            StatDecayRule.CODEC.parse(registryOps, entry.getValue())
                    .resultOrPartial(err -> Memento.LOGGER.error("Failed to parse decay rule {}: {}", entry.getKey(), err))
                    .ifPresent(rule -> {
                        if (rule.items().isPresent() && !rule.items().get().isEmpty()) {
                            Map<Item, List<StatDecayRule>> typeMap = INDEXED_RULES.computeIfAbsent(rule.trigger(), k -> new HashMap<>());
                            for (ResourceLocation itemId : rule.items().get()) {
                                Item item = BuiltInRegistries.ITEM.get(itemId);
                                typeMap.computeIfAbsent(item, k -> new ArrayList<>()).add(rule);
                            }
                        } else {
                            GLOBAL_RULES.computeIfAbsent(rule.trigger(), k -> new ArrayList<>()).add(rule);
                        }
                    });
            count++;
        }
        Memento.LOGGER.info("Loaded {} stat decay rules", count);
    }

    public static List<StatDecayRule> getRules(StatDecayRule.Trigger trigger, ItemStack stack) {
        List<StatDecayRule> results = new ArrayList<>();
        List<StatDecayRule> globals = GLOBAL_RULES.get(trigger);
        if (globals != null) results.addAll(globals);

        if (!stack.isEmpty()) {
            Map<Item, List<StatDecayRule>> typeIndex = INDEXED_RULES.get(trigger);
            if (typeIndex != null) {
                List<StatDecayRule> indexed = typeIndex.get(stack.getItem());
                if (indexed != null) results.addAll(indexed);
            }
        }
        return results;
    }
}