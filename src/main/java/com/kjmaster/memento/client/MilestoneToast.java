package com.kjmaster.memento.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.toasts.Toast;
import net.minecraft.client.gui.components.toasts.ToastComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

public class MilestoneToast implements Toast {
    // Standard vanilla toast texture
    private static final ResourceLocation TEXTURE = ResourceLocation.withDefaultNamespace("toast/advancement");

    private final ItemStack icon;
    private final Component title;
    private final Component description;

    private static final int TITLE_COLOR = 0xFFFFFF00; // Yellow
    private static final int DESC_COLOR = 0xFFFFFFFF; // White

    public MilestoneToast(ItemStack icon, Component title, Component description) {
        this.icon = icon;
        this.title = title;
        this.description = description;
    }

    @Override
    public @NotNull Visibility render(GuiGraphics guiGraphics, ToastComponent toastComponent, long timeSinceLastVisible) {
        // Draw the background
        guiGraphics.blitSprite(TEXTURE, 0, 0, 160, 32);

        // Draw the Title (Yellow)
        guiGraphics.drawString(toastComponent.getMinecraft().font, this.title, 30, 7, TITLE_COLOR, false);

        // Draw the Description (White)
        guiGraphics.drawString(toastComponent.getMinecraft().font, this.description, 30, 18, DESC_COLOR, false);

        // Draw the Item Icon
        guiGraphics.renderFakeItem(this.icon, 8, 8);

        // Show for 5 seconds (5000ms), then hide
        return timeSinceLastVisible >= 5000L ? Visibility.HIDE : Visibility.SHOW;
    }
}