package com.kjmaster.memento.event;

import com.kjmaster.memento.Memento;
import com.kjmaster.memento.api.event.StatChangeEvent;
import com.kjmaster.memento.component.UnlockedMilestones;
import com.kjmaster.memento.data.StatMilestone;
import com.kjmaster.memento.data.StatMilestoneManager;
import com.kjmaster.memento.network.MilestoneToastPayload;
import com.kjmaster.memento.registry.ModDataComponents;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.registries.DeferredHolder;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

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

            // TRANSACTIONAL LOGIC:
            // We pass the current 'unlocked' state to performMilestone.
            // It returns a Result indicating if it succeeded and if the stack was transformed.
            // We only update the persistent state if successful.
            MilestoneResult result = performMilestone(player, stack, milestone, milestoneId, unlocked);

            if (result == MilestoneResult.FAILED) {
                // Transformation failed (e.g. item missing/in bundle).
                // Do NOT unlock the milestone; let the player try again later.
                continue;
            }

            if (result == MilestoneResult.TRANSFORMED) {
                // Item is gone/replaced. Stop processing for this specific item instance.
                return;
            }

            // Milestone applied to CURRENT item (no transform), so we update local state
            unlocked = unlocked.add(milestoneId);
            changed = true;
        }

        if (changed && !stack.isEmpty()) {
            stack.set(ModDataComponents.MILESTONES, unlocked);
        }
    }

    private enum MilestoneResult {
        SUCCESS,        // Applied (Sound/Toast only)
        TRANSFORMED,    // Item replaced successfully
        FAILED          // Critical failure (e.g. could not find item to replace)
    }

    private static MilestoneResult performMilestone(ServerPlayer player, ItemStack stack, StatMilestone milestone, String milestoneId, UnlockedMilestones currentUnlocked) {
        // 1. Handle Transformation (Highest Priority & Risk)
        if (milestone.replacementItem().isPresent()) {
            ItemStack newStack = milestone.replacementItem().get().copy();

            // Prepare the new stack
            if (milestone.keepStats()) {
                // Copy all components to preserve identity/history
                newStack.applyComponents(stack.getComponents());
            }

            // Mark milestone unlocked on the NEW stack immediately
            // (Use a fresh lookup in case applyComponents overwrote it)
            UnlockedMilestones newUnlocked = newStack.getOrDefault(ModDataComponents.MILESTONES, UnlockedMilestones.EMPTY);
            newStack.set(ModDataComponents.MILESTONES, newUnlocked.add(milestoneId));

            // Attempt Replacement
            boolean success = replaceItemInInventory(player, stack, newStack);

            if (!success) {
                Memento.LOGGER.warn("Failed to transform item for player {}: Item not found in mutable inventory.", player.getName().getString());
                return MilestoneResult.FAILED;
            }

            // If successful, we point 'stack' to the new item for visual toasts below
            stack = newStack;
        }

        // 2. Visuals & Rewards (Safe to do now)
        if (milestone.titleName().isPresent()) {
            stack.set(DataComponents.CUSTOM_NAME, milestone.titleName().get());

            // Send Toast
            ItemStack visualStack = createVisualCopy(stack);
            PacketDistributor.sendToPlayer(player, new MilestoneToastPayload(
                    visualStack,
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

        return milestone.replacementItem().isPresent() ? MilestoneResult.TRANSFORMED : MilestoneResult.SUCCESS;
    }

    private static boolean replaceItemInInventory(ServerPlayer player, ItemStack oldStack, ItemStack newStack) {
        // 1. UUID Check (Robust)
        // If the item has a UUID, we look for that specific UUID.
        // This handles cases where the event system passed a copy, but the real item is in the inventory.
        if (oldStack.has(ModDataComponents.ITEM_UUID)) {
            UUID targetId = oldStack.get(ModDataComponents.ITEM_UUID);
            if (replaceByUuid(player, targetId, newStack)) return true;
        }

        // 2. Reference Equality Check (Fallback)
        // If no UUID (legacy item?), try to find the exact object instance.
        if (replaceByReference(player, oldStack, newStack)) return true;

        // 3. Modded/Curios Check
        if (CURIOS_LOADED) {
            return replaceCuriosItem(player, oldStack, newStack);
        }

        return false;
    }

    private static boolean replaceByUuid(ServerPlayer player, UUID targetId, ItemStack newStack) {
        Inventory inv = player.getInventory();

        if (scanAndReplace(inv.items, s -> s.has(ModDataComponents.ITEM_UUID) && s.get(ModDataComponents.ITEM_UUID).equals(targetId), newStack)) return true;
        if (scanAndReplace(inv.offhand, s -> s.has(ModDataComponents.ITEM_UUID) && s.get(ModDataComponents.ITEM_UUID).equals(targetId), newStack)) return true;
        return scanAndReplace(inv.armor, s -> s.has(ModDataComponents.ITEM_UUID) && s.get(ModDataComponents.ITEM_UUID).equals(targetId), newStack);
    }

    private static boolean replaceByReference(ServerPlayer player, ItemStack oldStack, ItemStack newStack) {
        Inventory inv = player.getInventory();

        if (scanAndReplace(inv.items, s -> s == oldStack, newStack)) return true;
        if (scanAndReplace(inv.offhand, s -> s == oldStack, newStack)) return true;
        if (scanAndReplace(inv.armor, s -> s == oldStack, newStack)) return true;

        // Mouse carried item (e.g. holding item while crafting/in menu)
        if (player.containerMenu != null && player.containerMenu.getCarried() == oldStack) {
            player.containerMenu.setCarried(newStack);
            return true;
        }

        return false;
    }

    private static boolean scanAndReplace(List<ItemStack> list, java.util.function.Predicate<ItemStack> predicate, ItemStack newStack) {
        for (int i = 0; i < list.size(); i++) {
            if (predicate.test(list.get(i))) {
                list.set(i, newStack);
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

    /**
     * Creates a lightweight copy of the stack containing only components relevant for rendering.
     */
    private static ItemStack createVisualCopy(ItemStack original) {
        ItemStack copy = new ItemStack(original.getItem());

        copyComponent(original, copy, DataComponents.CUSTOM_NAME);
        copyComponent(original, copy, DataComponents.ITEM_NAME);
        copyComponent(original, copy, DataComponents.RARITY);
        copyComponent(original, copy, DataComponents.ENCHANTMENTS);
        copyComponent(original, copy, DataComponents.ENCHANTMENT_GLINT_OVERRIDE);
        copyComponent(original, copy, DataComponents.TRIM);
        copyComponent(original, copy, DataComponents.DYED_COLOR);
        copyComponent(original, copy, DataComponents.PROFILE);
        copyComponent(original, copy, DataComponents.BANNER_PATTERNS);
        copyComponent(original, copy, DataComponents.BASE_COLOR);
        copyComponent(original, copy, DataComponents.POTION_CONTENTS);
        copyComponent(original, copy, DataComponents.CUSTOM_MODEL_DATA);

        copyComponent(original, copy, ModDataComponents.TRACKER_MAP);

        return copy;
    }

    private static <T> void copyComponent(ItemStack source, ItemStack dest, DeferredHolder<DataComponentType<?>, DataComponentType<T>> typeHolder) {
        copyComponent(source, dest, typeHolder.get());
    }

    private static <T> void copyComponent(ItemStack source, ItemStack dest, DataComponentType<T> type) {
        if (source.has(type)) {
            dest.set(type, source.get(type));
        }
    }
}