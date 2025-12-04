package com.kjmaster.memento.registry;

import com.kjmaster.memento.Memento;
import com.kjmaster.memento.loot.StatLootCondition;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.storage.loot.predicates.LootItemConditionType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModLootConditionTypes {
    public static final DeferredRegister<LootItemConditionType> LOOT_CONDITION_TYPES = DeferredRegister.create(Registries.LOOT_CONDITION_TYPE, Memento.MODID);

    public static final DeferredHolder<LootItemConditionType, LootItemConditionType> STAT_CHECK = LOOT_CONDITION_TYPES.register(
            "stat_check",
            () -> new LootItemConditionType(StatLootCondition.CODEC)
    );
}