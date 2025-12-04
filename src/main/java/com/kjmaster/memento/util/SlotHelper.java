package com.kjmaster.memento.util;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.items.IItemHandlerModifiable;
import top.theillusivec4.curios.api.CuriosApi;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

public class SlotHelper {

    private static final boolean CURIOS_LOADED = ModList.get().isLoaded("curios");

    /**
     * A wrapper to keep track of where the item was found.
     * @param slot The vanilla slot, or null if it is a Curio/Modded slot.
     */
    public record SlotContext(ItemStack stack, @Nullable EquipmentSlot slot) {}

    public static List<SlotContext> getAllWornItems(LivingEntity entity) {
        List<SlotContext> items = new ArrayList<>();

        // 1. Vanilla Slots
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            ItemStack stack = entity.getItemBySlot(slot);
            if (!stack.isEmpty()) {
                items.add(new SlotContext(stack, slot));
            }
        }

        // 2. Curios Slots
        if (CURIOS_LOADED) {
            getCuriosItems(entity, items);
        }

        return items;
    }

    private static void getCuriosItems(LivingEntity entity, List<SlotContext> items) {
        CuriosApi.getCuriosInventory(entity).ifPresent(handler -> {
            IItemHandlerModifiable curiosHandler = handler.getEquippedCurios();
            for (int i = 0; i < curiosHandler.getSlots(); i++) {
                ItemStack stack = curiosHandler.getStackInSlot(i);
                if (!stack.isEmpty()) {
                    // We pass null for the slot because Curios slots don't map to EquipmentSlot
                    items.add(new SlotContext(stack, null));
                }
            }
        });
    }
}