package com.kjmaster.memento.event;

import com.kjmaster.memento.Memento;
import com.kjmaster.memento.api.event.StatChangeEvent;
import com.kjmaster.memento.compat.CompatHandler;
import com.kjmaster.memento.component.UnlockedMilestones;
import com.kjmaster.memento.data.StatMilestone;
import com.kjmaster.memento.data.StatMilestoneManager;
import com.kjmaster.memento.network.MilestoneToastPayload;
import com.kjmaster.memento.registry.ModDataComponents;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
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
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.registries.DeferredHolder;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;

public class MilestoneEventHandler {

    private static final Map<LivingEntity, Long> LAST_FEEDBACK_TIME = new WeakHashMap<>();
    private static final long FEEDBACK_COOLDOWN = 1000L;

    @SubscribeEvent
    public static void onStatChange(StatChangeEvent.Post event) {
        checkMilestones(event.getEntity(), event.getItem(), event.getStatId(), event.getNewValue());
    }

    private static void checkMilestones(LivingEntity entity, ItemStack stack, ResourceLocation statId, long newValue) {
        List<StatMilestone> milestones = StatMilestoneManager.getMilestonesFor(statId);
        if (milestones.isEmpty()) return;

        UnlockedMilestones unlocked = stack.getOrDefault(ModDataComponents.MILESTONES, UnlockedMilestones.EMPTY);
        boolean changed = false;
        ResourceLocation itemKey = BuiltInRegistries.ITEM.getKey(stack.getItem());

        for (StatMilestone milestone : milestones) {
            // Apply Item Filter
            if (milestone.items().isPresent() && !milestone.items().get().contains(itemKey)) {
                continue;
            }

            String milestoneId = statId.toString() + "/" + milestone.targetValue();

            if (unlocked.hasUnlocked(milestoneId)) continue;
            if (newValue < milestone.targetValue()) continue;

            if (milestone.itemRequirement().isPresent()) {
                if (!milestone.itemRequirement().get().test(stack)) continue;
            }

            MilestoneResult result = performMilestone(entity, stack, milestone, milestoneId, unlocked);

            if (result == MilestoneResult.FAILED) continue;
            if (result == MilestoneResult.TRANSFORMED) return;

            unlocked = unlocked.add(milestoneId);
            changed = true;
        }

        if (changed && !stack.isEmpty()) {
            stack.set(ModDataComponents.MILESTONES, unlocked);
        }
    }

    private enum MilestoneResult { SUCCESS, TRANSFORMED, FAILED }

    private static MilestoneResult performMilestone(LivingEntity entity, ItemStack stack, StatMilestone milestone, String milestoneId, UnlockedMilestones currentUnlocked) {
        if (milestone.replacementItem().isPresent()) {
            ItemStack newStack = milestone.replacementItem().get().copy();
            if (milestone.keepStats()) newStack.applyComponents(stack.getComponents());

            UnlockedMilestones newUnlocked = newStack.getOrDefault(ModDataComponents.MILESTONES, UnlockedMilestones.EMPTY);
            newStack.set(ModDataComponents.MILESTONES, newUnlocked.add(milestoneId));

            if (!replaceItem(entity, stack, newStack)) {
                Memento.LOGGER.warn("Failed to transform item for entity {}: Item not found.", entity.getName().getString());
                return MilestoneResult.FAILED;
            }
            stack = newStack;
        }

        boolean allowFeedback = false;
        long now = System.currentTimeMillis();
        long lastTime = LAST_FEEDBACK_TIME.getOrDefault(entity, 0L);

        if (now - lastTime > FEEDBACK_COOLDOWN) {
            allowFeedback = true;
            LAST_FEEDBACK_TIME.put(entity, now);
        }

        if (entity instanceof ServerPlayer player) {
            if (allowFeedback) {
                player.serverLevel().sendParticles(ParticleTypes.HAPPY_VILLAGER, player.getX(), player.getY() + 1.0, player.getZ(), 20, 0.5, 0.5, 0.5, 0.1);
                player.serverLevel().sendParticles(ParticleTypes.END_ROD, player.getX(), player.getY() + 1.0, player.getZ(), 10, 0.3, 0.5, 0.3, 0.05);

                if (milestone.titleName().isPresent()) {
                    stack.set(DataComponents.CUSTOM_NAME, milestone.titleName().get());
                    PacketDistributor.sendToPlayer(player, new MilestoneToastPayload(createVisualCopy(stack), Component.translatable("memento.toast.levelup"), milestone.titleName().get()));
                }

                if (milestone.soundId().isPresent()) {
                    ResourceLocation soundLoc = ResourceLocation.parse(milestone.soundId().get());
                    SoundEvent sound = BuiltInRegistries.SOUND_EVENT.get(soundLoc);
                    if (sound != null) player.level().playSound(null, player.blockPosition(), sound, SoundSource.PLAYERS, 1.0f, 1.0f);
                }
            }

            if (!milestone.rewards().isEmpty()) {
                CommandSourceStack source = player.createCommandSourceStack().withPermission(2).withSuppressedOutput();
                for (String command : milestone.rewards()) {
                    player.server.getCommands().performPrefixedCommand(source, command);
                }
            }
        } else {
            if (allowFeedback) {
                if (entity.level() instanceof net.minecraft.server.level.ServerLevel serverLevel) {
                    serverLevel.sendParticles(ParticleTypes.HAPPY_VILLAGER, entity.getX(), entity.getY() + 0.5, entity.getZ(), 10, 0.5, 0.5, 0.5, 0.1);
                }
                if (milestone.soundId().isPresent()) {
                    ResourceLocation soundLoc = ResourceLocation.parse(milestone.soundId().get());
                    SoundEvent sound = BuiltInRegistries.SOUND_EVENT.get(soundLoc);
                    if (sound != null) entity.level().playSound(null, entity.blockPosition(), sound, SoundSource.NEUTRAL, 1.0f, 1.0f);
                }
            }
        }

        return milestone.replacementItem().isPresent() ? MilestoneResult.TRANSFORMED : MilestoneResult.SUCCESS;
    }

