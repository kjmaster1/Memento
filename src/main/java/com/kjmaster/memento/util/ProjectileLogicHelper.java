package com.kjmaster.memento.util;

import com.kjmaster.memento.api.MementoAPI;
import com.kjmaster.memento.data.StatProjectileLogic;
import com.kjmaster.memento.data.StatProjectileLogicManager;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;

public class ProjectileLogicHelper {

    public record BallisticModifiers(float velocity, float inaccuracy) {
    }

    /**
     * Calculates multipliers and applies Damage side effects.
     * Uses optimized lookups via getRules(stack).
     */
    public static BallisticModifiers applyBallistics(ItemStack stack, Projectile projectile, float baseVelocity, float baseInaccuracy) {
        double velocityMult = 1.0;
        double inaccuracyMult = 1.0;
        double damageMult = 1.0;

        for (StatProjectileLogic rule : StatProjectileLogicManager.getRules(stack)) {
            long val = MementoAPI.getStat(stack, rule.stat());
            if (val >= rule.minInfo()) {
                velocityMult *= rule.velocityMultiplier();
                inaccuracyMult *= rule.inaccuracyModifier();
                damageMult *= rule.damageMultiplier();
            }
        }

        applyDamage(projectile, damageMult);

        return new BallisticModifiers((float) (baseVelocity * velocityMult), (float) (baseInaccuracy * inaccuracyMult));
    }

    /**
     * Optimized single-pass application for EntityJoinLevelEvent.
     * Applies both velocity scaling and damage modifiers in one loop.
     */
    public static void applyEntityJoinBallistics(ItemStack stack, Projectile projectile) {
        double velocityMult = 1.0;
        double damageMult = 1.0;

        for (StatProjectileLogic rule : StatProjectileLogicManager.getRules(stack)) {
            if (MementoAPI.getStat(stack, rule.stat()) >= rule.minInfo()) {
                velocityMult *= rule.velocityMultiplier();
                damageMult *= rule.damageMultiplier();
            }
        }

        if (velocityMult != 1.0) {
            projectile.setDeltaMovement(projectile.getDeltaMovement().scale(velocityMult));
        }

        applyDamage(projectile, damageMult);
    }

    public static float getVelocityMultiplier(ItemStack stack) {
        double velocityMult = 1.0;
        for (StatProjectileLogic rule : StatProjectileLogicManager.getRules(stack)) {
            if (MementoAPI.getStat(stack, rule.stat()) >= rule.minInfo()) {
                velocityMult *= rule.velocityMultiplier();
            }
        }
        return (float) velocityMult;
    }

    public static void applyDamageModifier(ItemStack stack, Projectile projectile) {
        double damageMult = 1.0;
        for (StatProjectileLogic rule : StatProjectileLogicManager.getRules(stack)) {
            if (MementoAPI.getStat(stack, rule.stat()) >= rule.minInfo()) {
                damageMult *= rule.damageMultiplier();
            }
        }
        applyDamage(projectile, damageMult);
    }

    private static void applyDamage(Projectile projectile, double multiplier) {
        if (multiplier != 1.0 && projectile instanceof AbstractArrow arrow) {
            arrow.setBaseDamage(arrow.getBaseDamage() * multiplier);
        }
    }
}