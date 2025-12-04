package com.kjmaster.memento.compat;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

public interface InventoryCompat {
    /**
     * Attempts to replace an item in this specific inventory system.
     * @param player The player whose inventory is being checked.
     * @param oldStack The item stack to find.
     * @param newStack The item stack to replace it with.
     * @return true if the item was found and replaced.
     */
    boolean replaceItem(ServerPlayer player, ItemStack oldStack, ItemStack newStack);
}