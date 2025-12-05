package com.kjmaster.memento.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.kjmaster.memento.Memento;
import com.mojang.serialization.JsonOps;
import net.minecraft.core.HolderLookup;
import net.minecraft.resources.RegistryOps;
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
    private static final Map<ResourceLocation, List<StatEnchantment>> RULES = new HashMap<>();
    private final HolderLookup.Provider registries;

    public StatEnchantmentManager(HolderLookup.Provider registries) {
        super(GSON, "stat_enchantments");
        this.registries = registries;
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> object, @NotNull ResourceManager resourceManager, @NotNull ProfilerFiller profiler) {
        RULES.clear();
        int count = 0;

        RegistryOps<JsonElement> registryOps = RegistryOps.create(JsonOps.INSTANCE, this.registries);

        for (Map.Entry<ResourceLocation, JsonElement> entry : object.entrySet()) {
            ResourceLocation fileId = entry.getKey();
            JsonElement json = entry.getValue();

            StatEnchantment.CODEC.parse(registryOps, json)
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