package com.kjmaster.memento.compat.jei.category;

import com.kjmaster.memento.Memento;
import com.kjmaster.memento.data.StatMilestone;
import com.kjmaster.memento.registry.ModItems;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.NotNull;

public class MilestoneCategory implements IRecipeCategory<StatMilestone> {
    public static final RecipeType<StatMilestone> TYPE = RecipeType.create(Memento.MODID, "milestones", StatMilestone.class);
    private final IDrawable background;
    private final IDrawable icon;

    public MilestoneCategory(IGuiHelper helper) {
        // this.background = helper.createDrawable(Memento.loc("textures/gui/jei_milestone.png"), 0, 0, 160, 40);
        // Fallback to plain background if texture doesn't exist yet, for dev purposes:
        this.background = helper.createBlankDrawable(160, 40);
        this.icon = helper.createDrawableIngredient(VanillaTypes.ITEM_STACK, new ItemStack(ModItems.MEMENTO_CRYSTAL.get()));
    }

    @Override
    public @NotNull RecipeType<StatMilestone> getRecipeType() {
        return TYPE;
    }

    @Override
    public @NotNull Component getTitle() {
        return Component.translatable("jei.memento.milestone.title");
    }

    @Override
    public @NotNull IDrawable getBackground() {
        return background;
    }

    @Override
    public @NotNull IDrawable getIcon() {
        return icon;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, StatMilestone recipe, @NotNull IFocusGroup focuses) {
        // Input: The Stat Context (represented by the Crystal for now)
        builder.addSlot(RecipeIngredientRole.INPUT, 10, 11)
                .addItemStack(new ItemStack(ModItems.MEMENTO_CRYSTAL.get()))
                .addTooltipCallback((view, tooltip) -> {
                    tooltip.clear();
                    tooltip.add(Component.translatable("stat." + recipe.statId().getNamespace() + "." + recipe.statId().getPath())
                            .withStyle(ChatFormatting.GOLD));
                    tooltip.add(Component.literal("Requirement: " + recipe.targetValue()));
                });

        // Output: The Reward
        if (recipe.replacementItem().isPresent()) {
            builder.addSlot(RecipeIngredientRole.OUTPUT, 120, 11)
                    .addItemStack(recipe.replacementItem().get());
        } else {
            // If reward is a command/visual, show a generic "Reward" icon (e.g., Experience Bottle)
            builder.addSlot(RecipeIngredientRole.OUTPUT, 120, 11)
                    .addItemStack(new ItemStack(Items.EXPERIENCE_BOTTLE))
                    .addTooltipCallback((view, tooltip) -> {
                        tooltip.clear();
                        if (recipe.titleName().isPresent()) {
                            tooltip.add(Component.literal("Title: ").append(recipe.titleName().get()));
                        }
                        if (!recipe.rewards().isEmpty()) {
                            tooltip.add(Component.literal("Commands:").withStyle(ChatFormatting.YELLOW));
                            recipe.rewards().forEach(r -> tooltip.add(Component.literal("- " + r).withStyle(ChatFormatting.GRAY)));
                        }
                    });
        }
    }

    @Override
    public void draw(StatMilestone recipe, @NotNull IRecipeSlotsView recipeSlotsView, GuiGraphics guiGraphics, double mouseX, double mouseY) {
        Minecraft mc = Minecraft.getInstance();

        // Draw Stat Name
        String statName = Component.translatable("stat." + recipe.statId().getNamespace() + "." + recipe.statId().getPath()).getString();
        guiGraphics.drawString(mc.font, statName, 40, 5, 0xFFFFFFFF, false);

        // Draw Value Required
        guiGraphics.drawString(mc.font, "Req: " + recipe.targetValue(), 40, 15, 0xFFCCCCCC, false);
    }
}