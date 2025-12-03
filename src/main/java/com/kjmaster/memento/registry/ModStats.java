package com.kjmaster.memento.registry;

import com.kjmaster.memento.Memento;
import net.minecraft.resources.ResourceLocation;

public class ModStats {
    public static final ResourceLocation BLOCKS_BROKEN = loc("blocks_broken");
    public static final ResourceLocation ENTITIES_KILLED = loc("entities_killed");
    public static final ResourceLocation DISTANCE_FLOWN = loc("distance_flown");
    public static final ResourceLocation DAMAGE_TAKEN = loc("damage_taken");
    public static final ResourceLocation CROPS_HARVESTED = loc("crops_harvested");

    private static ResourceLocation loc(String path) {
        return ResourceLocation.fromNamespaceAndPath(Memento.MODID, path);
    }
}