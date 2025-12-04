package com.kjmaster.memento.event;

import com.kjmaster.memento.api.MementoAPI;
import com.kjmaster.memento.data.StatEffect;
import com.kjmaster.memento.data.StatEffectManager;
import com.kjmaster.memento.util.SlotHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.List;

public class EffectEventHandler {

    // --- 1. ATTACK (On Hit) & DEFEND (On Hurt) ---
    @SubscribeEvent
    public void onLivingDamage(LivingDamageEvent.Post event) {
        // A. ATTACK Logic
        if (event.getSource().getEntity() instanceof ServerPlayer attacker) {
            applyContext(attacker, event.getEntity(), StatEffect.EffectContext.ATTACK);
        }

        // B. DEFEND Logic
        if (event.getEntity() instanceof ServerPlayer defender) {
            // "Target" in JSON: USER = Defender, ATTACK_TARGET = Attacker
            LivingEntity attacker = (event.getSource().getEntity() instanceof LivingEntity le) ? le : null;
            applyContext(defender, attacker, StatEffect.EffectContext.DEFEND);
        }
    }

    // --- 2. KILL (On Death) ---
    @SubscribeEvent
    public void onLivingDeath(LivingDeathEvent event) {
        if (event.getSource().getEntity() instanceof ServerPlayer killer) {
            applyContext(killer, event.getEntity(), StatEffect.EffectContext.KILL);
        }
    }

    // --- 3. MINE (On Block Break) ---
    @SubscribeEvent
    public void onBlockBreak(BlockEvent.BreakEvent event) {
        if (event.getPlayer() instanceof ServerPlayer player) {
            // Block break doesn't have an "Entity Target", so target is null
            applyContext(player, null, StatEffect.EffectContext.MINE);
        }
    }

    // --- 4. PASSIVE (Every Tick) ---
    @SubscribeEvent
    public void onPlayerTick(PlayerTickEvent.Post event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            // Only run once per second (20 ticks) to save performance,
            // since passive effects usually have durations > 1s.
            if (player.tickCount % 20 == 0) {
                applyContext(player, null, StatEffect.EffectContext.PASSIVE);
            }
        }
    }

    /**
     * Core logic to check rules and apply effects.
     * @param user The player holding the Memento items.
     * @param target The other entity (victim/attacker), or null if none.
     * @param context The current event context.
     */
    private void applyContext(ServerPlayer user, LivingEntity target, StatEffect.EffectContext context) {
        List<StatEffect> rules = StatEffectManager.getRules(context);
        if (rules.isEmpty()) return;

        for (SlotHelper.SlotContext slotCtx : SlotHelper.getAllWornItems(user)) {
            ItemStack stack = slotCtx.stack();

            for (StatEffect rule : rules) {
                // 1. Check Slot Requirement
                if (slotCtx.slot() != null) {
                    // Vanilla Slot: Use standard checking (e.g., does HEAD match the rule?)
                    if (!rule.slots().test(slotCtx.slot())) continue;
                } else {
                    // Curios Slot: Since "Ring" or "Charm" isn't in EquipmentSlotGroup,
                    // we only allow it if the rule targets "ANY".
                    if (rule.slots() != net.minecraft.world.entity.EquipmentSlotGroup.ANY) continue;
                }

                // 2. Check Stat Requirement
                long statValue = MementoAPI.getStat(stack, rule.stat());
                if (statValue < rule.minInfo()) continue;

                // 3. Check Chance
                if (user.getRandom().nextFloat() >= rule.chance()) continue;

                // 4. Apply Effect
                LivingEntity effectTarget = (rule.target() == StatEffect.Target.USER) ? user : target;
                if (effectTarget != null) {
                    effectTarget.addEffect(rule.createInstance());
                }
            }
        }
    }
}