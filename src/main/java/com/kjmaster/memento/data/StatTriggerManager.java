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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class StatTriggerManager extends SimpleJsonResourceReloadListener {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private static final Map<StatTrigger.TriggerType, List<StatTrigger>> TRIGGERS = new HashMap<>();

    public StatTriggerManager() {
        super(GSON, "stat_triggers");
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> object, @NotNull ResourceManager resourceManager, @NotNull ProfilerFiller profiler) {
        TRIGGERS.clear();

        for (Map.Entry<ResourceLocation, JsonElement> entry : object.entrySet()) {
            StatTrigger.CODEC.parse(JsonOps.INSTANCE, entry.getValue())
                    .resultOrPartial(err -> Memento.LOGGER.error("Failed to parse trigger {}: {}", entry.getKey(), err))
                    .ifPresent(trigger -> {
                        TRIGGERS.computeIfAbsent(trigger.type(), k -> new ArrayList<>()).add(trigger);
                    });
        }
        Memento.LOGGER.info("Loaded {} data-driven stat triggers", object.size());
    }

    public static List<StatTrigger> get(StatTrigger.TriggerType type) {
        return TRIGGERS.getOrDefault(type, List.of());
    }
}