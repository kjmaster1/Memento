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

    @Unique
    private ItemStack memento$cachedSourceStack = ItemStack.EMPTY;

    @Inject(method = "shoot(DDDFF)V", at = @At("HEAD"))
    private void memento$captureSource(double x, double y, double z, float velocity, float inaccuracy, CallbackInfo ci) {
        Projectile self = (Projectile) (Object) this;

        if (self.getOwner() instanceof LivingEntity living) {
            if (!self.hasData(ModDataAttachments.SOURCE_STACK) || self.getData(ModDataAttachments.SOURCE_STACK).isEmpty()) {

                ItemStack weapon = ItemStack.EMPTY;

                if (!memento$cachedSourceStack.isEmpty()) {
                    weapon = memento$cachedSourceStack;
                }
                else {
                    weapon = memento$findWeapon(living);
                }

                if (!weapon.isEmpty()) {
                    if (!weapon.has(ModDataComponents.ITEM_UUID)) {
                        weapon.set(ModDataComponents.ITEM_UUID, UUID.randomUUID());
                    }
                    self.setData(ModDataAttachments.SOURCE_STACK, weapon);
                }
            }
        }

        memento$cachedSourceStack = ItemStack.EMPTY;
    }

    @ModifyVariable(method = "shoot(DDDFF)V", at = @At("HEAD"), ordinal = 1, argsOnly = true)
    private float memento$modifyInaccuracy(float inaccuracy) {
        Projectile self = (Projectile) (Object) this;

        if (self.getOwner() instanceof LivingEntity living) {
            if (memento$cachedSourceStack.isEmpty()) {
                memento$cachedSourceStack = memento$findWeapon(living);
            }

            ItemStack stack = memento$cachedSourceStack;

            if (!stack.isEmpty()) {
                double multiplier = 1.0;
                for (StatProjectileLogic rule : StatProjectileLogicManager.getRules(stack)) {
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
        ItemStack stack = living.getUseItem();
        if (!stack.isEmpty() && ItemContextHelper.isRangedWeapon(stack)) return stack;

        stack = living.getMainHandItem();
        if (!stack.isEmpty() && ItemContextHelper.isRangedWeapon(stack)) return stack;

        stack = living.getOffhandItem();
        if (!stack.isEmpty() && ItemContextHelper.isRangedWeapon(stack)) return stack;

        return ItemStack.EMPTY;
    }
}