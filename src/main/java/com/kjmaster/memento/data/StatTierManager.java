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
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class StatTierManager extends SimpleJsonResourceReloadListener {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private static final List<StatTierRule> RULES = new ArrayList<>();

    public StatTierManager() {
        super(GSON, "stat_tiers");
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> object, @NotNull ResourceManager resourceManager, @NotNull ProfilerFiller profiler) {
        RULES.clear();
        int count = 0;

        for (Map.Entry<ResourceLocation, JsonElement> entry : object.entrySet()) {
            StatTierRule.CODEC.parse(JsonOps.INSTANCE, entry.getValue())
                    .resultOrPartial(err -> Memento.LOGGER.error("Failed to parse tier rule {}: {}", entry.getKey(), err))
                    .ifPresent(RULES::add);
            count++;
        }
        Memento.LOGGER.info("Loaded {} stat tier rules", count);
    }

    public static List<StatTierRule> getApplicableRules(ItemStack stack) {
        // Linear scan is acceptable here as HarvestCheck is not per-tick (only on block interaction)
        // Optimization: In a real scenario, index this by Item
        List<StatTierRule> matching = new ArrayList<>();
        for (StatTierRule rule : RULES) {
            if (rule.item().test(stack)) {
                matching.add(rule);
            }
        }
        return matching;
    }
}