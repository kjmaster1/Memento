package com.kjmaster.memento.compat;

import com.kjmaster.memento.Memento;
import com.kjmaster.memento.registry.ModDataComponents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import top.theillusivec4.curios.api.CuriosApi;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

public class CuriosCompat implements InventoryCompat {
    @Override
    public boolean replaceItem(ServerPlayer player, ItemStack oldStack, ItemStack newStack) {
        AtomicBoolean found = new AtomicBoolean(false);
        try {
            // Optimization: UUID is constant for the check, fetch it once.
            final UUID targetUuid = oldStack.has(ModDataComponents.ITEM_UUID)
                    ? oldStack.get(ModDataComponents.ITEM_UUID)
                    : null;

            CuriosApi.getCuriosInventory(player).ifPresent(handler -> {
                var curiosHandler = handler.getEquippedCurios();
                int slots = curiosHandler.getSlots(); // Cache slot count

                for (int i = 0; i < slots; i++) {
                    ItemStack inSlot = curiosHandler.getStackInSlot(i);
                    if (inSlot.isEmpty()) continue;

                    boolean match = false;

                    // Check UUID
                    if (targetUuid != null && inSlot.has(ModDataComponents.ITEM_UUID)) {
                        if (targetUuid.equals(inSlot.get(ModDataComponents.ITEM_UUID))) {
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
                        // Break early once found
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