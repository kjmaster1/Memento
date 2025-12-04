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

public class StatAttributeManager extends SimpleJsonResourceReloadListener {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    // Map of StatID -> List of Attribute Rules linked to that stat
    private static final Map<ResourceLocation, List<StatAttribute>> RULES = new HashMap<>();

    public StatAttributeManager() {
        super(GSON, "stat_attributes");
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> object, @NotNull ResourceManager resourceManager, @NotNull ProfilerFiller profiler) {
        RULES.clear();

        for (Map.Entry<ResourceLocation, JsonElement> entry : object.entrySet()) {
            StatAttribute.CODEC.parse(JsonOps.INSTANCE, entry.getValue())
                    .resultOrPartial(err -> Memento.LOGGER.error("Failed to parse stat attribute {}: {}", entry.getKey(), err))
                    .ifPresent(rule -> {
                        RULES.computeIfAbsent(rule.stat(), k -> new ArrayList<>()).add(rule);
                    });
        }
        Memento.LOGGER.info("Loaded {} stat attribute rules", RULES.size());
    }

    public static List<StatAttribute> getRulesFor(ResourceLocation statId) {
        return RULES.getOrDefault(statId, List.of());
    }
}