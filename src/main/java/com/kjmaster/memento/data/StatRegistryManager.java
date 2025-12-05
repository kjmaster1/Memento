package com.kjmaster.memento.data;

import com.kjmaster.memento.Memento;
import com.kjmaster.memento.network.StatRegistryPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.*;

public class StatRegistryManager {

    private static final List<ResourceLocation> SORTED_STATS = new ArrayList<>();

    public static void reload() {

        Set<ResourceLocation> allStats = new HashSet<>(StatBehaviorManager.getKnownStats());

        // Sort for deterministic ID assignment
        SORTED_STATS.clear();
        SORTED_STATS.addAll(allStats);
        Collections.sort(SORTED_STATS);

        // Build Map
        Map<ResourceLocation, Integer> map = new HashMap<>();
        for (int i = 0; i < SORTED_STATS.size(); i++) {
            map.put(SORTED_STATS.get(i), i + 1); // Start IDs at 1, leaving 0 for "Literal/Unknown"
        }

        StatRegistry.setMapping(map);
    }

    /**
     * Called by the StatRegistryPayload handler to synchronize the client's network ID mapping.
     * This ensures the client has the correct ID-to-Stat mapping before processing any item stacks.
     */
    public static void syncClient(Map<ResourceLocation, Integer> mapping) {
        StatRegistry.setMapping(mapping);
        Memento.LOGGER.info("Memento Stat Registry Synced: {} unique stats loaded.", mapping.size());
    }

    @SubscribeEvent
    public static void onDatapackSync(OnDatapackSyncEvent event) {
        // Rebuild registry on reload (if not already done)
        if (event.getPlayer() == null) {
            reload();
        }

        StatRegistryPayload payload = new StatRegistryPayload(StatRegistry.getMap());

        if (event.getPlayer() != null) {
            PacketDistributor.sendToPlayer(event.getPlayer(), payload);
        } else {
            PacketDistributor.sendToAllPlayers(payload);
        }
    }

    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            PacketDistributor.sendToPlayer(player, new StatRegistryPayload(StatRegistry.getMap()));
        }
    }
}