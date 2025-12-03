package com.kjmaster.memento.component;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

public record ItemMetadata(String creatorName, long createdOnWorldDay) {
    public static final ItemMetadata EMPTY = new ItemMetadata("", -1);

    public static final Codec<ItemMetadata> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("creator").forGetter(ItemMetadata::creatorName),
            Codec.LONG.fieldOf("date").forGetter(ItemMetadata::createdOnWorldDay)
    ).apply(instance, ItemMetadata::new));

    public static final StreamCodec<ByteBuf, ItemMetadata> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, ItemMetadata::creatorName,
            ByteBufCodecs.VAR_LONG, ItemMetadata::createdOnWorldDay,
            ItemMetadata::new
    );
}