package com.kjmaster.memento.event;

import com.kjmaster.memento.api.event.StatChangeEvent;
import com.kjmaster.memento.component.UnlockedMilestones;
import com.kjmaster.memento.data.StatMilestone;
import com.kjmaster.memento.data.StatMilestoneManager;
import com.kjmaster.memento.network.MilestoneToastPayload;
import com.kjmaster.memento.registry.ModDataComponents;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.List;

public class MilestoneEventHandler {

    @SubscribeEvent
    public void onStatChange(StatChangeEvent.Post event) {
        checkMilestones(event.getPlayer(), event.getItem(), event.getStatId(), event.getNewValue());
    }

    private void checkMilestones(ServerPlayer player, ItemStack stack, ResourceLocation statId, long newValue) {
        List<StatMilestone> milestones = StatMilestoneManager.getMilestonesFor(statId);
        if (milestones.isEmpty()) return;

        UnlockedMilestones unlocked = stack.getOrDefault(ModDataComponents.MILESTONES, UnlockedMilestones.EMPTY);
        boolean changed = false;

        for (StatMilestone milestone : milestones) {
            String milestoneId = statId.toString() + "/" + milestone.targetValue();

            // 1. Check Unlocked
            if (unlocked.hasUnlocked(milestoneId)) continue;
            if (newValue < milestone.targetValue()) continue;

            // 2. Check Item Requirement
            // If the JSON specifies "diamond_sword", don't evolve a "wooden_sword"
            if (milestone.itemRequirement().isPresent()) {
                if (!milestone.itemRequirement().get().test(stack)) continue;
            }

            // 3. Unlock & Execute
            unlocked = unlocked.add(milestoneId);
            changed = true;

            // Pass the milestone ID so we can avoid re-triggering it on the new item if we copy data
            performMilestone(player, stack, milestone, milestoneId);

            // If we transformed the item, we must STOP processing other milestones
            // because the original 'stack' is now invalid/gone.
            if (milestone.replacementItem().isPresent()) {
                break;
            }
        }

        // Only update the map if we didn't transform the item (if transformed, the old stack is gone)
        if (changed && !stack.isEmpty()) {
            stack.set(ModDataComponents.MILESTONES, unlocked);
        }
    }

    private void performMilestone(ServerPlayer player, ItemStack stack, StatMilestone milestone, String milestoneId) {
        if (milestone.replacementItem().isPresent()) {
            ItemStack newStack = milestone.replacementItem().get().copy();

            // Copy Stats?
            if (milestone.keepStats() && stack.has(ModDataComponents.TRACKER_MAP)) {
                newStack.set(ModDataComponents.TRACKER_MAP, stack.get(ModDataComponents.TRACKER_MAP));

                // CRITICAL: We must also copy the "Unlocked Milestones" so the new item doesn't
                // immediately re-trigger the same evolution loop if it still meets the criteria.
                UnlockedMilestones currentUnlocked = stack.getOrDefault(ModDataComponents.MILESTONES, UnlockedMilestones.EMPTY);
                // Ensure THIS milestone is marked as unlocked on the new item
                currentUnlocked = currentUnlocked.add(milestoneId);
                newStack.set(ModDataComponents.MILESTONES, currentUnlocked);

                // Copy Metadata (Creator/Date)
                if (stack.has(ModDataComponents.ITEM_METADATA)) {
                    newStack.set(ModDataComponents.ITEM_METADATA, stack.get(ModDataComponents.ITEM_METADATA));
                }
            }

            // Find and Replace in Inventory
            replaceItemInInventory(player, stack, newStack);

            // Update the local variable 'stack' to reference the new item for the Toast/Sound logic below
            stack = newStack;
        }

        // Title
        if (milestone.titleName().isPresent()) {
            stack.set(DataComponents.CUSTOM_NAME, milestone.titleName().get());
            // If transformed, we probably want to inform the player
            PacketDistributor.sendToPlayer(player, new MilestoneToastPayload(
                    stack,
                    Component.translatable("memento.toast.levelup"), // Or "memento.toast.evolved"
                    milestone.titleName().get()
            ));
        }

        // Sound
        if (milestone.soundId().isPresent()) {
            ResourceLocation soundLoc = ResourceLocation.parse(milestone.soundId().get());
            SoundEvent sound = BuiltInRegistries.SOUND_EVENT.get(soundLoc);
            if (sound != null) {
                player.level().playSound(null, player.blockPosition(), sound, SoundSource.PLAYERS, 1.0f, 1.0f);
            }
        }

        // Commands
        if (!milestone.rewards().isEmpty()) {
            CommandSourceStack source = player.createCommandSourceStack().withPermission(2).withSuppressedOutput();
            for (String command : milestone.rewards()) {
                player.server.getCommands().performPrefixedCommand(source, command);
            }
        }
    }

    private void replaceItemInInventory(ServerPlayer player, ItemStack oldStack, ItemStack newStack) {
        // 1. Check Hands
        if (player.getMainHandItem() == oldStack) {
            player.setItemSlot(EquipmentSlot.MAINHAND, newStack);
            return;
        }
        if (player.getOffhandItem() == oldStack) {
            player.setItemSlot(EquipmentSlot.OFFHAND, newStack);
            return;
        }

        // 2. Scan Inventory
        // (Note: == comparison works because 'oldStack' is the exact instance from the event)
        int slot = player.getInventory().findSlotMatchingItem(oldStack);
        if (slot != -1) {
            player.getInventory().setItem(slot, newStack);
        }
    }
}