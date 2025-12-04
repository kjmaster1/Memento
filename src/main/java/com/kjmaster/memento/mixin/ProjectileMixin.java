package com.kjmaster.memento.mixin;

import com.kjmaster.memento.api.MementoAPI;
import com.kjmaster.memento.data.StatProjectileLogic;
import com.kjmaster.memento.data.StatProjectileLogicManager;
import com.kjmaster.memento.registry.ModDataAttachments;
import com.kjmaster.memento.registry.ModDataComponents;
import com.kjmaster.memento.util.ItemContextHelper;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.UUID;

@Mixin(Projectile.class)
public abstract class ProjectileMixin {

    // We only hook the 'shoot' method to capture the Source Stack (Context)
    // and to modify Inaccuracy (Spread), which must be done during calculation.

    @Inject(method = "shoot(DDDFF)V", at = @At("HEAD"))
    private void memento$captureSource(double x, double y, double z, float velocity, float inaccuracy, CallbackInfo ci) {
        Projectile self = (Projectile) (Object) this;

        if (self.getOwner() instanceof LivingEntity living) {
            if (!self.hasData(ModDataAttachments.SOURCE_STACK) || self.getData(ModDataAttachments.SOURCE_STACK).isEmpty()) {
                ItemStack weapon = memento$findWeapon(living);
                if (!weapon.isEmpty()) {
                    if (!weapon.has(ModDataComponents.ITEM_UUID)) {
                        weapon.set(ModDataComponents.ITEM_UUID, UUID.randomUUID());
                    }
                    self.setData(ModDataAttachments.SOURCE_STACK, weapon);
                }
            }
        }
    }

    // "Soft" Mixin: Only modifies the inaccuracy float parameter.
    // If another mod overrides shoot(), this simply won't run, which is safe failure.
    @ModifyVariable(method = "shoot(DDDFF)V", at = @At("HEAD"), ordinal = 1, argsOnly = true)
    private float memento$modifyInaccuracy(float inaccuracy) {
        Projectile self = (Projectile) (Object) this;
        // We can't easily rely on SOURCE_STACK attachment here as it might not be set yet (race condition in Mixin HEAD).
        // So we re-fetch weapon context quickly.
        if (self.getOwner() instanceof LivingEntity living) {
            ItemStack stack = memento$findWeapon(living);
            if (!stack.isEmpty()) {
                double multiplier = 1.0;
                // Calculate multiplier
                for (StatProjectileLogic rule : StatProjectileLogicManager.getAllRules()) {
                    long val = MementoAPI.getStat(stack, rule.stat());
                    if (val >= rule.minInfo()) {
                        multiplier *= rule.inaccuracyModifier();
                    }
                }
                return (float) (inaccuracy * multiplier);
            }
        }
        return inaccuracy;
    }

    @Unique
    private ItemStack memento$findWeapon(LivingEntity living) {
        // IMPROVEMENT: Prioritize the item currently being used (e.g. Bow being drawn, Trident being aimed)
        // This is more reliable than hand scanning for items that have a "charging" state.
        ItemStack stack = living.getUseItem();
        if (!stack.isEmpty() && ItemContextHelper.isRangedWeapon(stack)) return stack;

        // Fallback: Check Main Hand for any Ranged Weapon (e.g. Crossbows, Instant-cast wands)
        stack = living.getMainHandItem();
        if (!stack.isEmpty() && ItemContextHelper.isRangedWeapon(stack)) return stack;

        // Fallback: Check Off Hand
        stack = living.getOffhandItem();
        if (!stack.isEmpty() && ItemContextHelper.isRangedWeapon(stack)) return stack;

        return ItemStack.EMPTY;
    }
}