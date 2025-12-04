package com.kjmaster.memento.mixin;

import com.kjmaster.memento.api.MementoAPI;
import com.kjmaster.memento.data.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.function.Consumer;

@Mixin(ItemStack.class)
public abstract class ItemStackMixin {

    @Inject(method = "getRarity", at = @At("RETURN"), cancellable = true)
    private void memento$modifyRarity(CallbackInfoReturnable<Rarity> cir) {
        ItemStack stack = (ItemStack) (Object) this;

        // Don't run logic on empty stacks
        if (stack.isEmpty()) return;

        Rarity currentRarity = cir.getReturnValue();
        Rarity bestRarity = currentRarity;

        for (StatVisualPrestige rule : StatVisualPrestigeManager.getAllRules()) {
            if (rule.rarity().isEmpty()) continue;

            long val = MementoAPI.getStat(stack, rule.stat());
            if (val >= rule.minInfo()) {
                Rarity ruleRarity = rule.rarity().get();
                // Upgrade only (Prevent downgrading if item is already Epic)
                if (ruleRarity.ordinal() > bestRarity.ordinal()) {
                    bestRarity = ruleRarity;
                }
            }
        }

        if (bestRarity != currentRarity) {
            cir.setReturnValue(bestRarity);
        }
    }

    @Inject(method = "hasFoil", at = @At("RETURN"), cancellable = true)
    private void memento$modifyGlint(CallbackInfoReturnable<Boolean> cir) {
        // If it already has glint (enchanted), we don't need to do anything
        if (cir.getReturnValue()) return;

        ItemStack stack = (ItemStack) (Object) this;
        if (stack.isEmpty()) return;

        for (StatVisualPrestige rule : StatVisualPrestigeManager.getAllRules()) {
            // Only check rules that WANT to force glint
            if (rule.glint().orElse(false)) {
                long val = MementoAPI.getStat(stack, rule.stat());
                if (val >= rule.minInfo()) {
                    cir.setReturnValue(true);
                    return; // Found a rule, apply and exit
                }
            }
        }
    }

    @Inject(method = "hurtAndBreak(ILnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/entity/LivingEntity;Ljava/util/function/Consumer;)V", at = @At("HEAD"), cancellable = true)
    private void memento$preventDamageIfMastered(int damage, ServerLevel level, @Nullable LivingEntity entity, Consumer<Object> onBreak, CallbackInfo ci) {
        ItemStack stack = (ItemStack) (Object) this;

        for (StatMastery rule : StatMasteryManager.getAllRules()) {
            if (rule.preventDamage()) {
                long val = MementoAPI.getStat(stack, rule.stat());
                if (val >= rule.value()) {
                    // Item is Mastered! Prevent damage by cancelling the method call.
                    ci.cancel();
                    return;
                }
            }
        }
    }
}