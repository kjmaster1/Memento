package com.kjmaster.memento.mixin;

import com.kjmaster.memento.data.StatProjectileLogicManager;
import com.kjmaster.memento.util.IMementoProjectile;
import com.kjmaster.memento.util.ProjectileLogicHelper;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ProjectileWeaponItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;

@Mixin(Projectile.class)
public abstract class ProjectileMixin implements IMementoProjectile {

    @Unique
    private boolean memento$ballisticsApplied = false;

    @Override
    public boolean memento$isBallisticsApplied() {
        return memento$ballisticsApplied;
    }

    @Override
    public void memento$setBallisticsApplied(boolean applied) {
        this.memento$ballisticsApplied = applied;
    }

    // Intercept: shootFromRotation (Bows, Tridents, Aimed Modded Weapons)
    @WrapOperation(
            method = "shootFromRotation",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/projectile/Projectile;shoot(DDDFF)V")
    )
    private void memento$onShootFromRotation(Projectile instance, double x, double y, double z, float velocity, float inaccuracy, Operation<Void> original, Entity shooter, float xRot, float yRot, float zRot, float velocityArg, float inaccuracyArg) {
        memento$applyLogic(instance, shooter, velocity, inaccuracy, original, x, y, z);
    }

    @Inject(method = "shoot(DDDFF)V", at = @At("HEAD"))
    private void memento$onShootHead(double x, double y, double z, float velocity, float inaccuracy, org.spongepowered.asm.mixin.injection.callback.CallbackInfo ci) {
        // Optimization: Skip all logic if no rules are defined
        if (StatProjectileLogicManager.getAllRules().isEmpty()) return;

        if (this.memento$isBallisticsApplied()) return;

        Projectile self = (Projectile) (Object) this;
        Entity owner = self.getOwner();

        if (owner instanceof LivingEntity living) {
            ItemStack stack = memento$findWeapon(living);

            if (!stack.isEmpty()) {
                ProjectileLogicHelper.applyDamageModifier(stack, self);
            }
        }
    }

    @Inject(method = "shoot(DDDFF)V", at = @At("RETURN"))
    private void memento$onShootReturn(double x, double y, double z, float velocity, float inaccuracy, org.spongepowered.asm.mixin.injection.callback.CallbackInfo ci) {
        // Optimization: Skip all logic if no rules are defined
        if (StatProjectileLogicManager.getAllRules().isEmpty()) return;

        if (this.memento$isBallisticsApplied()) return;

        Projectile self = (Projectile) (Object) this;
        Entity owner = self.getOwner();

        if (owner instanceof LivingEntity living) {
            ItemStack stack = memento$findWeapon(living);
            if (!stack.isEmpty()) {
                // Apply Velocity Multiplier to the final vector
                float mult = ProjectileLogicHelper.getVelocityMultiplier(stack);
                if (mult != 1.0f) {
                    self.setDeltaMovement(self.getDeltaMovement().scale(mult));
                }

                // Mark applied so we don't do it again
                this.memento$setBallisticsApplied(true);
            }
        }
    }

    @Unique
    private void memento$applyLogic(Projectile instance, Entity shooter, float velocity, float inaccuracy, Operation<Void> original, double x, double y, double z) {
        // Optimization: Skip all logic if no rules are defined
        if (StatProjectileLogicManager.getAllRules().isEmpty()) {
            original.call(instance, x, y, z, velocity, inaccuracy);
            return;
        }

        if (this.memento$isBallisticsApplied()) {
            original.call(instance, x, y, z, velocity, inaccuracy);
            return;
        }

        if (shooter instanceof LivingEntity living) {
            ItemStack stack = memento$findWeapon(living);
            if (!stack.isEmpty()) {
                var mods = ProjectileLogicHelper.applyBallistics(stack, instance, velocity, inaccuracy);
                original.call(instance, x, y, z, mods.velocity(), mods.inaccuracy());
                this.memento$setBallisticsApplied(true);
                return;
            }
        }

        original.call(instance, x, y, z, velocity, inaccuracy);
    }

    @Unique
    private ItemStack memento$findWeapon(LivingEntity living) {
        // 1. Check Active Item (Bows being drawn)
        ItemStack stack = living.getUseItem();
        if (!stack.isEmpty() && stack.getItem() instanceof ProjectileWeaponItem) return stack;

        // 2. Check Main Hand (Crossbows, Instant-fire wands)
        stack = living.getMainHandItem();
        if (!stack.isEmpty() && stack.getItem() instanceof ProjectileWeaponItem) return stack;

        // 3. Check OffHand
        stack = living.getOffhandItem();
        if (!stack.isEmpty() && stack.getItem() instanceof ProjectileWeaponItem) return stack;

        // 4. Fallback: Return Main Hand anyway (for non-ProjectileWeaponItem modded items)
        return living.getMainHandItem();
    }
}