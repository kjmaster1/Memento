package com.kjmaster.memento.api;

import com.kjmaster.memento.component.TrackerMap;
import com.kjmaster.memento.registry.ModDataComponents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class StatProviderRegistry {
    private static final List<IStatProvider> PROVIDERS = new ArrayList<>();

    // The Fallback Provider: Uses Memento's standard Data Component system
    public static final IStatProvider DEFAULT = new IStatProvider() {
        @Override
        public boolean canProvide(ItemStack stack, ResourceLocation statId) {
            return true; // Always handles anything not picked up by others
        }

        @Override
        public long getStat(ItemStack stack, ResourceLocation statId) {
            if (stack.isEmpty() || !stack.has(ModDataComponents.TRACKER_MAP)) return 0L;
            TrackerMap trackers = stack.get(ModDataComponents.TRACKER_MAP);
            return trackers.getValue(statId);
        }

        @Override
        public void setStat(ItemStack stack, ResourceLocation statId, long value) {
            // Ensure Identity
            if (!stack.has(ModDataComponents.ITEM_UUID)) {
                stack.set(ModDataComponents.ITEM_UUID, UUID.randomUUID());
            }

            TrackerMap current = stack.getOrDefault(ModDataComponents.TRACKER_MAP, TrackerMap.EMPTY);
            // We use the 'overwrite' merge function here because MementoAPI has already calculated the final value
            TrackerMap updated = current.update(statId, value, (a, b) -> b);

            stack.set(ModDataComponents.TRACKER_MAP, updated);
        }
    };

    /**
     * Registers a new Stat Provider.
     * Providers registered later take precedence (LIFO), allowing addons to override behavior.
     */
    public static void register(IStatProvider provider) {
        PROVIDERS.addFirst(provider);
    }

    /**
     * Finds the most relevant provider for the given context.
     */
    public static IStatProvider getProvider(ItemStack stack, ResourceLocation statId) {
        for (IStatProvider provider : PROVIDERS) {
            if (provider.canProvide(stack, statId)) {
                return provider;
            }
        }
        return DEFAULT;
    }
}