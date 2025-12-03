package com.kjmaster.memento.event;

import com.kjmaster.memento.api.MementoAPI;
import com.kjmaster.memento.registry.ModStats;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CocoaBlock;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.NetherWartBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.event.level.BlockEvent;

public class CultivationEvents {

    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        if (event.getPlayer() instanceof ServerPlayer player) {
            ItemStack heldItem = player.getMainHandItem();
            BlockState state = event.getState();

            // 1. Check if holding a Hoe
            if (!heldItem.is(ItemTags.HOES)) {
                return;
            }

            // 2. Check if the block is a "Crop"
            // We check the Tag (Wheat, Carrots, etc.) OR specific blocks like Nether Wart that might not be in the tag
            boolean isCrop = state.is(BlockTags.CROPS)
                    || state.is(Tags.Blocks.PUMPKINS)
                    || state.is(Blocks.MELON)
                    || state.is(Blocks.COCOA)
                    || state.getBlock() instanceof NetherWartBlock;

            // 3. CRITICAL: Only count if it is fully grown!
            // Otherwise players can spam place/break a seed to farm stats.
            boolean isFullyGrown = isFullyGrown(state);

            if (isCrop && isFullyGrown) {
                MementoAPI.incrementStat(player, heldItem, ModStats.CROPS_HARVESTED, 1);
            }
        }
    }

    private static boolean isFullyGrown(BlockState state) {
        if (state.getBlock() instanceof CropBlock crop) {
            return crop.isMaxAge(state);
        }
        if (state.getBlock() instanceof NetherWartBlock) {
            return state.getValue(NetherWartBlock.AGE) >= 3;
        }
        if (state.getBlock() instanceof CocoaBlock) {
            return state.getValue(CocoaBlock.AGE) >= 2;
        }
        return true;
    }
}