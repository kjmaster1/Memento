package com.kjmaster.memento.registry;

import com.kjmaster.memento.Memento;
import com.mojang.serialization.Codec;
import net.minecraft.core.UUIDUtil;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ModDataAttachments {
    public static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES = DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, Memento.MODID);

    // Stores the position (Vec3) from where a projectile was fired
    public static final DeferredHolder<AttachmentType<?>, AttachmentType<Vec3>> PROJECTILE_ORIGIN = ATTACHMENT_TYPES.register(
            "projectile_origin",
            () -> AttachmentType.builder(() -> Vec3.ZERO).serialize(Vec3.CODEC).build()
    );

    // Stores the last known value of the "Aviate One cm" statistic for a player
    // Default value is 0. We use Codec.INT for serialization.
    public static final DeferredHolder<AttachmentType<?>, AttachmentType<Integer>> LAST_AVIATE_VALUE = ATTACHMENT_TYPES.register(
            "last_aviate_value",
            () -> AttachmentType.builder(() -> 0).serialize(Codec.INT).build()
    );

    // Stores the weapon ItemStack that fired a projectile.
    // We use ItemStack.EMPTY as the default.
    public static final DeferredHolder<AttachmentType<?>, AttachmentType<ItemStack>> SOURCE_STACK = ATTACHMENT_TYPES.register(
            "source_stack",
            () -> AttachmentType.builder(() -> ItemStack.EMPTY).serialize(ItemStack.CODEC).build()
    );

    // Prevents double-application of stats on chunk load
    public static final DeferredHolder<AttachmentType<?>, AttachmentType<Boolean>> BALLISTICS_APPLIED = ATTACHMENT_TYPES.register(
            "ballistics_applied",
            () -> AttachmentType.builder(() -> false).serialize(Codec.BOOL).build()
    );

    public static final DeferredHolder<AttachmentType<?>, AttachmentType<Map<UUID, Map<ResourceLocation, Long>>>> PENDING_STATS = ATTACHMENT_TYPES.register(
            "pending_stats",
            () -> AttachmentType.builder(() -> (Map<UUID, Map<ResourceLocation, Long>>) new HashMap<UUID, Map<ResourceLocation, Long>>())
                    .serialize(Codec.unboundedMap(UUIDUtil.CODEC, Codec.unboundedMap(ResourceLocation.CODEC, Codec.LONG)))
                    .build()
    );
}