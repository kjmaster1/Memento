package com.kjmaster.memento.event;

import com.kjmaster.memento.Config;
import com.kjmaster.memento.api.MementoAPI;
import com.kjmaster.memento.registry.ModDataAttachments;
import com.kjmaster.memento.registry.ModStats;
import com.kjmaster.memento.util.ItemContextHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.common.IShearable;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.ProjectileImpactEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingShieldBlockEvent;
import net.neoforged.neoforge.event.entity.player.ItemFishedEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockEvent;

public class ContextEvents {

    // --- Shields: Damage Blocked ---
    @SubscribeEvent
    public static void onShieldBlock(LivingShieldBlockEvent event) {
        if (!Config.isDefaultEnabled(ModStats.DAMAGE_BLOCKED)) return;
        if (event.getEntity() instanceof ServerPlayer player) {
            ItemStack useItem = player.getUseItem();
            if (ItemContextHelper.isShield(useItem)) {
                long blocked = (long) (event.getBlockedDamage() * 100); // Scale logic similar to Damage Taken
                if (blocked > 0) {
                    MementoAPI.incrementStat(player, useItem, ModStats.DAMAGE_BLOCKED, blocked);
                }
            }
        }
    }

    // --- Ranged: Shots Fired & Longest Shot ---
    @SubscribeEvent
    public static void onEntityJoin(EntityJoinLevelEvent event) {
        if (event.getEntity() instanceof Projectile projectile && !event.getLevel().isClientSide) {
            if (projectile.getOwner() instanceof ServerPlayer player) {
                // Attach Origin Data for Longest Shot calculation later
                if (Config.isDefaultEnabled(ModStats.LONGEST_SHOT)) {
                    projectile.setData(ModDataAttachments.PROJECTILE_ORIGIN, projectile.position());
                }

                if (!Config.isDefaultEnabled(ModStats.SHOTS_FIRED)) return;

                // Identify the weapon using the Attachment (preferred) or Fallback
                ItemStack activeItem = ItemStack.EMPTY;

                if (projectile.hasData(ModDataAttachments.SOURCE_STACK)) {
                    activeItem = projectile.getData(ModDataAttachments.SOURCE_STACK);
                }

                // Fallback if Mixin didn't catch it (unlikely but possible for some modded projectiles)
                if (activeItem.isEmpty()) {
                    activeItem = player.getUseItem();
                    if (activeItem.isEmpty()) activeItem = player.getMainHandItem();
                }

                if (ItemContextHelper.isRangedWeapon(activeItem)) {
                    // VALIDATION: Ensure the item is valid and present before updating
                    if (isItemInInventory(player, activeItem)) {
                        MementoAPI.incrementStat(player, activeItem, ModStats.SHOTS_FIRED, 1);
                    }
                }
            }
        }
    }

    @SubscribeEvent
    public static void onProjectileImpact(ProjectileImpactEvent event) {

        if (!Config.isDefaultEnabled(ModStats.LONGEST_SHOT)) return;

        Projectile projectile = event.getProjectile();
        if (!projectile.level().isClientSide && projectile.getOwner() instanceof ServerPlayer player) {
            // Calculate Distance
            Vec3 origin = projectile.getData(ModDataAttachments.PROJECTILE_ORIGIN);
            if (origin.equals(Vec3.ZERO)) return; // No data attached

            double distance = origin.distanceTo(projectile.position());
            long distanceCm = (long) (distance * 100);

            // Use the Attached Weapon for context
            ItemStack held = ItemStack.EMPTY;
            if (projectile.hasData(ModDataAttachments.SOURCE_STACK)) {
                held = projectile.getData(ModDataAttachments.SOURCE_STACK);
            }

            // Fallback logic (Old heuristic)
            if (held.isEmpty()) {
                held = player.getMainHandItem();
                if (!ItemContextHelper.isRangedWeapon(held)) {
                    held = player.getOffhandItem();
                }
            }

            if (ItemContextHelper.isRangedWeapon(held)) {
                // VALIDATION: Critical check to ensure we don't update a "ghost" item reference
                // if the player has dropped, destroyed, or split the stack since firing.
                if (isItemInInventory(player, held)) {
                    MementoAPI.maximizeStat(player, held, ModStats.LONGEST_SHOT, distanceCm);
                }
            }
        }
    }

