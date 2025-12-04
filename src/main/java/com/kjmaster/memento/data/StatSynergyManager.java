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
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class StatSynergyManager extends SimpleJsonResourceReloadListener {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private static final List<StatSynergy> SYNERGIES = new ArrayList<>();

    public StatSynergyManager() {
        super(GSON, "stat_synergies");
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> object, @NotNull ResourceManager resourceManager, @NotNull ProfilerFiller profiler) {
        SYNERGIES.clear();
        int count = 0;

        for (Map.Entry<ResourceLocation, JsonElement> entry : object.entrySet()) {
            ResourceLocation id = entry.getKey();
            // We use the JSON content but ensure the ID matches the file location for consistency if needed,
            // or just rely on the ID inside the JSON. Here we prefer the JSON's ID field for flexibility.
            StatSynergy.CODEC.parse(JsonOps.INSTANCE, entry.getValue())
                    .resultOrPartial(err -> Memento.LOGGER.error("Failed to parse synergy {}: {}", id, err))
                    .ifPresent(SYNERGIES::add);
            count++;
        }
        Memento.LOGGER.info("Loaded {} stat synergies", count);
    }

    public static List<StatSynergy> getAllSynergies() {
        return SYNERGIES;
    }
}