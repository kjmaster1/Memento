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
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class StatTransferFilterManager extends SimpleJsonResourceReloadListener {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private static final List<StatTransferFilter> RULES = new ArrayList<>();

    private final HolderLookup.Provider registries;

    public StatTransferFilterManager(HolderLookup.Provider registries) {
        super(GSON, "stat_transfer_filters");
        this.registries = registries;
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> object, @NotNull ResourceManager resourceManager, @NotNull ProfilerFiller profiler) {
        RULES.clear();

        RegistryOps<JsonElement> registryOps = RegistryOps.create(JsonOps.INSTANCE, this.registries);

        for (Map.Entry<ResourceLocation, JsonElement> entry : object.entrySet()) {
            StatTransferFilter.CODEC.parse(registryOps, entry.getValue())
                    .resultOrPartial(err -> Memento.LOGGER.error("Failed to parse transfer filter {}: {}", entry.getKey(), err))
                    .ifPresent(RULES::add);
        }
        Memento.LOGGER.info("Loaded {} stat transfer filters", RULES.size());
    }

    public static boolean isAllowed(ItemStack stack, ResourceLocation statId) {
        // Default: Allow
        boolean allowed = true;

        for (StatTransferFilter rule : RULES) {
            if (rule.itemMatcher().test(stack)) {
                // Check Bans
                if (rule.bannedStats().isPresent() && rule.bannedStats().get().contains(statId)) {
                    return false;
                }
                // Check Allow-list (if present, strictly enforces)
                if (rule.allowedStats().isPresent()) {
                    if (!rule.allowedStats().get().contains(statId)) {
                        return false;
                    }
                }
            }
        }
        return true;
    }
}