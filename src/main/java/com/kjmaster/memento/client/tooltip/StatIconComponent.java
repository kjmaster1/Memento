package com.kjmaster.memento.client.tooltip;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

public record StatIconComponent(ItemStack icon, String text, int color) implements ClientTooltipComponent, TooltipComponent {

    @Override
    public int getHeight() {
        return 19; // Item (16) + Padding (3)
    }

    @Override
    public int getWidth(@NotNull Font font) {
        return 20 + font.width(text); // Icon space (20) + Text width
    }

    @Override
    public void renderImage(@NotNull Font font, int x, int y, GuiGraphics guiGraphics) {
        // Draw Item Icon
        guiGraphics.renderFakeItem(icon, x, y);

        // Draw Value Text
        // Center text vertically relative to the 16px icon
        guiGraphics.drawString(font, text, x + 20, y + 4, color, true);
    }
}