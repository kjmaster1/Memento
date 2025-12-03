package com.kjmaster.memento.util;

import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.ItemAbilities;
import net.neoforged.neoforge.common.Tags;

public class ItemContextHelper {

    public static boolean isShield(ItemStack stack) {
        return stack.canPerformAction(ItemAbilities.SHIELD_BLOCK)
                || stack.getItem() instanceof ShieldItem
                || stack.is(Tags.Items.TOOLS_SHIELD);
    }

    public static boolean isRangedWeapon(ItemStack stack) {
        return stack.getItem() instanceof ProjectileWeaponItem
                || stack.getItem() instanceof TridentItem
                || stack.is(Tags.Items.RANGED_WEAPON_TOOLS);
    }

    public static boolean isFishingRod(ItemStack stack) {
        return stack.canPerformAction(ItemAbilities.FISHING_ROD_CAST)
                || stack.getItem() instanceof FishingRodItem
                || stack.is(Tags.Items.TOOLS_FISHING_ROD);
    }

    public static boolean isFlintAndSteel(ItemStack stack) {
        return stack.canPerformAction(ItemAbilities.FIRESTARTER_LIGHT)
                || stack.getItem() instanceof FlintAndSteelItem
                || stack.is(Tags.Items.TOOLS_IGNITER);
    }

    public static boolean isShears(ItemStack stack) {
        return stack.canPerformAction(ItemAbilities.SHEARS_CARVE)
                || stack.canPerformAction(ItemAbilities.SHEARS_DISARM)
                || stack.canPerformAction(ItemAbilities.SHEARS_DIG)
                || stack.canPerformAction(ItemAbilities.SHEARS_HARVEST)
                || stack.canPerformAction(ItemAbilities.SHEARS_TRIM)
                || stack.canPerformAction(ItemAbilities.SHEARS_REMOVE_ARMOR)
                || stack.getItem() instanceof ShearsItem
                || stack.is(Tags.Items.TOOLS_SHEAR);
    }

    public static boolean isEffectiveMiningTool(ItemStack stack, BlockState state) {
        return stack.isCorrectToolForDrops(state);
    }

    public static boolean isMeleeWeapon(ItemStack stack) {
        return stack.getItem() instanceof SwordItem
                || stack.getItem() instanceof TridentItem
                || stack.getItem() instanceof AxeItem
                || stack.is(ItemTags.SWORDS)
                || stack.is(ItemTags.AXES)
                || stack.is(ItemTags.WEAPON_ENCHANTABLE)
                || stack.canPerformAction(ItemAbilities.SWORD_SWEEP);
    }

    public static boolean isArmor(ItemStack stack) {
        return stack.getItem() instanceof ArmorItem
                || stack.is(ItemTags.ARMOR_ENCHANTABLE);
    }

    public static boolean isHoe(ItemStack stack) {
        return stack.getItem() instanceof HoeItem
                || stack.is(ItemTags.HOES)
                || stack.canPerformAction(ItemAbilities.HOE_DIG);
    }

    public static boolean isElytra(ItemStack stack, LivingEntity entity) {
        return stack.canElytraFly(entity);
    }
}