package com.kjmaster.memento.item;

import com.kjmaster.memento.component.TrackerMap;
import com.kjmaster.memento.registry.ModDataComponents;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;

public class MementoCrystalItem extends Item {
    public MementoCrystalItem(Properties properties) {
        super(properties);
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        // Glint if it contains data
        return stack.has(ModDataComponents.TRACKER_MAP) && !stack.get(ModDataComponents.TRACKER_MAP).stats().isEmpty();
    }

    @Override
    public void appendHoverText(@NotNull ItemStack stack, @NotNull TooltipContext context, @NotNull List<Component> tooltipComponents, @NotNull TooltipFlag tooltipFlag) {
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);

        if (stack.has(ModDataComponents.TRACKER_MAP)) {
            TrackerMap map = stack.get(ModDataComponents.TRACKER_MAP);
            if (map.stats().isEmpty()) {
                tooltipComponents.add(Component.translatable("item.memento.memento_crystal.empty").withStyle(ChatFormatting.GRAY));
            } else {
                tooltipComponents.add(Component.translatable("item.memento.memento_crystal.stored").withStyle(ChatFormatting.GOLD));
                // Only show a summary count to keep tooltip clean, detailed view is via Shift on the tool itself usually.
                // Or we can list a few.
                int count = 0;
                for (Map.Entry<ResourceLocation, Long> entry : map.stats().entrySet()) {
                    if (count++ > 4) {
                        tooltipComponents.add(Component.literal("...").withStyle(ChatFormatting.GRAY));
                        break;
                    }
                    String key = String.format("stat.%s.%s", entry.getKey().getNamespace(), entry.getKey().getPath());
                    tooltipComponents.add(Component.translatable(key).append(": " + entry.getValue()).withStyle(ChatFormatting.DARK_AQUA));
                }
            }
        } else {
            tooltipComponents.add(Component.translatable("item.memento.memento_crystal.empty").withStyle(ChatFormatting.GRAY));
        }
    }
}