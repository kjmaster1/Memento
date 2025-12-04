package com.kjmaster.memento.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.kjmaster.memento.Memento;
import com.kjmaster.memento.api.MementoAPI;
import com.kjmaster.memento.component.TrackerMap;
import com.kjmaster.memento.registry.ModDataComponents;
import com.kjmaster.memento.util.SlotHelper;
import com.mojang.serialization.JsonOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class StatRequirementManager extends SimpleJsonResourceReloadListener {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private static final List<StatRequirement> RULES = new ArrayList<>();

    public StatRequirementManager() {
        super(GSON, "stat_requirements");
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> object, @NotNull ResourceManager resourceManager, @NotNull ProfilerFiller profiler) {
        RULES.clear();
        for (Map.Entry<ResourceLocation, JsonElement> entry : object.entrySet()) {
            StatRequirement.CODEC.parse(JsonOps.INSTANCE, entry.getValue())
                    .resultOrPartial(err -> Memento.LOGGER.error("Failed to parse restriction rule {}: {}", entry.getKey(), err))
                    .ifPresent(RULES::add);
        }
        Memento.LOGGER.info("Loaded {} usage restrictions", RULES.size());
    }

    /**
     * Checks if the player is allowed to use the given stack.
     * @return Optional.empty() if allowed, or Optional containing the error message key if denied.
     */
    public static Optional<String> checkRestriction(Player player, ItemStack stack) {
        if (stack.isEmpty()) return Optional.empty();

        for (StatRequirement rule : RULES) {
            if (rule.restrictedItem().test(stack)) {
                boolean passed = false;

                if (rule.scope() == StatRequirement.RequirementScope.SELF) {
                    // Check the item itself
                    long val = MementoAPI.getStat(stack, rule.stat());
                    if (val >= rule.min()) passed = true;
                } else {
                    // Check inventory for a source item
                    passed = checkInventory(player, rule);
                }

                if (!passed) {
                    return Optional.of(rule.failureMessage().orElse("message.memento.restriction.default"));
                }
            }
        }
        return Optional.empty();
    }

    private static boolean checkInventory(Player player, StatRequirement rule) {
        // Scan main inventory
        for (ItemStack item : player.getInventory().items) {
            if (checkSourceItem(item, rule)) return true;
        }
        // Scan armor/offhand/curios via SlotHelper
        // Note: SlotHelper might overlap with main inventory depending on implementation, but it's safe.
        // We use a simple atomic flag to break early.
        final boolean[] found = {false};
        SlotHelper.forEachWornItem(player, ctx -> {
            if (!found[0] && checkSourceItem(ctx.stack(), rule)) {
                found[0] = true;
            }
        });
        return found[0];
    }

    private static boolean checkSourceItem(ItemStack stack, StatRequirement rule) {
        if (stack.isEmpty()) return false;

        // Must match the "Source" predicate (e.g. "is a Stone Pickaxe")
        if (rule.sourceMatcher().isPresent() && !rule.sourceMatcher().get().test(stack)) {
            return false;
        }

        // Must have the stat
        // Optimization: Direct component check avoids API recursion if we were calling from API (we aren't here)
        if (!stack.has(ModDataComponents.TRACKER_MAP)) return false;

        TrackerMap map = stack.get(ModDataComponents.TRACKER_MAP);
        return map.getValue(rule.stat()) >= rule.min();
    }
}