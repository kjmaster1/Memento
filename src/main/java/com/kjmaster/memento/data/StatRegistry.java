package com.kjmaster.memento.data;

import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;
import java.util.Map;

public class StatRegistry {
    private static final Map<ResourceLocation, Integer> ID_MAP = new HashMap<>();
    private static final Map<Integer, ResourceLocation> STAT_MAP = new HashMap<>();

    public static void setMapping(Map<ResourceLocation, Integer> mapping) {
        ID_MAP.clear();
        STAT_MAP.clear();
        ID_MAP.putAll(mapping);
        for (Map.Entry<ResourceLocation, Integer> entry : mapping.entrySet()) {
            STAT_MAP.put(entry.getValue(), entry.getKey());
        }
    }

    public static int getId(ResourceLocation stat) {
        return ID_MAP.getOrDefault(stat, -1);
    }

    public static ResourceLocation getStat(int id) {
        return STAT_MAP.get(id);
    }

    public static Map<ResourceLocation, Integer> getMap() {
        return new HashMap<>(ID_MAP);
    }
}