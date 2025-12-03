package com.kjmaster.memento.network;

import com.kjmaster.memento.client.MilestoneToast;
import net.minecraft.client.Minecraft;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public class ClientPayloadHandler {
    public static void handleData(final MilestoneToastPayload payload, final IPayloadContext context) {
        // Enqueue work to the main client thread
        context.enqueueWork(() -> {
            Minecraft.getInstance().getToasts().addToast(
                    new MilestoneToast(payload.item(), payload.title(), payload.description())
            );
        });
    }
}