    // --- Fishing Rods: Fish Caught ---
    @SubscribeEvent
    public static void onItemFished(ItemFishedEvent event) {

        if (!Config.isDefaultEnabled(ModStats.ITEMS_CAUGHT)) return;

        if (event.getEntity() instanceof ServerPlayer player) {
            ItemStack stack = player.getMainHandItem();
            if (ItemContextHelper.isFishingRod(stack)) {
                // Count number of items caught (usually 1, but treasure can vary)
                MementoAPI.incrementStat(player, stack, ModStats.ITEMS_CAUGHT, event.getDrops().size());
            } else {
                // Check offhand
                stack = player.getOffhandItem();
                if (ItemContextHelper.isFishingRod(stack)) {
                    MementoAPI.incrementStat(player, stack, ModStats.ITEMS_CAUGHT, event.getDrops().size());
                }
            }
        }
    }

    // --- Flint & Steel: Fires Started ---
    @SubscribeEvent
    public static void onBlockPlace(BlockEvent.EntityPlaceEvent event) {

        if (!Config.isDefaultEnabled(ModStats.FIRES_STARTED)) return;

        if (event.getEntity() instanceof ServerPlayer player && event.getPlacedBlock().getBlock() instanceof BaseFireBlock) {
            // Check if placed via an item
            ItemStack usedItem = player.getMainHandItem();

            if (ItemContextHelper.isFlintAndSteel(usedItem)) {
                MementoAPI.incrementStat(player, usedItem, ModStats.FIRES_STARTED, 1);
            } else {
                if (ItemContextHelper.isFlintAndSteel(player.getOffhandItem())) {
                    MementoAPI.incrementStat(player, usedItem, ModStats.FIRES_STARTED, 1);
                }
            }
        }
    }

    // --- Shears: Wool Clipped ---
    @SubscribeEvent
    public static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {

        if (!Config.isDefaultEnabled(ModStats.MOBS_SHEARED)) return;

        if (event.getEntity() instanceof ServerPlayer player) {
            ItemStack stack = event.getItemStack();
            if (ItemContextHelper.isShears(stack)) {
                // Check if target is shearable (NeoForge capability or standard class)
                if (event.getTarget() instanceof IShearable shearable) {
                    if (shearable.isShearable(player, stack, event.getLevel(), event.getPos())) {
                        // We count the attempt/success.
                        // Note: This fires before the shear happens, but it's the best hook available
                        // without using mixins into ShearItem.
                        MementoAPI.incrementStat(player, stack, ModStats.MOBS_SHEARED, 1);
                    }
                }
            }
        }
    }

    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event) {

        if (!Config.isDefaultEnabled(ModStats.BLOCKS_BROKEN)) return;

        if (event.getPlayer() instanceof ServerPlayer player) {
            ItemStack heldItem = player.getMainHandItem();
            BlockState state = event.getState();

            if (ItemContextHelper.isEffectiveMiningTool(heldItem, state)) {
                MementoAPI.incrementStat(player, heldItem, ModStats.BLOCKS_BROKEN, 1);
            }
        }
    }

    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {

        if (!Config.isDefaultEnabled(ModStats.ENTITIES_KILLED)) return;

        if (event.getSource().getEntity() instanceof ServerPlayer player) {

            ItemStack weapon = event.getSource().getWeaponItem();

            if (weapon == null) {
                return;
            }

            if (ItemContextHelper.isMeleeWeapon(weapon) || ItemContextHelper.isRangedWeapon(weapon)) {
                MementoAPI.incrementStat(player, weapon, ModStats.ENTITIES_KILLED, 1);
            }
        }
    }

    @SubscribeEvent
    public static void onLivingDamage(LivingDamageEvent.Post event) {

        if (!Config.isDefaultEnabled(ModStats.DAMAGE_TAKEN)) return;

        if (event.getEntity() instanceof ServerPlayer player) {
            float damageAmount = event.getNewDamage();
            long scaledDamage = (long) (damageAmount * 100);

            if (scaledDamage <= 0) return;

            for (ItemStack stack : player.getArmorSlots()) {
                if (!stack.isEmpty() && ItemContextHelper.isArmor(stack)) {
                    MementoAPI.incrementStat(player, stack, ModStats.DAMAGE_TAKEN, scaledDamage);
                }
            }
        }
    }

    /**
     * Checks if the specific ItemStack object instance is still present in the player's inventory.
     * This uses reference equality (==) to ensure we are targeting the exact object attached to the projectile,
     * protecting against split stacks or ghost items.
     */
    private static boolean isItemInInventory(ServerPlayer player, ItemStack stack) {
        if (stack.isEmpty()) return false;

        // Check main inventory (includes hotbar)
        for (ItemStack s : player.getInventory().items) {
            if (s == stack) return true;
        }
        // Check offhand
        for (ItemStack s : player.getInventory().offhand) {
            if (s == stack) return true;
        }
        // Check armor slots
        for (ItemStack s : player.getInventory().armor) {
            if (s == stack) return true;
        }

        return false;
    }
}