package com.kjmaster.memento.registry;

import com.kjmaster.memento.Memento;
import com.kjmaster.memento.item.MementoCrystalItem;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Memento.MODID);

    public static final DeferredHolder<Item, MementoCrystalItem> MEMENTO_CRYSTAL = ITEMS.register("memento_crystal",
            () -> new MementoCrystalItem(new Item.Properties().stacksTo(1).fireResistant()));
}