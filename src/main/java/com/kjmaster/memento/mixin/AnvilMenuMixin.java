package com.kjmaster.memento.mixin;

import com.kjmaster.memento.api.MementoAPI;
import com.kjmaster.memento.component.TrackerMap;
import com.kjmaster.memento.data.StatBehavior;
import com.kjmaster.memento.data.StatBehaviorManager;
import com.kjmaster.memento.data.StatRepairCap;
import com.kjmaster.memento.data.StatRepairCapManager;
import com.kjmaster.memento.registry.ModDataComponents;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.ItemCombinerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;

@Mixin(AnvilMenu.class)
public abstract class AnvilMenuMixin extends ItemCombinerMenu {

    @Shadow @Final private DataSlot cost;

    public AnvilMenuMixin(MenuType<?> type, int containerId, net.minecraft.world.entity.player.Inventory playerInventory, net.minecraft.world.inventory.ContainerLevelAccess access) {
        super(type, containerId, playerInventory, access);
    }

    @Inject(method = "createResult", at = @At("RETURN"))
    private void memento$anvilLogic(CallbackInfo ci) {
        ItemStack left = this.inputSlots.getItem(0);
        ItemStack right = this.inputSlots.getItem(1);
        ItemStack result = this.resultSlots.getItem(0);

        if (left.isEmpty() || result.isEmpty()) return;

        // --- 1. STAT MERGING LOGIC ---
        // If both items have Memento stats, we merge them onto the result.
        if (left.has(ModDataComponents.TRACKER_MAP) && right.has(ModDataComponents.TRACKER_MAP)) {
            TrackerMap leftMap = left.get(ModDataComponents.TRACKER_MAP);
            TrackerMap rightMap = right.get(ModDataComponents.TRACKER_MAP);

            // Start with Left's stats (which Vanilla usually copies to Result already)
            // We use the Result's map as the base to ensure we are modifying the output
            TrackerMap resultMap = result.getOrDefault(ModDataComponents.TRACKER_MAP, leftMap);

            // Merge Right's stats into Result
            if (rightMap != null && !rightMap.stats().isEmpty()) {
                for (Map.Entry<ResourceLocation, Long> entry : rightMap.stats().entrySet()) {
                    ResourceLocation stat = entry.getKey();
                    long rightValue = entry.getValue();
                    long currentResultValue = resultMap.getValue(stat);

                    // Determine Merge Strategy from JSON (Default: SUM)
                    StatBehavior.MergeStrategy strategy = StatBehaviorManager.getStrategy(stat);
                    long newValue = switch (strategy) {
                        case MAX -> Math.max(currentResultValue, rightValue);
                        case MIN -> (currentResultValue == 0) ? rightValue : Math.min(currentResultValue, rightValue);
                        case SUM -> currentResultValue + rightValue;
                    };

                    // Only update if changed (optimization)
                    if (newValue != currentResultValue) {
                        final long finalVal = newValue;
                        resultMap = resultMap.update(stat, newValue, (old, n) -> finalVal);
                    }
                }
                result.set(ModDataComponents.TRACKER_MAP, resultMap);
            }
        }

        // --- 2. REPAIR COST CAP LOGIC
        int lowestCap = -1;
        for (StatRepairCap rule : StatRepairCapManager.getAllRules()) {
            long val = MementoAPI.getStat(left, rule.stat());
            if (val >= rule.minInfo()) {
                if (lowestCap == -1 || rule.cap() < lowestCap) {
                    lowestCap = rule.cap();
                }
            }
        }

        if (lowestCap != -1) {
            int currentCost = this.cost.get();
            if (currentCost >= 40) {
                this.cost.set(39);
            }
            int futureCost = result.getOrDefault(DataComponents.REPAIR_COST, 0);
            if (futureCost > lowestCap) {
                result.set(DataComponents.REPAIR_COST, lowestCap);
            }
        }
    }
}