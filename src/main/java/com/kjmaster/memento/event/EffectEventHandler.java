package com.kjmaster.memento.event;

import com.kjmaster.memento.api.MementoAPI;
import com.kjmaster.memento.data.StatEffect;
import com.kjmaster.memento.data.StatEffectManager;
import com.kjmaster.memento.util.SlotHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.List;

public class EffectEventHandler {

    @SubscribeEvent
    public static void onLivingDamage(LivingDamageEvent.Post event) {
        if (event.getSource().getEntity() instanceof ServerPlayer attacker) {
            applyContext(attacker, event.getEntity(), StatEffect.EffectContext.ATTACK);
        }

        if (event.getEntity() instanceof ServerPlayer defender) {
            LivingEntity attacker = (event.getSource().getEntity() instanceof LivingEntity le) ? le : null;
            applyContext(defender, attacker, StatEffect.EffectContext.DEFEND);
        }
    }

    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        if (event.getSource().getEntity() instanceof ServerPlayer killer) {
            applyContext(killer, event.getEntity(), StatEffect.EffectContext.KILL);
        }
    }

    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        if (event.getPlayer() instanceof ServerPlayer player) {
            applyContext(player, null, StatEffect.EffectContext.MINE);
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            if (player.tickCount % 20 == 0) {
                applyContext(player, null, StatEffect.EffectContext.PASSIVE);
            }
        }
    }

    private static void applyContext(ServerPlayer user, LivingEntity target, StatEffect.EffectContext context) {
        for (SlotHelper.SlotContext slotCtx : SlotHelper.getAllWornItems(user)) {
            ItemStack stack = slotCtx.stack();

            // OPTIMIZATION: Only fetch rules that are Global OR specific to this Item
            List<StatEffect> rules = StatEffectManager.getRules(context, stack);
            if (rules.isEmpty()) continue;

            for (StatEffect rule : rules) {
                if (slotCtx.slot() != null) {
                    if (!rule.slots().test(slotCtx.slot())) continue;
                } else {
                    if (rule.slots() != net.minecraft.world.entity.EquipmentSlotGroup.ANY) continue;
                }

                long statValue = MementoAPI.getStat(stack, rule.stat());
                if (statValue < rule.minInfo()) continue;

                if (user.getRandom().nextFloat() >= rule.chance()) continue;

                LivingEntity effectTarget = (rule.target() == StatEffect.Target.USER) ? user : target;
                if (effectTarget != null) {
                    effectTarget.addEffect(rule.createInstance());
                }
            }
        }
    }
}