package com.kjmaster.memento.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.kjmaster.memento.Memento;
import com.mojang.serialization.JsonOps;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class StatTriggerManager extends SimpleJsonResourceReloadListener {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private static final Map<StatTrigger.TriggerType, List<StatTrigger>> GLOBAL_TRIGGERS = new HashMap<>();
    private static final Map<StatTrigger.TriggerType, Map<Item, List<StatTrigger>>> INDEXED_TRIGGERS = new HashMap<>();
    private final HolderLookup.Provider registries;

    public StatTriggerManager(HolderLookup.Provider registries) {
        super(GSON, "memento/stat_triggers");
        this.registries = registries;
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> object, @NotNull ResourceManager resourceManager, @NotNull ProfilerFiller profiler) {
        GLOBAL_TRIGGERS.clear();
        INDEXED_TRIGGERS.clear();
        int count = 0;
        RegistryOps<JsonElement> registryOps = RegistryOps.create(JsonOps.INSTANCE, this.registries);

        for (Map.Entry<ResourceLocation, JsonElement> entry : object.entrySet()) {
            StatTrigger.CODEC.parse(registryOps, entry.getValue())
                    .resultOrPartial(err -> Memento.LOGGER.error("Failed to parse trigger {}: {}", entry.getKey(), err))
                    .ifPresent(trigger -> {
                        if (trigger.items().isPresent() && trigger.items().get().size() > 0) {
                            Map<Item, List<StatTrigger>> typeMap = INDEXED_TRIGGERS.computeIfAbsent(trigger.type(), k -> new HashMap<>());
                            for (Holder<Item> holder : trigger.items().get()) {
                                Item item = holder.value();
                                if (item == Items.AIR && !holder.is(ResourceLocation.withDefaultNamespace("air"))) {
                                    continue;
                                }
                                typeMap.computeIfAbsent(item, k -> new ArrayList<>()).add(trigger);
                            }
                        } else {
                            GLOBAL_TRIGGERS.computeIfAbsent(trigger.type(), k -> new ArrayList<>()).add(trigger);
                        }
                    });
            count++;
        }
        Memento.LOGGER.info("Loaded {} stat triggers", count);
    }

    public static List<StatTrigger> getTriggers(StatTrigger.TriggerType type, ItemStack stack) {
        List<StatTrigger> results = new ArrayList<>();
        List<StatTrigger> globals = GLOBAL_TRIGGERS.get(type);
        if (globals != null) results.addAll(globals);

        if (!stack.isEmpty()) {
            Map<Item, List<StatTrigger>> typeIndex = INDEXED_TRIGGERS.get(type);
            if (typeIndex != null) {
                List<StatTrigger> indexed = typeIndex.get(stack.getItem());
                if (indexed != null) results.addAll(indexed);
            }
        }
        return results;
    }
}