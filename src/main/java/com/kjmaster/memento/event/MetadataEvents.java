package com.kjmaster.memento.event;

import com.kjmaster.memento.component.ItemMetadata;
import com.kjmaster.memento.registry.ModDataComponents;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

import java.util.List;

public class MetadataEvents {

    @SubscribeEvent
    public static void onItemCrafted(PlayerEvent.ItemCraftedEvent event) {
        ItemStack stack = event.getCrafting();

        // Only tag items that can break (tools/armor) OR have a max stack size of 1
        if (stack.isDamageableItem() || stack.getMaxStackSize() == 1) {
            if (!stack.has(ModDataComponents.ITEM_METADATA)) {
                String playerName = event.getEntity().getName().getString();
                long worldDay = event.getEntity().level().getDayTime() / 24000L;

                // Capture the original name (e.g. "Iron Sword") before any anvils
                String originalName = stack.getHoverName().getString();

                stack.set(ModDataComponents.ITEM_METADATA, new ItemMetadata(playerName, worldDay, originalName, List.of()));
            }
        }
    }
}