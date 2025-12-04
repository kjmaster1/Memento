package com.kjmaster.memento.client.tooltip;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import org.jetbrains.annotations.NotNull;

public record StatBarComponent(float progress, int color) implements ClientTooltipComponent, TooltipComponent {

    @Override
    public int getHeight() {
        return 10; // Height of the bar area + padding
    }

    @Override
    public int getWidth(@NotNull Font font) {
        return 102; // Width of the bar + border
    }

    @Override
    public void renderImage(@NotNull Font font, int x, int y, GuiGraphics guiGraphics) {
        // Draw Background (Dark Gray)
        guiGraphics.fill(x, y + 2, x + 100, y + 8, 0xFF555555);

        // Draw Foreground (Colored)
        // Ensure progress is clamped 0.0 - 1.0
        float clampedProgress = Math.max(0.0f, Math.min(progress, 1.0f));
        int barWidth = (int) (100 * clampedProgress);

        // OR 0xFF000000 ensures 100% Alpha so it isn't transparent
        guiGraphics.fill(x, y + 2, x + barWidth, y + 8, color | 0xFF000000);

        // Draw Border (White)
        guiGraphics.renderOutline(x - 1, y + 1, 102, 8, 0xFFFFFFFF);
    }
}