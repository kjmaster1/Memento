package com.kjmaster.memento.event;

import com.kjmaster.memento.api.MementoAPI;
import com.kjmaster.memento.component.EchoCooldowns;
import com.kjmaster.memento.data.StatEchoManager;
import com.kjmaster.memento.data.StatEchoRule;
import com.kjmaster.memento.registry.ModDataComponents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingEvent;
import net.neoforged.neoforge.event.entity.living.LivingFallEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockEvent;

import java.util.List;

public class EchoEvents {

    @SubscribeEvent
    public static void onAttack(LivingDamageEvent.Post event) {
        if (event.getSource().getEntity() instanceof ServerPlayer player) {
            processEchoes(player, player.getMainHandItem(), StatEchoRule.Trigger.ATTACK, event.getEntity().position());
        }
    }

    @SubscribeEvent
    public static void onMine(BlockEvent.BreakEvent event) {
        if (event.getPlayer() instanceof ServerPlayer player) {
            processEchoes(player, player.getMainHandItem(), StatEchoRule.Trigger.MINE, event.getPos().getCenter());
        }
    }

    @SubscribeEvent
    public static void onJump(LivingEvent.LivingJumpEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            processEchoes(player, player.getMainHandItem(), StatEchoRule.Trigger.JUMP, player.position());
        }
    }

    @SubscribeEvent
    public static void onLand(LivingFallEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && event.getDistance() > 3.0f) {
            processEchoes(player, player.getMainHandItem(), StatEchoRule.Trigger.LAND, player.position());
        }
    }

    @SubscribeEvent
    public static void onInteract(PlayerInteractEvent.RightClickBlock event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            processEchoes(player, event.getItemStack(), StatEchoRule.Trigger.BLOCK_INTERACT, event.getPos().getCenter());
        }
    }

    private static void processEchoes(ServerPlayer player, ItemStack stack, StatEchoRule.Trigger trigger, Vec3 pos) {
        if (stack.isEmpty()) return;

        List<StatEchoRule> rules = StatEchoManager.getRules(trigger);
        if (rules.isEmpty()) return;

        long gameTime = player.level().getGameTime();
        EchoCooldowns cooldowns = stack.getOrDefault(ModDataComponents.ECHO_COOLDOWNS, EchoCooldowns.EMPTY);
        boolean cooldownChanged = false;

        for (StatEchoRule rule : rules) {
            if (rule.items().isPresent()) {
                if (rule.items().get().stream().noneMatch(id -> id.equals(BuiltInRegistries.ITEM.getKey(stack.getItem())))) {
                    continue;
                }
            }

            if (!checkConditions(stack, rule)) continue;
            if (cooldowns.isOnCooldown(rule.id(), gameTime)) continue;

            executeAction(player, rule, pos);

            if (rule.cooldownTicks() > 0) {
                cooldowns = cooldowns.setCooldown(rule.id(), gameTime, rule.cooldownTicks());
                cooldownChanged = true;
            }
        }

        if (cooldownChanged) {
            stack.set(ModDataComponents.ECHO_COOLDOWNS, cooldowns);
        }
    }

    private static boolean checkConditions(ItemStack stack, StatEchoRule rule) {
        for (StatEchoRule.Condition cond : rule.conditions()) {
            if (MementoAPI.getStat(stack, cond.stat()) < cond.min()) return false;
        }
        return true;
    }

    private static void executeAction(ServerPlayer player, StatEchoRule rule, Vec3 pos) {
        ServerLevel level = player.serverLevel();
        StatEchoRule.Parameters params = rule.parameters();

        switch (rule.action()) {
            case EXPLOSION -> {
                level.explode(player, pos.x, pos.y, pos.z, params.radius().orElse(3.0).floatValue(), params.causesFire().orElse(false), params.getExplosionInteraction());
            }
            case LIGHTNING -> {
                var bolt = EntityType.LIGHTNING_BOLT.create(level);
                if (bolt != null) {
                    bolt.moveTo(pos);
                    level.addFreshEntity(bolt);
                }
            }
            case SPAWN_ENTITY -> {
                if (params.id().isPresent()) {
                    EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.get(params.id().get());
                    int count = params.count().orElse(1);
                    for (int i = 0; i < count; i++) type.spawn(level, BlockPos.containing(pos), MobSpawnType.EVENT);
                }
            }
            case PLAY_SOUND -> {
                if (params.id().isPresent()) {
                    SoundEvent sound = BuiltInRegistries.SOUND_EVENT.get(params.id().get());
                    if (sound != null)
                        level.playSound(null, BlockPos.containing(pos), sound, SoundSource.PLAYERS, params.volume().orElse(1.0f), params.pitch().orElse(1.0f));
                }
            }
            case PARTICLE_BURST -> {
                if (params.id().isPresent()) {
                    ParticleType<?> type = BuiltInRegistries.PARTICLE_TYPE.get(params.id().get());
                    if (type instanceof SimpleParticleType simpleParticle) {
                        level.sendParticles(simpleParticle, pos.x, pos.y, pos.z, params.count().orElse(10), params.radius().orElse(0.5), params.radius().orElse(0.5), params.radius().orElse(0.5), params.speed().orElse(0.1));
                    }
                }
            }
        }
    }
}