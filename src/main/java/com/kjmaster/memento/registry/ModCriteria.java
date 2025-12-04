package com.kjmaster.memento.registry;

import com.kjmaster.memento.Memento;
import com.kjmaster.memento.advancement.StatChangedTrigger;
import net.minecraft.core.registries.BuiltInRegistries;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.registries.RegisterEvent;

import static com.kjmaster.memento.Memento.loc;

@EventBusSubscriber(modid = Memento.MODID)
public class ModCriteria {
    public static final StatChangedTrigger STAT_CHANGED = new StatChangedTrigger();

    @SubscribeEvent
    public static void register(RegisterEvent event) {
        event.register(BuiltInRegistries.TRIGGER_TYPES.key(), helper -> {
            helper.register(loc("stat_changed"), STAT_CHANGED);
        });
    }
}