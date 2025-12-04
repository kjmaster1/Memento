package com.kjmaster.memento.mixin;

import com.kjmaster.memento.api.MementoAPI;
import com.kjmaster.memento.data.StatMastery;
import com.kjmaster.memento.data.StatMasteryManager;
import com.kjmaster.memento.data.StatVisualPrestige;
import com.kjmaster.memento.data.StatVisualPrestigeManager;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(value = ItemStack.class, priority = 2000)
public abstract class ItemStackMixin {

    @Inject(method = "getRarity", at = @At("RETURN"), cancellable = true)
    private void memento$modifyRarity(CallbackInfoReturnable<Rarity> cir) {
        ItemStack stack = (ItemStack) (Object) this;

        // Don't run logic on empty stacks
        if (stack.isEmpty()) return;

        Rarity currentRarity = cir.getReturnValue();
        Rarity bestRarity = currentRarity;

        // Optimization: Use getRules(stack) to avoid iterating irrelevant rules
        List<StatVisualPrestige> rules = StatVisualPrestigeManager.getRules(stack);

        for (StatVisualPrestige rule : rules) {
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

        List<StatVisualPrestige> rules = StatVisualPrestigeManager.getRules(stack);

        for (StatVisualPrestige rule : rules) {
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

    // SAFER: Instead of cancelling execution, we set the damage amount to 0.
    // This allows other mods to still hook 'hurtAndBreak' if they need to triggers side effects.
    @ModifyVariable(
            method = "hurtAndBreak(ILnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/entity/LivingEntity;Ljava/util/function/Consumer;)V",
            at = @At("HEAD"),
            argsOnly = true
    )
    private int memento$preventDamageIfMastered(int amount) {
        if (amount <= 0) return amount;

        ItemStack stack = (ItemStack) (Object) this;
        for (StatMastery rule : StatMasteryManager.getAllRules()) {
            if (rule.preventDamage()) {
                long val = MementoAPI.getStat(stack, rule.stat());
                if (val >= rule.value()) {
                    return 0; // Reduce damage to zero
                }
            }
        }
        return amount;
    }
}