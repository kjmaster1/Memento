package com.kjmaster.memento.component;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

import java.util.List;

public record ItemMetadata(String creatorName, long createdOnWorldDay, String originalName, List<OwnerEntry> wieldedBy) {
    public static final ItemMetadata EMPTY = new ItemMetadata("", -1, "", List.of());

    public static final Codec<ItemMetadata> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("creator").forGetter(ItemMetadata::creatorName),
            Codec.LONG.fieldOf("date").forGetter(ItemMetadata::createdOnWorldDay),
            Codec.STRING.optionalFieldOf("original_name", "").forGetter(ItemMetadata::originalName),
            OwnerEntry.CODEC.listOf().optionalFieldOf("wielded_by", List.of()).forGetter(ItemMetadata::wieldedBy)
    ).apply(instance, ItemMetadata::new));

    public static final StreamCodec<ByteBuf, ItemMetadata> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, ItemMetadata::creatorName,
            ByteBufCodecs.VAR_LONG, ItemMetadata::createdOnWorldDay,
            ByteBufCodecs.STRING_UTF8, ItemMetadata::originalName,
            OwnerEntry.STREAM_CODEC.apply(ByteBufCodecs.list()), ItemMetadata::wieldedBy,
            ItemMetadata::new
    );

    /**
     * Represents an entry for a previous owner of the item.
     * Stored in the ItemMetadata's 'wieldedBy' list.
     */
    public record OwnerEntry(String ownerName, long dayWielded) {
        public static final Codec<OwnerEntry> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.STRING.fieldOf("name").forGetter(OwnerEntry::ownerName),
                Codec.LONG.fieldOf("day").forGetter(OwnerEntry::dayWielded)
        ).apply(instance, OwnerEntry::new));

        public static final StreamCodec<ByteBuf, OwnerEntry> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.STRING_UTF8, OwnerEntry::ownerName,
                ByteBufCodecs.VAR_LONG, OwnerEntry::dayWielded,
                OwnerEntry::new
        );
    }
}