package com.kjmaster.memento;

import com.kjmaster.memento.data.*;
import com.kjmaster.memento.event.*;
import com.kjmaster.memento.data.StatMilestoneManager;
import com.kjmaster.memento.registry.ModDataAttachments;
import com.kjmaster.memento.registry.ModDataComponents;
import com.kjmaster.memento.registry.ModLootConditionTypes;
import com.kjmaster.memento.registry.ModRecipes;
import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceLocation;
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

        // 1. Register (Mod Bus)
        ModDataComponents.COMPONENTS.register(modEventBus);
        ModDataAttachments.ATTACHMENT_TYPES.register(modEventBus);
        ModRecipes.SERIALIZERS.register(modEventBus);
        ModLootConditionTypes.LOOT_CONDITION_TYPES.register(modEventBus);

        // 2. Register Game Logic Events (Game Bus)

        NeoForge.EVENT_BUS.register(new MilestoneEventHandler());
        NeoForge.EVENT_BUS.register(new AdvancementEventHandler());
        NeoForge.EVENT_BUS.register(new EffectEventHandler());
        NeoForge.EVENT_BUS.register(new AttributeEventHandler());
        NeoForge.EVENT_BUS.register(new EnchantmentEventHandler());

        NeoForge.EVENT_BUS.register(DataDrivenEvents.class);
        NeoForge.EVENT_BUS.register(MetadataEvents.class);
        NeoForge.EVENT_BUS.register(AviationEvents.class);
        NeoForge.EVENT_BUS.register(CultivationEvents.class);

        NeoForge.EVENT_BUS.register(ContextEvents.class);

        if (FMLEnvironment.dist.isClient()) {
            NeoForge.EVENT_BUS.register(MementoClientEvents.class);
        }

        NeoForge.EVENT_BUS.addListener(this::addReloadListener);
    }

    public static ResourceLocation loc(String path) {
        return ResourceLocation.fromNamespaceAndPath(Memento.MODID, path);
    }

    private void addReloadListener(AddReloadListenerEvent event) {
        event.addListener(new StatMilestoneManager());
        event.addListener(new StatTriggerManager());
        event.addListener(new StatAttributeManager());
        event.addListener(new StatEffectManager());
        event.addListener(new StatEnchantmentManager());
        event.addListener(new StatRepairCapManager());
        event.addListener(new StatBehaviorManager());
        event.addListener(new StatUsageSpeedManager());
        event.addListener(new StatProjectileLogicManager());
        event.addListener(new StatVisualPrestigeManager());
        event.addListener(new StatMasteryManager());
    }
}
