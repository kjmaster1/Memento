package com.kjmaster.memento.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.kjmaster.memento.Memento;
import com.mojang.serialization.JsonOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;

import java.util.HashMap;
import java.util.Map;

public class StatBehaviorManager extends SimpleJsonResourceReloadListener {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private static final Map<ResourceLocation, StatBehavior.MergeStrategy> STRATEGIES = new HashMap<>();

    public StatBehaviorManager() {
        super(GSON, "stat_behaviors");
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> object, ResourceManager resourceManager, ProfilerFiller profiler) {
        STRATEGIES.clear();

        for (Map.Entry<ResourceLocation, JsonElement> entry : object.entrySet()) {
            StatBehavior.CODEC.parse(JsonOps.INSTANCE, entry.getValue())
                    .resultOrPartial(err -> Memento.LOGGER.error("Failed to parse stat behavior {}: {}", entry.getKey(), err))
                    .ifPresent(behavior -> STRATEGIES.put(behavior.stat(), behavior.mergeStrategy()));
        }
        Memento.LOGGER.info("Loaded {} stat behaviors", STRATEGIES.size());
    }

    public static StatBehavior.MergeStrategy getStrategy(ResourceLocation stat) {
        return STRATEGIES.getOrDefault(stat, StatBehavior.MergeStrategy.SUM);
    }
}