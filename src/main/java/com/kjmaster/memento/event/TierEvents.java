package com.kjmaster.memento.event;

import com.kjmaster.memento.api.MementoAPI;
import com.kjmaster.memento.data.StatTierManager;
import com.kjmaster.memento.data.StatTierRule;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Tier;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

import java.util.List;

public class TierEvents {

    @SubscribeEvent
    public static void onHarvestCheck(PlayerEvent.HarvestCheck event) {
        // If Vanilla or another mod already said YES, we don't need to interfere.
        if (event.canHarvest()) return;

        Player player = event.getEntity();
        ItemStack stack = player.getMainHandItem();
        BlockState state = event.getTargetBlock();

        List<StatTierRule> rules = StatTierManager.getApplicableRules(stack);
        if (rules.isEmpty()) return;

        for (StatTierRule rule : rules) {
            long val = MementoAPI.getStat(stack, rule.stat());
            if (val >= rule.min()) {
                Tier tier = rule.resolveTier();

                // 1.21 Logic: A tier works if the block is NOT in its "Incorrect for Drops" tag.
                if (!state.is(tier.getIncorrectBlocksForDrops())) {
                    event.setCanHarvest(true);
                    return; // Found a working tier, stop looking.
                }
            }
        }
    }

    @SubscribeEvent
    public static void onBreakSpeed(PlayerEvent.BreakSpeed event) {
        // If the tool is already effective (speed > 1.0), we generally don't mess with it.
        // We only want to boost "Ineffective" tools that we forced to be harvestable.
        if (event.getOriginalSpeed() > 1.0f) return;

        Player player = event.getEntity();
        ItemStack stack = player.getMainHandItem();
        BlockState state = event.getState();

        List<StatTierRule> rules = StatTierManager.getApplicableRules(stack);
        if (rules.isEmpty()) return;

        float bestSpeed = 1.0f;
        boolean foundValidTier = false;

        for (StatTierRule rule : rules) {
            long val = MementoAPI.getStat(stack, rule.stat());
            if (val >= rule.min()) {
                Tier tier = rule.resolveTier();

                // Check if this tier effectively mines the block
                if (!state.is(tier.getIncorrectBlocksForDrops())) {
                    // We found a valid tier. Since we can't compare "Levels" anymore,
                    // we compare Mining Speed to find the "best" upgrade.
                    if (tier.getSpeed() > bestSpeed) {
                        bestSpeed = tier.getSpeed();
                        foundValidTier = true;
                    }
                }
            }
        }

        if (foundValidTier) {
            event.setNewSpeed(bestSpeed);
        }
    }
}