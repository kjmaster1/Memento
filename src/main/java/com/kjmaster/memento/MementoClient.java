package com.kjmaster.memento;

import com.kjmaster.memento.client.ModKeyMappings;
import com.kjmaster.memento.client.StatDefinitionManager;
import com.kjmaster.memento.client.tooltip.StatBarComponent;
import com.kjmaster.memento.client.tooltip.StatIconComponent;
import com.kjmaster.memento.component.TrackerMap;
import com.kjmaster.memento.registry.ModDataComponents;
import com.kjmaster.memento.registry.ModStats;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent;
import net.neoforged.neoforge.client.event.RegisterClientTooltipComponentFactoriesEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

@Mod(value = Memento.MODID, dist = Dist.CLIENT)
public class MementoClient {
    public MementoClient(IEventBus modEventBus, ModContainer container) {
        container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
        modEventBus.addListener(this::clientSetup);
        modEventBus.addListener(this::registerReloadListeners);
        modEventBus.addListener(this::registerTooltipFactories);
        modEventBus.addListener(this::registerKeyMappings);
    }

    private void registerReloadListeners(RegisterClientReloadListenersEvent event) {
        event.registerReloadListener(new StatDefinitionManager());
    }

    private void registerKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(ModKeyMappings.TOGGLE_COMPACT_MODE);
    }

    private void clientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            // Register Default Stats
            registerStatProperty(ModStats.BLOCKS_BROKEN);
            registerStatProperty(ModStats.ENTITIES_KILLED);
            registerStatProperty(ModStats.DISTANCE_FLOWN);
            registerStatProperty(ModStats.DAMAGE_TAKEN);
            registerStatProperty(ModStats.CROPS_HARVESTED);
            registerStatProperty(ModStats.DAMAGE_BLOCKED);
            registerStatProperty(ModStats.SHOTS_FIRED);
            registerStatProperty(ModStats.LONGEST_SHOT);
            registerStatProperty(ModStats.ITEMS_CAUGHT);
            registerStatProperty(ModStats.FIRES_STARTED);
            registerStatProperty(ModStats.MOBS_SHEARED);

            // Register Custom Configured Stats
            for (String statStr : Config.VISUAL_STATS.get()) {
                registerStatProperty(ResourceLocation.parse(statStr));
            }
        });
    }

    private void registerStatProperty(ResourceLocation statId) {
        ItemProperties.registerGeneric(statId, (stack, level, entity, seed) -> {
            if (!stack.has(ModDataComponents.TRACKER_MAP)) return 0.0f;

            TrackerMap map = stack.get(ModDataComponents.TRACKER_MAP);
            if (map == null) return 0.0f;
            return (float) map.getValue(statId);
        });
    }

    private void registerTooltipFactories(RegisterClientTooltipComponentFactoriesEvent event) {
        // Register Bar Component
        event.register(StatBarComponent.class, component -> component);
        // Register Icon Component
        event.register(StatIconComponent.class, component -> component);
    }
}
