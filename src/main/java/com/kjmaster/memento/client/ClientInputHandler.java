package com.kjmaster.memento.client;

import com.kjmaster.memento.Memento;
import com.kjmaster.memento.event.MementoClientEvents;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.InputEvent;

@EventBusSubscriber(modid = Memento.MODID, value = Dist.CLIENT)
public class ClientInputHandler {
    // Global state for tooltip rendering mode
    public static boolean isCompactMode = false;

    @SubscribeEvent
    public static void onKeyInput(InputEvent.Key event) {
        if (ModKeyMappings.TOGGLE_COMPACT_MODE.consumeClick()) {
            isCompactMode = !isCompactMode;
            // Clear the tooltip cache to force immediate re-render with the new mode
            MementoClientEvents.clearCache();
        }
    }
}