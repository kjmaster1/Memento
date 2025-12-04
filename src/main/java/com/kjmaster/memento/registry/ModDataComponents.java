package com.kjmaster.memento.registry;

import com.kjmaster.memento.Memento;
import com.kjmaster.memento.component.*;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.UUID;
import java.util.function.UnaryOperator;

public class ModDataComponents {
    public static final DeferredRegister<DataComponentType<?>> COMPONENTS = DeferredRegister.create(Registries.DATA_COMPONENT_TYPE, Memento.MODID);

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<TrackerMap>> TRACKER_MAP = register("tracker_map", builder -> builder
            .persistent(TrackerMap.CODEC)
            .networkSynchronized(TrackerMap.STREAM_CODEC)
            .cacheEncoding() // Optimization: caches the serialized form since these might be synced often
    );

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<ItemMetadata>> ITEM_METADATA = register("item_metadata", builder -> builder
            .persistent(ItemMetadata.CODEC)
            .networkSynchronized(ItemMetadata.STREAM_CODEC)
            .cacheEncoding()
    );

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<UnlockedMilestones>> MILESTONES = register("milestones", builder -> builder
            .persistent(UnlockedMilestones.CODEC)
            .networkSynchronized(UnlockedMilestones.STREAM_CODEC)
    );

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<UUID>> ITEM_UUID = register("uuid", builder -> builder
            .persistent(UUIDUtil.CODEC)
            .networkSynchronized(UUIDUtil.STREAM_CODEC)
    );

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<EchoCooldowns>> ECHO_COOLDOWNS = register("echo_cooldowns", builder -> builder
            .persistent(EchoCooldowns.CODEC)
            .networkSynchronized(EchoCooldowns.STREAM_CODEC)
    );

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<UnlockedSynergies>> UNLOCKED_SYNERGIES = register("unlocked_synergies", builder -> builder
            .persistent(UnlockedSynergies.CODEC)
            .networkSynchronized(UnlockedSynergies.STREAM_CODEC)
    );

    private static <T> DeferredHolder<DataComponentType<?>, DataComponentType<T>> register(String name, UnaryOperator<DataComponentType.Builder<T>> builderOp) {
        return COMPONENTS.register(name, () -> builderOp.apply(DataComponentType.builder()).build());
    }
}
