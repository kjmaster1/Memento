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

public class StatEnchantmentManager extends SimpleJsonResourceReloadListener {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    // Map of StatID -> List of Enchantment Rules linked to that stat
    private static final Map<ResourceLocation, List<StatEnchantment>> RULES = new HashMap<>();

    public StatEnchantmentManager() {
        super(GSON, "stat_enchantments");
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> object, @NotNull ResourceManager resourceManager, @NotNull ProfilerFiller profiler) {
        RULES.clear();
        int count = 0;

        for (Map.Entry<ResourceLocation, JsonElement> entry : object.entrySet()) {
            ResourceLocation fileId = entry.getKey();
            JsonElement json = entry.getValue();

            StatEnchantment.CODEC.parse(JsonOps.INSTANCE, json)
                    .resultOrPartial(err -> Memento.LOGGER.error("Failed to parse stat enchantment rule {}: {}", fileId, err))
                    .ifPresent(rule -> {
                        RULES.computeIfAbsent(rule.stat(), k -> new ArrayList<>()).add(rule);
                    });
            count++;
        }
        Memento.LOGGER.info("Loaded {} stat enchantment rules", count);
    }

    public static List<StatEnchantment> getRulesFor(ResourceLocation statId) {
        return RULES.getOrDefault(statId, List.of());
    }
}