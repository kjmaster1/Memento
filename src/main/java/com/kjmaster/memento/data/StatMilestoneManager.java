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

import java.util.*;

public class StatMilestoneManager extends SimpleJsonResourceReloadListener {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    // Map of Stat ID -> List of Milestones for that stat
    private static final Map<ResourceLocation, List<StatMilestone>> MILESTONES = new HashMap<>();

    public StatMilestoneManager() {
        super(GSON, "milestones");
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> object, @NotNull ResourceManager resourceManager, @NotNull ProfilerFiller profiler) {
        MILESTONES.clear();

        for (Map.Entry<ResourceLocation, JsonElement> entry : object.entrySet()) {
            ResourceLocation id = entry.getKey();
            JsonElement json = entry.getValue();

            StatMilestone.CODEC.parse(JsonOps.INSTANCE, json)
                    .resultOrPartial(err -> Memento.LOGGER.error("Failed to parse milestone {}: {}", id, err))
                    .ifPresent(milestone -> {
                        MILESTONES.computeIfAbsent(milestone.statId(), k -> new ArrayList<>()).add(milestone);
                    });
        }
        Memento.LOGGER.info("Loaded {} milestones", MILESTONES.values().stream().mapToInt(List::size).sum());
    }

    public static List<StatMilestone> getMilestonesFor(ResourceLocation statId) {
        return MILESTONES.getOrDefault(statId, Collections.emptyList());
    }

    /**
     * @return All loaded milestones, useful for JEI integration.
     */
    public static Map<ResourceLocation, List<StatMilestone>> getAllMilestones() {
        return Collections.unmodifiableMap(MILESTONES);
    }
}