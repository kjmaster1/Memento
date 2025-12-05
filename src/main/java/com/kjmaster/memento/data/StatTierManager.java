package com.kjmaster.memento.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.kjmaster.memento.Memento;
import com.mojang.serialization.JsonOps;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
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

public class StatTierManager extends SimpleJsonResourceReloadListener {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private static final List<StatTierRule> GLOBAL_RULES = new ArrayList<>();
    private static final Map<Item, List<StatTierRule>> INDEXED_RULES = new HashMap<>();
    private final HolderLookup.Provider registries;

    public StatTierManager(HolderLookup.Provider registries) {
        super(GSON, "stat_tiers");
        this.registries = registries;
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> object, @NotNull ResourceManager resourceManager, @NotNull ProfilerFiller profiler) {
        GLOBAL_RULES.clear();
        INDEXED_RULES.clear();
        RegistryOps<JsonElement> registryOps = RegistryOps.create(JsonOps.INSTANCE, this.registries);

        for (Map.Entry<ResourceLocation, JsonElement> entry : object.entrySet()) {
            StatTierRule.CODEC.parse(registryOps, entry.getValue())
                    .resultOrPartial(err -> Memento.LOGGER.error("Failed to parse tier rule {}: {}", entry.getKey(), err))
                    .ifPresent(rule -> {
                        if (rule.items().isPresent() && rule.items().get().size() > 0) {
                            for (Holder<Item> holder : rule.items().get()) {
                                Item item = holder.value();
                                INDEXED_RULES.computeIfAbsent(item, k -> new ArrayList<>()).add(rule);
                            }
                        } else {
                            GLOBAL_RULES.add(rule);
                        }
                    });
        }
        Memento.LOGGER.info("Loaded {} stat tier rules", GLOBAL_RULES.size() + INDEXED_RULES.size());
    }

    public static List<StatTierRule> getApplicableRules(ItemStack stack) {
        List<StatTierRule> results = new ArrayList<>(GLOBAL_RULES);
        if (!stack.isEmpty()) {
            List<StatTierRule> indexed = INDEXED_RULES.get(stack.getItem());
            if (indexed != null) results.addAll(indexed);
        }

        // Filter out non-matching predicates
        List<StatTierRule> matching = new ArrayList<>();
        for (StatTierRule rule : results) {
            if (rule.item().test(stack)) {
                matching.add(rule);
            }
        }
        return matching;
    }
}