package com.kjmaster.memento.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.kjmaster.memento.Memento;
import com.mojang.serialization.JsonOps;
import net.minecraft.core.HolderLookup;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

public class StatLoreManager extends SimpleJsonResourceReloadListener {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private static final List<StatLoreRule> RULES = new ArrayList<>();
    private final HolderLookup.Provider registries;

    public StatLoreManager(HolderLookup.Provider registries) {
        super(GSON, "stat_lore");
        this.registries = registries;
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> object, @NotNull ResourceManager resourceManager, @NotNull ProfilerFiller profiler) {
        RULES.clear();
        int count = 0;

        RegistryOps<JsonElement> registryOps = RegistryOps.create(JsonOps.INSTANCE, this.registries);

        for (Map.Entry<ResourceLocation, JsonElement> entry : object.entrySet()) {
            StatLoreRule.CODEC.parse(registryOps, entry.getValue())
                    .resultOrPartial(err -> Memento.LOGGER.error("Failed to parse stat lore rule {}: {}", entry.getKey(), err))
                    .ifPresent(RULES::add);
            count++;
        }

        // Sort by priority (Higher priority first)
        RULES.sort(Comparator.comparingInt(StatLoreRule::priority).reversed());

        Memento.LOGGER.info("Loaded {} stat lore rules", count);
    }

    public static List<StatLoreRule> getAllRules() {
        return RULES;
    }
}