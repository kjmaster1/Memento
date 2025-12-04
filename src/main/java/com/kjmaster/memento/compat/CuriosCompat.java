package com.kjmaster.memento.compat;

import com.kjmaster.memento.Memento;
import com.kjmaster.memento.registry.ModDataComponents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import top.theillusivec4.curios.api.CuriosApi;

import java.util.concurrent.atomic.AtomicBoolean;

public class CuriosCompat implements InventoryCompat {
    @Override
    public boolean replaceItem(ServerPlayer player, ItemStack oldStack, ItemStack newStack) {
        AtomicBoolean found = new AtomicBoolean(false);
        try {
            CuriosApi.getCuriosInventory(player).ifPresent(handler -> {
                var curiosHandler = handler.getEquippedCurios();
                for (int i = 0; i < curiosHandler.getSlots(); i++) {
                    ItemStack inSlot = curiosHandler.getStackInSlot(i);

                    // Check UUID
                    boolean match = false;
                    if (oldStack.has(ModDataComponents.ITEM_UUID) && inSlot.has(ModDataComponents.ITEM_UUID)) {
                        if (oldStack.get(ModDataComponents.ITEM_UUID).equals(inSlot.get(ModDataComponents.ITEM_UUID))) {
                            match = true;
                        }
                    }
                    // Fallback to reference
                    else if (inSlot == oldStack) {
                        match = true;
                    }

                    if (match) {
                        curiosHandler.setStackInSlot(i, newStack);
                        found.set(true);
                        return;
                    }
                }
            });
        } catch (Exception e) {
            Memento.LOGGER.error("Error attempting to replace item in Curios slot", e);
        }
        return found.get();
    }
}