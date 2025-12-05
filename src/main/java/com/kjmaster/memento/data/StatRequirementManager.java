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
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class StatRequirementManager extends SimpleJsonResourceReloadListener {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    // Split rules into O(1) indexed rules and O(N) global rules
    private static final List<StatRequirement> GLOBAL_RULES = new ArrayList<>();
    private static final Map<Item, List<StatRequirement>> INDEXED_RULES = new HashMap<>();

    public StatRequirementManager() {
        super(GSON, "stat_requirements");
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> object, @NotNull ResourceManager resourceManager, @NotNull ProfilerFiller profiler) {
        GLOBAL_RULES.clear();
        INDEXED_RULES.clear();

        for (Map.Entry<ResourceLocation, JsonElement> entry : object.entrySet()) {
            StatRequirement.CODEC.parse(JsonOps.INSTANCE, entry.getValue())
                    .resultOrPartial(err -> Memento.LOGGER.error("Failed to parse restriction rule {}: {}", entry.getKey(), err))
                    .ifPresent(rule -> {
                        // If optimization is requested, index it
                        if (rule.optimizedItems().isPresent() && !rule.optimizedItems().get().isEmpty()) {
                            for (ResourceLocation itemId : rule.optimizedItems().get()) {
                                Item item = BuiltInRegistries.ITEM.get(itemId);
                                INDEXED_RULES.computeIfAbsent(item, k -> new ArrayList<>()).add(rule);
                            }
                        } else {
                            // Fallback to global scan
                            GLOBAL_RULES.add(rule);
                        }
                    });
        }
        Memento.LOGGER.info("Loaded {} usage restrictions ({} global, {} indexed)",
                GLOBAL_RULES.size() + INDEXED_RULES.values().stream().mapToInt(List::size).sum(),
                GLOBAL_RULES.size(),
                INDEXED_RULES.size()
        );
    }

    /**
     * Checks if the player is allowed to use the given stack.
     * @return Optional.empty() if allowed, or Optional containing the error message key if denied.
     */
    public static Optional<String> checkRestriction(Player player, ItemStack stack) {
        if (stack.isEmpty()) return Optional.empty();

        // 1. Check Indexed Rules (O(1)) - Highly efficient
        List<StatRequirement> indexed = INDEXED_RULES.get(stack.getItem());
        if (indexed != null) {
            Optional<String> result = checkRules(player, stack, indexed);
            if (result.isPresent()) return result;
        }

        // 2. Check Global Rules (O(N)) - Only iterate if absolutely necessary
        if (!GLOBAL_RULES.isEmpty()) {
            return checkRules(player, stack, GLOBAL_RULES);
        }

        return Optional.empty();
    }

    private static Optional<String> checkRules(Player player, ItemStack stack, List<StatRequirement> rules) {
        for (StatRequirement rule : rules) {
            // Predicate check matches the item to the rule
            if (rule.restrictedItem().test(stack)) {
                boolean passed = false;

                if (rule.scope() == StatRequirement.RequirementScope.SELF) {
                    // Check the item itself (Fast)
                    long val = MementoAPI.getStat(stack, rule.stat());
                    if (val >= rule.min()) passed = true;
                } else {
                    // Check inventory for a source item (Slower, but only runs if rule matches)
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
        // 1. Scan main inventory
        for (ItemStack item : player.getInventory().items) {
            if (checkSourceItem(item, rule)) return true;
        }

        // 2. Scan Vanilla Equipment Slots (Armor, Offhand)
        // Explicit loop avoids lambda/array allocation
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            ItemStack stack = player.getItemBySlot(slot);
            if (!stack.isEmpty() && checkSourceItem(stack, rule)) {
                return true;
            }
        }

        // 3. Scan Curios (via SlotHelper to handle mod dependency safely)
        // Uses stackless exception for control flow to avoid boolean array allocation
        try {
            SlotHelper.processCurios(player, (stack, idx) -> {
                if (checkSourceItem(stack, rule)) {
                    throw FoundItemException.INSTANCE;
                }
            });
        } catch (FoundItemException e) {
            return true;
        }

        return false;
    }

    private static boolean checkSourceItem(ItemStack stack, StatRequirement rule) {
        if (stack.isEmpty()) return false;

        // Must match the "Source" predicate (e.g. "is a Stone Pickaxe")
        if (rule.sourceMatcher().isPresent() && !rule.sourceMatcher().get().test(stack)) {
            return false;
        }

        // Must have the stat
        if (!stack.has(ModDataComponents.TRACKER_MAP)) return false;

        TrackerMap map = stack.get(ModDataComponents.TRACKER_MAP);
        return map.getValue(rule.stat()) >= rule.min();
    }

    // Optimization: Stackless exception used solely for breaking out of the Curios lambda loop.
    private static final class FoundItemException extends RuntimeException {
        public static final FoundItemException INSTANCE = new FoundItemException();
        private FoundItemException() {
            super(null, null, false, false);
        }
    }
}