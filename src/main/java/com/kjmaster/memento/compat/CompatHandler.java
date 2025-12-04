package com.kjmaster.memento.compat;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.ModList;

import java.util.ArrayList;
import java.util.List;

public class CompatHandler {
    private static final List<InventoryCompat> COMPATS = new ArrayList<>();

    public static void init() {
        if (ModList.get().isLoaded("curios")) {
            COMPATS.add(new CuriosCompat());
        }
        // Future: Add other mods here
    }

    public static boolean replaceModdedItem(ServerPlayer player, ItemStack oldStack, ItemStack newStack) {
        for (InventoryCompat compat : COMPATS) {
            if (compat.replaceItem(player, oldStack, newStack)) {
                return true;
            }
        }
        return false;
    }
}