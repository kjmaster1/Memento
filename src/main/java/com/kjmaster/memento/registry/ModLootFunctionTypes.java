package com.kjmaster.memento.registry;

import com.kjmaster.memento.Memento;
import com.kjmaster.memento.loot.AddStatFunction;
import com.kjmaster.memento.loot.SetStatFunction;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.storage.loot.functions.LootItemFunctionType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModLootFunctionTypes {
    public static final DeferredRegister<LootItemFunctionType<?>> LOOT_FUNCTION_TYPES = DeferredRegister.create(Registries.LOOT_FUNCTION_TYPE, Memento.MODID);

    public static final DeferredHolder<LootItemFunctionType<?>, LootItemFunctionType<SetStatFunction>> SET_STAT = LOOT_FUNCTION_TYPES.register(
            "set_stat",
            () -> new LootItemFunctionType<>(SetStatFunction.CODEC)
    );

    public static final DeferredHolder<LootItemFunctionType<?>, LootItemFunctionType<AddStatFunction>> ADD_STAT = LOOT_FUNCTION_TYPES.register(
            "add_stat",
            () -> new LootItemFunctionType<>(AddStatFunction.CODEC)
    );
}