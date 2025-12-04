package com.kjmaster.memento.network;

import com.kjmaster.memento.client.MilestoneToast;
import com.kjmaster.memento.component.TrackerMap;
import com.kjmaster.memento.data.StatBehavior;
import com.kjmaster.memento.data.StatBehaviorManager;
import com.kjmaster.memento.registry.ModDataComponents;
import com.kjmaster.memento.util.SlotHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.function.Consumer;

public class ClientPayloadHandler {

    public static void handleData(final MilestoneToastPayload payload, final IPayloadContext context) {
        context.enqueueWork(() -> {
            Minecraft.getInstance().getToasts().addToast(
                    new MilestoneToast(payload.item(), payload.title(), payload.description())
            );
        });
    }

    public static void handleStatUpdate(final StatUpdatePayload payload, final IPayloadContext context) {
        context.enqueueWork(() -> {
            Player player = Minecraft.getInstance().player;
            if (player == null) return;

            // Fetch the strategy (SUM, MAX, MIN) so we know how to apply the partial update
            StatBehavior.MergeStrategy strategy = StatBehaviorManager.getStrategy(payload.stat());

            Consumer<ItemStack> updater = stack -> {
                if (stack.isEmpty()) return;

                // UUID Match Check
                if (stack.has(ModDataComponents.ITEM_UUID) && stack.get(ModDataComponents.ITEM_UUID).equals(payload.itemUuid())) {
                    TrackerMap map = stack.getOrDefault(ModDataComponents.TRACKER_MAP, TrackerMap.EMPTY);

                    // Apply update using the correct strategy (e.g. SUM for distance, MAX for records)
                    TrackerMap newMap = map.update(payload.stat(), payload.value(), (oldVal, newVal) -> switch (strategy) {
                        case MAX -> Math.max(oldVal, newVal);
                        case MIN -> (oldVal == 0) ? newVal : Math.min(oldVal, newVal);
                        case SUM -> oldVal + newVal;
                    });

                    stack.set(ModDataComponents.TRACKER_MAP, newMap);
                }
            };

            // 1. Scan Open Container
            if (player.containerMenu != null) {
                for (Slot slot : player.containerMenu.slots) {
                    updater.accept(slot.getItem());
                }
            }

            // 2. Scan Inventory
            player.getInventory().items.forEach(updater);
            player.getInventory().armor.forEach(updater);
            player.getInventory().offhand.forEach(updater);

            // 3. Scan Curios
            SlotHelper.processCurios(player, (stack, slotIndex) -> updater.accept(stack));
        });
    }
}