package com.kjmaster.memento.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.kjmaster.memento.Memento;
import com.kjmaster.memento.event.MementoClientEvents;
import com.mojang.serialization.JsonOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

public class StatDefinitionManager extends SimpleJsonResourceReloadListener {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private static final Map<ResourceLocation, StatDefinition> DEFINITIONS = new HashMap<>();

    public StatDefinitionManager() {
        super(GSON, "stat_definitions");
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> object, @NotNull ResourceManager resourceManager, ProfilerFiller profiler) {
        DEFINITIONS.clear();

        for (Map.Entry<ResourceLocation, JsonElement> entry : object.entrySet()) {
            ResourceLocation statId = entry.getKey();
            StatDefinition.CODEC.parse(JsonOps.INSTANCE, entry.getValue())
                    .resultOrPartial(err -> Memento.LOGGER.error("Failed to parse stat definition for {}: {}", statId, err))
                    .ifPresent(def -> DEFINITIONS.put(statId, def));
        }
        Memento.LOGGER.info("Loaded {} stat definitions", DEFINITIONS.size());

        MementoClientEvents.clearCache();
    }

    public static StatDefinition get(ResourceLocation statId) {
        return DEFINITIONS.getOrDefault(statId, StatDefinition.DEFAULT);
    }
}