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

@Mixin(value = AnvilMenu.class, priority = 1001)
public abstract class AnvilMenuMixin extends ItemCombinerMenu {

    @Shadow
    @Final
    private DataSlot cost;

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
        // Use stack.update() for atomic, safe component modification.
        // This ensures we respect any existing data on the result stack (which Vanilla copies from 'left').
        if (right.has(ModDataComponents.TRACKER_MAP)) {
            TrackerMap rightMap = right.get(ModDataComponents.TRACKER_MAP);

            if (rightMap != null && !rightMap.stats().isEmpty()) {
                result.update(ModDataComponents.TRACKER_MAP, TrackerMap.EMPTY, currentMap -> {
                    // Start with the current map (from Left item) and merge Right item's stats into it
                    TrackerMap merged = currentMap;
                    for (Map.Entry<ResourceLocation, Long> entry : rightMap.stats().entrySet()) {
                        ResourceLocation stat = entry.getKey();
                        long rightValue = entry.getValue();

                        // Determine Merge Strategy from JSON (Default: SUM)
                        StatBehavior.MergeStrategy strategy = StatBehaviorManager.getStrategy(stat);

                        merged = merged.update(stat, rightValue, (oldVal, newVal) -> switch (strategy) {
                            case MAX -> Math.max(oldVal, newVal);
                            case MIN ->
                                    (oldVal == 0) ? newVal : Math.min(oldVal, newVal); // Treat 0 as "uninitialized" for MIN
                            case SUM -> oldVal + newVal;
                        });
                    }
                    return merged;
                });
            }
        }

        // --- 2. REPAIR COST CAP LOGIC ---
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

            // Safety Logic:
            // 1. Only act if the current cost effectively exceeds our cap.
            // 2. Compatibility: If the cost is >= 40 (Too Expensive allowed by another mod or Creative mode),
            //    we ONLY override it if our cap is explicitly a "Discount" (e.g. < 39).
            //    This prevents us from resetting a "Hardcore Repair" mod's intentionally high cost (e.g. 50)
            //    back to the vanilla limit (39) just because a generic cap rule exists.
            if (currentCost > lowestCap) {
                if (lowestCap < 39 || currentCost < 40) {
                    this.cost.set(lowestCap);

                    // Update the item's internal repair cost component to ensure the penalty
                    // doesn't immediately scale back up on the next repair.
                    int itemRepairCost = result.getOrDefault(DataComponents.REPAIR_COST, 0);
                    if (itemRepairCost > lowestCap) {
                        result.set(DataComponents.REPAIR_COST, lowestCap);
                    }
                }
            }
        }
    }
}