package com.kjmaster.memento.mixin;

import com.kjmaster.memento.registry.ModDataComponents;
import net.minecraft.world.inventory.ItemCombinerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.SmithingMenu;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SmithingMenu.class)
public abstract class SmithingMenuMixin extends ItemCombinerMenu {

    public SmithingMenuMixin(MenuType<?> type, int containerId, net.minecraft.world.entity.player.Inventory playerInventory, net.minecraft.world.inventory.ContainerLevelAccess access) {
        super(type, containerId, playerInventory, access);
    }

    @Inject(method = "createResult", at = @At("RETURN"))
    private void memento$preserveStatsOnUpgrade(CallbackInfo ci) {
        // In the Smithing Table:
        // inputSlots(0) = Template (e.g. Upgrade Template)
        // inputSlots(1) = Base Item (e.g. Diamond Sword)
        // inputSlots(2) = Addition (e.g. Netherite Ingot)
        // resultSlots(0) = Result (e.g. Netherite Sword)

        ItemStack base = this.inputSlots.getItem(1);
        ItemStack result = this.resultSlots.getItem(0);

        if (base.isEmpty() || result.isEmpty()) return;

        // 1. Copy Stats (TrackerMap)
        if (base.has(ModDataComponents.TRACKER_MAP)) {
            result.set(ModDataComponents.TRACKER_MAP, base.get(ModDataComponents.TRACKER_MAP));
        }

        // 2. Copy Identity & History (UUID + Metadata)
        if (base.has(ModDataComponents.ITEM_UUID)) {
            result.set(ModDataComponents.ITEM_UUID, base.get(ModDataComponents.ITEM_UUID));
        }
        if (base.has(ModDataComponents.ITEM_METADATA)) {
            result.set(ModDataComponents.ITEM_METADATA, base.get(ModDataComponents.ITEM_METADATA));
        }

        // 3. Copy Progression (Milestones, Synergies)
        if (base.has(ModDataComponents.MILESTONES)) {
            result.set(ModDataComponents.MILESTONES, base.get(ModDataComponents.MILESTONES));
        }
        if (base.has(ModDataComponents.UNLOCKED_SYNERGIES)) {
            result.set(ModDataComponents.UNLOCKED_SYNERGIES, base.get(ModDataComponents.UNLOCKED_SYNERGIES));
        }

        // 4. Copy Runtime Data (Echo Cooldowns)
        if (base.has(ModDataComponents.ECHO_COOLDOWNS)) {
            result.set(ModDataComponents.ECHO_COOLDOWNS, base.get(ModDataComponents.ECHO_COOLDOWNS));
        }
    }
}