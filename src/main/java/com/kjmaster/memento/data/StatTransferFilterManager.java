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

public class StatTransferFilterManager extends SimpleJsonResourceReloadListener {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private static final List<StatTransferFilter> GLOBAL_RULES = new ArrayList<>();
    private static final Map<Item, List<StatTransferFilter>> INDEXED_RULES = new HashMap<>();
    private final HolderLookup.Provider registries;

    public StatTransferFilterManager(HolderLookup.Provider registries) {
        super(GSON, "memento/stat_transfer_filters");
        this.registries = registries;
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> object, @NotNull ResourceManager resourceManager, @NotNull ProfilerFiller profiler) {
        GLOBAL_RULES.clear();
        INDEXED_RULES.clear();
        RegistryOps<JsonElement> registryOps = RegistryOps.create(JsonOps.INSTANCE, this.registries);

        for (Map.Entry<ResourceLocation, JsonElement> entry : object.entrySet()) {
            StatTransferFilter.CODEC.parse(registryOps, entry.getValue())
                    .resultOrPartial(err -> Memento.LOGGER.error("Failed to parse transfer filter {}: {}", entry.getKey(), err))
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
        Memento.LOGGER.info("Loaded {} stat transfer filters", GLOBAL_RULES.size() + INDEXED_RULES.size());
    }

    public static boolean isAllowed(ItemStack stack, ResourceLocation statId) {
        List<StatTransferFilter> rules = new ArrayList<>(GLOBAL_RULES);
        if (!stack.isEmpty()) {
            List<StatTransferFilter> indexed = INDEXED_RULES.get(stack.getItem());
            if (indexed != null) rules.addAll(indexed);
        }

        for (StatTransferFilter rule : rules) {
            if (rule.itemMatcher().test(stack)) {
                if (rule.bannedStats().isPresent() && rule.bannedStats().get().contains(statId)) return false;
                if (rule.allowedStats().isPresent()) {
                    if (!rule.allowedStats().get().contains(statId)) return false;
                }
            }
        }
        return true;
    }
}