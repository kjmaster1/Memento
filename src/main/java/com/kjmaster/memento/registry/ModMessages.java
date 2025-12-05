package com.kjmaster.memento.registry;

import com.kjmaster.memento.Memento;
import com.kjmaster.memento.network.ClientPayloadHandler;
import com.kjmaster.memento.network.MilestoneToastPayload;
import com.kjmaster.memento.network.StatRegistryPayload;
import com.kjmaster.memento.network.StatUpdatePayload;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

@EventBusSubscriber(modid = Memento.MODID)
public class ModMessages {

    @SubscribeEvent
    public static void register(RegisterPayloadHandlersEvent event) {
        final PayloadRegistrar registrar = event.registrar("1");

        // Register the Client-bound packet
        registrar.playToClient(
                MilestoneToastPayload.TYPE,
                MilestoneToastPayload.STREAM_CODEC,
                ClientPayloadHandler::handleData
        );

        registrar.playToClient(
                StatUpdatePayload.TYPE,
                StatUpdatePayload.STREAM_CODEC,
                ClientPayloadHandler::handleStatUpdate
        );

        registrar.playToClient(
                StatRegistryPayload.TYPE,
                StatRegistryPayload.STREAM_CODEC,
                StatRegistryPayload::handle
        );
    }
}