package com.kjmaster.memento.event;

import com.kjmaster.memento.api.MementoAPI;
import com.kjmaster.memento.api.event.StatChangeEvent;
import com.kjmaster.memento.component.UnlockedSynergies;
import com.kjmaster.memento.data.StatSynergy;
import com.kjmaster.memento.data.StatSynergyManager;
import com.kjmaster.memento.network.MilestoneToastPayload;
import com.kjmaster.memento.registry.ModDataComponents;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.Map;

public class SynergyEvents {

    @SubscribeEvent
    public static void onStatChange(StatChangeEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        ItemStack stack = event.getItem();
        if (stack.isEmpty()) return;

        UnlockedSynergies unlocked = stack.getOrDefault(ModDataComponents.UNLOCKED_SYNERGIES, UnlockedSynergies.EMPTY);
        boolean changed = false;
        ResourceLocation itemKey = BuiltInRegistries.ITEM.getKey(stack.getItem());

        for (StatSynergy synergy : StatSynergyManager.getAllSynergies()) {
            if (unlocked.hasUnlocked(synergy.id())) continue;

            // Apply Item Filter
            if (synergy.items().isPresent() && !synergy.items().get().contains(itemKey)) {
                continue;
            }

            if (checkRequirements(stack, synergy.requirements())) {
                unlocked = unlocked.add(synergy.id());
                changed = true;
                grantRewards(player, stack, synergy);
            }
        }

        if (changed) {
            stack.set(ModDataComponents.UNLOCKED_SYNERGIES, unlocked);
        }
    }

    private static boolean checkRequirements(ItemStack stack, Map<ResourceLocation, Long> reqs) {
        for (Map.Entry<ResourceLocation, Long> req : reqs.entrySet()) {
            long currentVal = MementoAPI.getStat(stack, req.getKey());
            if (currentVal < req.getValue()) return false;
        }
        return true;
    }

    private static void grantRewards(ServerPlayer player, ItemStack stack, StatSynergy synergy) {
        if (synergy.title().isPresent()) {
            PacketDistributor.sendToPlayer(player, new MilestoneToastPayload(
                    stack, synergy.title().get(), synergy.description().orElse(Component.empty())
            ));
        }

        if (synergy.sound().isPresent()) {
            ResourceLocation soundLoc = ResourceLocation.parse(synergy.sound().get());
            SoundEvent sound = BuiltInRegistries.SOUND_EVENT.get(soundLoc);
            if (sound != null) {
                player.level().playSound(null, player.blockPosition(), sound, SoundSource.PLAYERS, 1.0f, 1.0f);
            }
        }

        if (!synergy.rewards().isEmpty()) {
            CommandSourceStack source = player.createCommandSourceStack().withPermission(2).withSuppressedOutput();
            for (String command : synergy.rewards()) {
                player.server.getCommands().performPrefixedCommand(source, command);
            }
        }
    }
}