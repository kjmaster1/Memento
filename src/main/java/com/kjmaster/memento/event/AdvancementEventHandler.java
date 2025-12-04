package com.kjmaster.memento.event;

import com.kjmaster.memento.api.event.StatChangeEvent;
import com.kjmaster.memento.registry.ModCriteria;
import net.neoforged.bus.api.SubscribeEvent;

public class AdvancementEventHandler {
    @SubscribeEvent
    public void onStatChange(StatChangeEvent.Post event) {
        ModCriteria.STAT_CHANGED.trigger(
                event.getPlayer(),
                event.getItem(),
                event.getStatId(),
                event.getNewValue()
        );
    }
}