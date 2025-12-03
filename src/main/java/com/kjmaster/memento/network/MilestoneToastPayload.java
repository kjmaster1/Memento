package com.kjmaster.memento.network;

import com.kjmaster.memento.Memento;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

public record MilestoneToastPayload(ItemStack item, Component title, Component description) implements CustomPacketPayload {

    public static final Type<MilestoneToastPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Memento.MODID, "milestone_toast"));

    // Define how to encode/decode this packet over the network
    public static final StreamCodec<RegistryFriendlyByteBuf, MilestoneToastPayload> STREAM_CODEC = StreamCodec.composite(
            ItemStack.STREAM_CODEC, MilestoneToastPayload::item,
            ComponentSerialization.STREAM_CODEC, MilestoneToastPayload::title,
            ComponentSerialization.STREAM_CODEC, MilestoneToastPayload::description,
            MilestoneToastPayload::new
    );

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}