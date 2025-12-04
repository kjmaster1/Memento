package com.kjmaster.memento.compat.jei.category;

import com.kjmaster.memento.Memento;
import com.kjmaster.memento.data.StatEffect;
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
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import org.jetbrains.annotations.NotNull;

public class StatEffectCategory implements IRecipeCategory<StatEffect> {
    public static final RecipeType<StatEffect> TYPE = RecipeType.create(Memento.MODID, "stat_effects", StatEffect.class);
    private final IDrawable background;
    private final IDrawable icon;

    public StatEffectCategory(IGuiHelper helper) {
        // this.background = helper.createDrawable(Memento.loc("textures/gui/jei_effect.png"), 0, 0, 160, 40);
        this.background = helper.createBlankDrawable(160, 40);
        this.icon = helper.createDrawableIngredient(VanillaTypes.ITEM_STACK, new ItemStack(Items.POTION));
    }

    @Override
    public @NotNull RecipeType<StatEffect> getRecipeType() {
        return TYPE;
    }

    @Override
    public @NotNull Component getTitle() {
        return Component.translatable("jei.memento.effect.title");
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
    public void setRecipe(IRecipeLayoutBuilder builder, @NotNull StatEffect recipe, @NotNull IFocusGroup focuses) {
        // Input: Context
        builder.addSlot(RecipeIngredientRole.INPUT, 10, 11)
                .addItemStack(new ItemStack(ModItems.MEMENTO_CRYSTAL.get()))
                .addTooltipCallback((view, tooltip) -> {
                    tooltip.clear();
                    tooltip.add(Component.translatable("stat." + recipe.stat().getNamespace() + "." + recipe.stat().getPath()).withStyle(ChatFormatting.GOLD));
                    tooltip.add(Component.literal("Context: " + recipe.context().name()).withStyle(ChatFormatting.GRAY));
                    tooltip.add(Component.literal("Min Value: " + recipe.minInfo()).withStyle(ChatFormatting.GRAY));
                });

        // Output: Potion Effect Representation
        ItemStack potion = PotionContents.createItemStack(Items.POTION, Potions.WATER);
        // We can't easily dye the potion here without more boilerplate, but the tooltip is key.

        builder.addSlot(RecipeIngredientRole.OUTPUT, 120, 11)
                .addItemStack(potion)
                .addTooltipCallback((view, tooltip) -> {
                    tooltip.clear();
                    tooltip.add(recipe.effect().getDisplayName().copy().withStyle(ChatFormatting.GREEN));
                    tooltip.add(Component.literal("Amplifier: " + recipe.amplifier()));
                    tooltip.add(Component.literal("Duration: " + (recipe.durationTicks() / 20) + "s"));
                    tooltip.add(Component.literal("Chance: " + (recipe.chance() * 100) + "%").withStyle(ChatFormatting.YELLOW));
                });
    }

    @Override
    public void draw(StatEffect recipe, @NotNull IRecipeSlotsView recipeSlotsView, GuiGraphics guiGraphics, double mouseX, double mouseY) {
        Minecraft mc = Minecraft.getInstance();

        // Draw Context
        guiGraphics.drawString(mc.font, "When: " + recipe.context().name(), 40, 5, 0xFFFFFFFF, false);

        // Draw Target
        guiGraphics.drawString(mc.font, "To: " + recipe.target().name(), 40, 15, 0xFFCCCCCC, false);
    }
}