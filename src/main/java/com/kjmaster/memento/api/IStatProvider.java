package com.kjmaster.memento.api;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

/**
 * Interface for systems that want to provide custom storage for Memento stats.
 * Register implementations via StatProviderRegistry.
 */
public interface IStatProvider {
    /**
     * @return True if this provider handles storage for the given stat on the given item.
     * Return false to let Memento handle it via Data Components.
     */
    boolean canProvide(ItemStack stack, ResourceLocation statId);

    /**
     * Retrieve the current value of the stat.
     */
    long getStat(ItemStack stack, ResourceLocation statId);

    /**
     * Set the absolute value of the stat.
     * Note: MementoAPI handles merging logic (sum/max/min) before calling this.
     */
    void setStat(ItemStack stack, ResourceLocation statId, long value);
}