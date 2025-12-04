package com.kjmaster.memento.event;

import com.kjmaster.memento.Memento;
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
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class MilestoneEventHandler {

    private static final boolean CURIOS_LOADED = ModList.get().isLoaded("curios");

    @SubscribeEvent
    public static void onStatChange(StatChangeEvent.Post event) {
        checkMilestones(event.getPlayer(), event.getItem(), event.getStatId(), event.getNewValue());
    }

    private static void checkMilestones(ServerPlayer player, ItemStack stack, ResourceLocation statId, long newValue) {
        List<StatMilestone> milestones = StatMilestoneManager.getMilestonesFor(statId);
        if (milestones.isEmpty()) return;

        UnlockedMilestones unlocked = stack.getOrDefault(ModDataComponents.MILESTONES, UnlockedMilestones.EMPTY);
        boolean changed = false;

        for (StatMilestone milestone : milestones) {
            String milestoneId = statId.toString() + "/" + milestone.targetValue();

            if (unlocked.hasUnlocked(milestoneId)) continue;
            if (newValue < milestone.targetValue()) continue;

            if (milestone.itemRequirement().isPresent()) {
                if (!milestone.itemRequirement().get().test(stack)) continue;
            }

            unlocked = unlocked.add(milestoneId);
            changed = true;

            boolean transformed = performMilestone(player, stack, milestone, milestoneId);

            if (transformed) {
                return;
            }
        }

        if (changed && !stack.isEmpty()) {
            stack.set(ModDataComponents.MILESTONES, unlocked);
        }
    }

    private static boolean performMilestone(ServerPlayer player, ItemStack stack, StatMilestone milestone, String milestoneId) {
        boolean transformed = false;

        if (milestone.replacementItem().isPresent()) {
            ItemStack newStack = milestone.replacementItem().get().copy();

            if (milestone.keepStats()) {
                // FIX: Use applyComponents to copy ALL data (Enchants, Name, Lore, Other Mod Data)
                // instead of manually copying just Memento components.
                // This ensures we don't wipe the item's history/identity during transformation.
                newStack.applyComponents(stack.getComponents());

                // Ensure the NEW milestone is recorded (since we just unlocked it)
                // It might not be in the old stack's components yet if this loop just added it.
                UnlockedMilestones currentUnlocked = newStack.getOrDefault(ModDataComponents.MILESTONES, UnlockedMilestones.EMPTY);
                currentUnlocked = currentUnlocked.add(milestoneId);
                newStack.set(ModDataComponents.MILESTONES, currentUnlocked);
            }

            boolean success = replaceItemInInventory(player, stack, newStack);

            if (success) {
                stack = newStack;
                transformed = true;
            } else {
                Memento.LOGGER.warn("Failed to apply milestone transformation for player {}: Could not locate original item instance.", player.getName().getString());
                return false;
            }
        }

        if (milestone.titleName().isPresent()) {
            stack.set(DataComponents.CUSTOM_NAME, milestone.titleName().get());
            PacketDistributor.sendToPlayer(player, new MilestoneToastPayload(
                    stack,
                    Component.translatable("memento.toast.levelup"),
                    milestone.titleName().get()
            ));
        }

        if (milestone.soundId().isPresent()) {
            ResourceLocation soundLoc = ResourceLocation.parse(milestone.soundId().get());
            SoundEvent sound = BuiltInRegistries.SOUND_EVENT.get(soundLoc);
            if (sound != null) {
                player.level().playSound(null, player.blockPosition(), sound, SoundSource.PLAYERS, 1.0f, 1.0f);
            }
        }

        if (!milestone.rewards().isEmpty()) {
            CommandSourceStack source = player.createCommandSourceStack().withPermission(2).withSuppressedOutput();
            for (String command : milestone.rewards()) {
                player.server.getCommands().performPrefixedCommand(source, command);
            }
        }

        return transformed;
    }

    private static boolean replaceItemInInventory(ServerPlayer player, ItemStack oldStack, ItemStack newStack) {
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            if (player.getItemBySlot(slot) == oldStack) {
                player.setItemSlot(slot, newStack);
                return true;
            }
        }

        if (player.containerMenu != null && player.containerMenu.getCarried() == oldStack) {
            player.containerMenu.setCarried(newStack);
            return true;
        }

        net.minecraft.world.entity.player.Inventory inv = player.getInventory();
        for (int i = 0; i < inv.items.size(); i++) {
            if (inv.items.get(i) == oldStack) {
                inv.items.set(i, newStack);
                return true;
            }
        }
        for (int i = 0; i < inv.offhand.size(); i++) {
            if (inv.offhand.get(i) == oldStack) {
                inv.offhand.set(i, newStack);
                return true;
            }
        }
        for (int i = 0; i < inv.armor.size(); i++) {
            if (inv.armor.get(i) == oldStack) {
                inv.armor.set(i, newStack);
                return true;
            }
        }

        if (CURIOS_LOADED) {
            if (replaceCuriosItem(player, oldStack, newStack)) {
                return true;
            }
        }

        return false;
    }

    private static boolean replaceCuriosItem(LivingEntity entity, ItemStack oldStack, ItemStack newStack) {
        AtomicBoolean found = new AtomicBoolean(false);
        try {
            top.theillusivec4.curios.api.CuriosApi.getCuriosInventory(entity).ifPresent(handler -> {
                var curiosHandler = handler.getEquippedCurios();
                for (int i = 0; i < curiosHandler.getSlots(); i++) {
                    if (curiosHandler.getStackInSlot(i) == oldStack) {
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