    private static boolean replaceItem(LivingEntity entity, ItemStack oldStack, ItemStack newStack) {
        if (oldStack.has(ModDataComponents.ITEM_UUID)) {
            UUID targetId = oldStack.get(ModDataComponents.ITEM_UUID);
            if (replaceByUuid(entity, targetId, newStack)) return true;
        }
        if (replaceByReference(entity, oldStack, newStack)) return true;
        if (entity instanceof ServerPlayer player) {
            if (CompatHandler.replaceModdedItem(player, oldStack, newStack)) return true;
        }
        return false;
    }

    private static boolean replaceByUuid(LivingEntity entity, UUID targetId, ItemStack newStack) {
        if (entity instanceof ServerPlayer player) {
            Inventory inv = player.getInventory();
            if (scanAndReplace(inv.items, s -> s.has(ModDataComponents.ITEM_UUID) && s.get(ModDataComponents.ITEM_UUID).equals(targetId), newStack)) return true;
            if (scanAndReplace(inv.offhand, s -> s.has(ModDataComponents.ITEM_UUID) && s.get(ModDataComponents.ITEM_UUID).equals(targetId), newStack)) return true;
            return scanAndReplace(inv.armor, s -> s.has(ModDataComponents.ITEM_UUID) && s.get(ModDataComponents.ITEM_UUID).equals(targetId), newStack);
        } else {
            for (EquipmentSlot slot : EquipmentSlot.values()) {
                ItemStack s = entity.getItemBySlot(slot);
                if (s.has(ModDataComponents.ITEM_UUID) && s.get(ModDataComponents.ITEM_UUID).equals(targetId)) {
                    entity.setItemSlot(slot, newStack);
                    return true;
                }
            }
            return false;
        }
    }

    private static boolean replaceByReference(LivingEntity entity, ItemStack oldStack, ItemStack newStack) {
        if (entity instanceof ServerPlayer player) {
            Inventory inv = player.getInventory();
            if (scanAndReplace(inv.items, s -> s == oldStack, newStack)) return true;
            if (scanAndReplace(inv.offhand, s -> s == oldStack, newStack)) return true;
            if (scanAndReplace(inv.armor, s -> s == oldStack, newStack)) return true;
            if (player.containerMenu != null && player.containerMenu.getCarried() == oldStack) {
                player.containerMenu.setCarried(newStack);
                return true;
            }
        } else {
            for (EquipmentSlot slot : EquipmentSlot.values()) {
                if (entity.getItemBySlot(slot) == oldStack) {
                    entity.setItemSlot(slot, newStack);
                    return true;
                }
            }
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
        if (source.has(type)) dest.set(type, source.get(type));
    }
}