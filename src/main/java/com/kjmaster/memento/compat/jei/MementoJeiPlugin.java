package com.kjmaster.memento.compat.jei;

import com.kjmaster.memento.Memento;
import com.kjmaster.memento.compat.jei.category.MilestoneCategory;
import com.kjmaster.memento.compat.jei.category.StatEffectCategory;
import com.kjmaster.memento.data.StatEffect;
import com.kjmaster.memento.data.StatEffectManager;
import com.kjmaster.memento.data.StatMilestone;
import com.kjmaster.memento.data.StatMilestoneManager;
import com.kjmaster.memento.registry.ModItems;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

@JeiPlugin
public class MementoJeiPlugin implements IModPlugin {
    public static final ResourceLocation UID = ResourceLocation.fromNamespaceAndPath(Memento.MODID, "jei_plugin");

    @Override
    public @NotNull ResourceLocation getPluginUid() {
        return UID;
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        registration.addRecipeCategories(new MilestoneCategory(registration.getJeiHelpers().getGuiHelper()));
        registration.addRecipeCategories(new StatEffectCategory(registration.getJeiHelpers().getGuiHelper()));
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        // Register Milestones
        List<StatMilestone> milestones = new ArrayList<>();
        StatMilestoneManager.getAllMilestones().values().forEach(milestones::addAll);
        registration.addRecipes(MilestoneCategory.TYPE, milestones);

        // Register Effects
        List<StatEffect> effects = StatEffectManager.getAllRules();
        registration.addRecipes(StatEffectCategory.TYPE, effects);

        // Add the Memento Crystal as a catalyst for these categories so users can find them easily
        registration.addRecipeCatalyst(new ItemStack(ModItems.MEMENTO_CRYSTAL.get()), MilestoneCategory.TYPE);
        registration.addRecipeCatalyst(new ItemStack(ModItems.MEMENTO_CRYSTAL.get()), StatEffectCategory.TYPE);
    }
}