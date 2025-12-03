package com.kjmaster.memento.api;

import com.kjmaster.memento.component.TrackerMap;
import com.kjmaster.memento.component.UnlockedMilestones;
import com.kjmaster.memento.milestone.Milestone;
import com.kjmaster.memento.milestone.MilestoneManager;
import com.kjmaster.memento.network.MilestoneToastPayload;
import com.kjmaster.memento.registry.ModDataComponents;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.List;

public class MementoAPI {

    /**
     * Increments a statistic on the given ItemStack and handles all Milestone logic.
     * @param player The player causing the event (used for sound/chat feedback).
     * @param stack The item to update (must be mutable).
     * @param statId The unique ID of the stat (e.g. memento:blocks_broken).
     * @param amount The amount to add.
     */
    public static void incrementStat(ServerPlayer player, ItemStack stack, ResourceLocation statId, long amount) {
        if (stack.isEmpty() || stack.getMaxStackSize() > 1) return;

        // 1. Update the Stat
        TrackerMap currentStats = stack.getOrDefault(ModDataComponents.TRACKER_MAP, TrackerMap.EMPTY);
        TrackerMap newStats = currentStats.increment(statId, amount);
        stack.set(ModDataComponents.TRACKER_MAP, newStats);

        long newValue = newStats.getValue(statId);

        // 2. Check for Milestones
        List<Milestone> milestones = MilestoneManager.getMilestonesFor(statId);
        if (milestones.isEmpty()) return;

        UnlockedMilestones unlocked = stack.getOrDefault(ModDataComponents.MILESTONES, UnlockedMilestones.EMPTY);
        boolean changed = false;

        for (Milestone milestone : milestones) {
            String milestoneId = statId.toString() + "/" + milestone.targetValue();

            if (newValue >= milestone.targetValue() && !unlocked.hasUnlocked(milestoneId)) {
                unlocked = unlocked.add(milestoneId);
                changed = true;

                // Apply Title
                if (milestone.titleName().isPresent()) {
                    PacketDistributor.sendToPlayer(player, new MilestoneToastPayload(
                            stack,
                            Component.translatable("memento.toast.levelup"),
                            milestone.titleName().get()
                    ));
                    stack.set(DataComponents.CUSTOM_NAME, milestone.titleName().get());
                }

                // Play Sound
                if (milestone.soundId().isPresent()) {
                    ResourceLocation soundLoc = ResourceLocation.parse(milestone.soundId().get());
                    SoundEvent sound = BuiltInRegistries.SOUND_EVENT.get(soundLoc);
                    if (sound != null) {
                        player.level().playSound(null, player.blockPosition(), sound, SoundSource.PLAYERS, 1.0f, 1.0f);
                    }
                }
            }
        }

        if (changed) {
            stack.set(ModDataComponents.MILESTONES, unlocked);
        }
    }
}