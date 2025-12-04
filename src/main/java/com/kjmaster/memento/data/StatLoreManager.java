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
import java.util.Comparator;
import java.util.List;
import java.util.Map;

public class StatLoreManager extends SimpleJsonResourceReloadListener {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private static final List<StatLoreRule> RULES = new ArrayList<>();

    public StatLoreManager() {
        super(GSON, "stat_lore");
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> object, @NotNull ResourceManager resourceManager, @NotNull ProfilerFiller profiler) {
        RULES.clear();
        int count = 0;

        for (Map.Entry<ResourceLocation, JsonElement> entry : object.entrySet()) {
            StatLoreRule.CODEC.parse(JsonOps.INSTANCE, entry.getValue())
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