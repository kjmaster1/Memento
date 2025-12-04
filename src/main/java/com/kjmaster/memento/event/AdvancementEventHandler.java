package com.kjmaster.memento.event;

import com.kjmaster.memento.api.event.StatChangeEvent;
import com.kjmaster.memento.registry.ModCriteria;
import net.neoforged.bus.api.SubscribeEvent;

public class AdvancementEventHandler {
    @SubscribeEvent
    public static void onStatChange(StatChangeEvent.Post event) {
        if (event.getPlayer() != null) {
            ModCriteria.STAT_CHANGED.trigger(
                    event.getPlayer(),
                    event.getItem(),
                    event.getStatId(),
                    event.getNewValue()
            );
        }
    }
}