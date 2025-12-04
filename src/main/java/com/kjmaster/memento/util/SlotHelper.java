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
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class SlotHelper {

    private static final boolean CURIOS_LOADED = ModList.get().isLoaded("curios");

    /**
     * A wrapper to keep track of where the item was found.
     *
     * @param slot The vanilla slot, or null if it is a Curio/Modded slot.
     */
    public record SlotContext(ItemStack stack, @Nullable EquipmentSlot slot) {
    }

    /**
     * Iterates over all worn items (Vanilla Armor/Offhand + Curios) and applies the consumer.
     * Optimized to avoid creating an ArrayList on every call.
     */
    public static void forEachWornItem(LivingEntity entity, Consumer<SlotContext> action) {
        // 1. Vanilla Slots
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            ItemStack stack = entity.getItemBySlot(slot);
            if (!stack.isEmpty()) {
                action.accept(new SlotContext(stack, slot));
            }
        }

        // 2. Curios Slots
        if (CURIOS_LOADED) {
            processCurios(entity, (stack, slotIndex) -> action.accept(new SlotContext(stack, null)));
        }
    }

    /**
     * Legacy method retained for compatibility, but delegates to the optimized consumer.
     * Prefer using forEachWornItem where possible to save memory.
     */
    public static List<SlotContext> getAllWornItems(LivingEntity entity) {
        List<SlotContext> items = new ArrayList<>();
        forEachWornItem(entity, items::add);
        return items;
    }

    /**
     * Iterates ONLY Curios slots.
     * Useful when the caller has already iterated vanilla inventory and wants to avoid double-scanning.
     */
    public static void processCurios(LivingEntity entity, BiConsumer<ItemStack, Integer> action) {
        if (!CURIOS_LOADED) return;

        CuriosApi.getCuriosInventory(entity).ifPresent(handler -> {
            IItemHandlerModifiable curiosHandler = handler.getEquippedCurios();
            int slots = curiosHandler.getSlots();
            for (int i = 0; i < slots; i++) {
                ItemStack stack = curiosHandler.getStackInSlot(i);
                if (!stack.isEmpty()) {
                    action.accept(stack, i);
                }
            }
        });
    }
}