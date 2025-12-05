package com.kjmaster.memento.registry;

import com.kjmaster.memento.Memento;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;

public class ModTags {
    public static final TagKey<Block> STAT_BLACKLIST_BLOCKS = TagKey.create(Registries.BLOCK, Memento.loc("stat_blacklist_blocks"));
}