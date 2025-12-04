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

public class StatEffectManager extends SimpleJsonResourceReloadListener {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    // Categorized rules for fast lookup
    private static final Map<StatEffect.EffectContext, List<StatEffect>> RULES = new HashMap<>();

    public StatEffectManager() {
        super(GSON, "stat_effects");
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> object, @NotNull ResourceManager resourceManager, @NotNull ProfilerFiller profiler) {
        RULES.clear();
        int count = 0;

        for (Map.Entry<ResourceLocation, JsonElement> entry : object.entrySet()) {
            StatEffect.CODEC.parse(JsonOps.INSTANCE, entry.getValue())
                    .resultOrPartial(err -> Memento.LOGGER.error("Failed to parse stat effect {}: {}", entry.getKey(), err))
                    .ifPresent(rule -> {
                        RULES.computeIfAbsent(rule.context(), k -> new ArrayList<>()).add(rule);
                    });
            count++;
        }
        Memento.LOGGER.info("Loaded {} stat effect rules", count);
    }

    public static List<StatEffect> getRules(StatEffect.EffectContext context) {
        return RULES.getOrDefault(context, List.of());
    }
}