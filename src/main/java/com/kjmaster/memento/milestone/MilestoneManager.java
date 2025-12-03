package com.kjmaster.memento.milestone;

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

import java.util.*;

public class MilestoneManager extends SimpleJsonResourceReloadListener {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    // Map of Stat ID -> List of Milestones for that stat
    private static final Map<ResourceLocation, List<Milestone>> MILESTONES = new HashMap<>();

    public MilestoneManager() {
        super(GSON, "milestones");
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> object, @NotNull ResourceManager resourceManager, @NotNull ProfilerFiller profiler) {
        MILESTONES.clear();

        for (Map.Entry<ResourceLocation, JsonElement> entry : object.entrySet()) {
            ResourceLocation id = entry.getKey();
            JsonElement json = entry.getValue();

            Milestone.CODEC.parse(JsonOps.INSTANCE, json)
                    .resultOrPartial(err -> Memento.LOGGER.error("Failed to parse milestone {}: {}", id, err))
                    .ifPresent(milestone -> {
                        MILESTONES.computeIfAbsent(milestone.statId(), k -> new ArrayList<>()).add(milestone);
                    });
        }
        Memento.LOGGER.info("Loaded {} milestones", MILESTONES.values().stream().mapToInt(List::size).sum());
    }

    public static List<Milestone> getMilestonesFor(ResourceLocation statId) {
        return MILESTONES.getOrDefault(statId, Collections.emptyList());
    }
}