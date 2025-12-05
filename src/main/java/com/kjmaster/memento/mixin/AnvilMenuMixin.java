package com.kjmaster.memento.mixin;

import com.kjmaster.memento.api.MementoAPI;
import com.kjmaster.memento.component.TrackerMap;
import com.kjmaster.memento.data.StatRepairCap;
import com.kjmaster.memento.data.StatRepairCapManager;
import com.kjmaster.memento.registry.ModDataComponents;
import net.minecraft.core.component.DataComponents;
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

@Mixin(value = AnvilMenu.class, priority = 1001)
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

        if (right.has(ModDataComponents.TRACKER_MAP)) {
            TrackerMap rightMap = right.get(ModDataComponents.TRACKER_MAP);
            if (rightMap != null && !rightMap.stats().isEmpty()) {
                result.update(ModDataComponents.TRACKER_MAP, TrackerMap.EMPTY, currentMap ->
                        MementoAPI.mergeStats(currentMap, rightMap)
                );
            }
        }

        int lowestCap = -1;
        // Use optimized lookup
        for (StatRepairCap rule : StatRepairCapManager.getRules(left)) {
            long val = MementoAPI.getStat(left, rule.stat());
            if (val >= rule.minInfo()) {
                if (lowestCap == -1 || rule.cap() < lowestCap) {
                    lowestCap = rule.cap();
                }
            }
        }

        if (lowestCap != -1) {
            int currentCost = this.cost.get();
            if (currentCost > lowestCap) {
                if (lowestCap < 39 || currentCost < 40) {
                    this.cost.set(lowestCap);
                    int itemRepairCost = result.getOrDefault(DataComponents.REPAIR_COST, 0);
                    if (itemRepairCost > lowestCap) {
                        result.set(DataComponents.REPAIR_COST, lowestCap);
                    }
                }
            }
        }
    }
}