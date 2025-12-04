package com.kjmaster.memento.registry;

import com.kjmaster.memento.Memento;
import com.kjmaster.memento.crafting.PreserveStatsRecipe;
import com.kjmaster.memento.crafting.StatTransferRecipe;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModRecipes {
    public static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS = DeferredRegister.create(Registries.RECIPE_SERIALIZER, Memento.MODID);

    public static final DeferredHolder<RecipeSerializer<?>, PreserveStatsRecipe.Serializer> PRESERVE_STATS = SERIALIZERS.register(
            "preserve_stats",
            PreserveStatsRecipe.Serializer::new
    );

    public static final DeferredHolder<RecipeSerializer<?>, StatTransferRecipe.Serializer> STAT_TRANSFER = SERIALIZERS.register(
            "stat_transfer",
            StatTransferRecipe.Serializer::new
    );
}