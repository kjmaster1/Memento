package com.kjmaster.memento.event;

import com.kjmaster.memento.data.StatRequirementManager;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockEvent;

import java.util.Optional;

public class RestrictionEvents {

    // Prevent Right-Click Usage (Bows, Food, Shields, etc.)
    @SubscribeEvent
    public static void onItemUse(PlayerInteractEvent.RightClickItem event) {
        Player player = event.getEntity();
        ItemStack stack = event.getItemStack();

        if (checkAndWarn(player, stack)) {
            event.setCanceled(true);
        }
    }

    // Prevent Attacks (Swords)
    @SubscribeEvent
    public static void onAttack(AttackEntityEvent event) {
        Player player = event.getEntity();
        ItemStack stack = player.getMainHandItem();

        if (checkAndWarn(player, stack)) {
            event.setCanceled(true);
        }
    }

    // Prevent Mining - Speed Modification (Mining Fatigue effect)
    @SubscribeEvent
    public static void onBreakSpeed(PlayerEvent.BreakSpeed event) {
        Player player = event.getEntity();
        ItemStack stack = player.getMainHandItem();

        // Silent check for speed (no chat spam)
        Optional<String> error = StatRequirementManager.checkRestriction(player, stack);
        if (error.isPresent()) {
            event.setNewSpeed(0.0f);
            event.setCanceled(true);
        }
    }

    // Prevent Block Break (Final check)
    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        Player player = event.getPlayer();
        ItemStack stack = player.getMainHandItem();

        if (checkAndWarn(player, stack)) {
            event.setCanceled(true);
        }
    }

    // Prevent Left Click Interaction (General)
    @SubscribeEvent
    public static void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
        Player player = event.getEntity();
        ItemStack stack = event.getItemStack();

        // Use warning here as it's an explicit action
        if (checkAndWarn(player, stack)) {
            event.setCanceled(true);
        }
    }

    private static boolean checkAndWarn(Player player, ItemStack stack) {
        if (player.isCreative()) return false;

        Optional<String> error = StatRequirementManager.checkRestriction(player, stack);
        if (error.isPresent()) {
            // Cooldown to prevent chat spam (40 ticks / 2 sec)
            if (!player.getCooldowns().isOnCooldown(stack.getItem())) {
                player.displayClientMessage(Component.translatable(error.get()).withStyle(ChatFormatting.RED), true);
                player.getCooldowns().addCooldown(stack.getItem(), 40);
            }
            return true;
        }
        return false;
    }
}