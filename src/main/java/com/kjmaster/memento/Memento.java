package com.kjmaster.memento;

import com.kjmaster.memento.event.*;
import com.kjmaster.memento.milestone.MilestoneManager;
import com.kjmaster.memento.registry.ModDataComponents;
import com.mojang.logging.LogUtils;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import org.slf4j.Logger;

@Mod(Memento.MODID)
public class Memento {
    // Define mod id in a common place for everything to reference
    public static final String MODID = "memento";
    // Directly reference a slf4j logger
    public static final Logger LOGGER = LogUtils.getLogger();

    public Memento(IEventBus modEventBus, ModContainer modContainer) {
        modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC);

        // 1. Register Data Components (Mod Bus)
        ModDataComponents.COMPONENTS.register(modEventBus);

        // 2. Register Game Logic Events (Game Bus)
        // These are the "server-side" events like BlockBreak
        NeoForge.EVENT_BUS.register(MementoEvents.class);
        NeoForge.EVENT_BUS.register(MetadataEvents.class);
        NeoForge.EVENT_BUS.register(AviationEvents.class);
        NeoForge.EVENT_BUS.register(TankEvents.class);
        NeoForge.EVENT_BUS.register(CultivationEvents.class);

        if (FMLEnvironment.dist.isClient()) {
            NeoForge.EVENT_BUS.register(MementoClientEvents.class);
        }

        NeoForge.EVENT_BUS.addListener(this::addReloadListener);
    }

    private void addReloadListener(AddReloadListenerEvent event) {
        event.addListener(new MilestoneManager());
    }
}
