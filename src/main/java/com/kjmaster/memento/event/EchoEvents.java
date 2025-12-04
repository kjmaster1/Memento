package com.kjmaster.memento.event;

import com.kjmaster.memento.Memento;
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

    // --- Hooks ---

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

    // --- Core Logic ---

    private static void processEchoes(ServerPlayer player, ItemStack stack, StatEchoRule.Trigger trigger, Vec3 pos) {
        if (stack.isEmpty()) return;

        List<StatEchoRule> rules = StatEchoManager.getRules(trigger);
        if (rules.isEmpty()) return;

        long gameTime = player.level().getGameTime();
        EchoCooldowns cooldowns = stack.getOrDefault(ModDataComponents.ECHO_COOLDOWNS, EchoCooldowns.EMPTY);
        boolean cooldownChanged = false;

        for (StatEchoRule rule : rules) {
            // 1. Check Optimization (Item whitelist)
            if (rule.optimizedItems().isPresent()) {
                if (rule.optimizedItems().get().stream().noneMatch(id -> id.equals(BuiltInRegistries.ITEM.getKey(stack.getItem())))) {
                    continue;
                }
            }

            // 2. Check Conditions
            if (!checkConditions(stack, rule)) continue;

            // 3. Check Cooldown
            if (cooldowns.isOnCooldown(rule.id(), gameTime)) continue;

            // 4. EXECUTE
            executeAction(player, rule, pos);

            // 5. Update Cooldown
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
            if (MementoAPI.getStat(stack, cond.stat()) < cond.min()) {
                return false;
            }
        }
        return true;
    }

    private static void executeAction(ServerPlayer player, StatEchoRule rule, Vec3 pos) {
        ServerLevel level = player.serverLevel();
        StatEchoRule.Parameters params = rule.parameters();

        switch (rule.action()) {
            case EXPLOSION -> {
                float radius = params.radius().orElse(3.0).floatValue();
                boolean fire = params.causesFire().orElse(false);
                level.explode(
                        player,
                        pos.x, pos.y, pos.z,
                        radius,
                        fire,
                        params.getExplosionInteraction()
                );
            }
            case LIGHTNING -> {
                EntityType.LIGHTNING_BOLT.create(level).moveTo(pos);
                // Lightning entity handles its own spawning logic usually, but create() returns the entity
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
                    for (int i = 0; i < count; i++) {
                        type.spawn(level, BlockPos.containing(pos), MobSpawnType.EVENT);
                    }
                }
            }
            case PLAY_SOUND -> {
                if (params.id().isPresent()) {
                    SoundEvent sound = BuiltInRegistries.SOUND_EVENT.get(params.id().get());
                    if (sound != null) {
                        float volume = params.volume().orElse(1.0f);
                        float pitch = params.pitch().orElse(1.0f);
                        level.playSound(null, BlockPos.containing(pos), sound, SoundSource.PLAYERS, volume, pitch);
                    }
                }
            }
            case PARTICLE_BURST -> {
                if (params.id().isPresent()) {
                    // 1. Resolve the Particle Type from Registry
                    ParticleType<?> type = BuiltInRegistries.PARTICLE_TYPE.get(params.id().get());

                    // 2. Safety Check: We only support "Simple" particles (flame, heart, cloud, etc.)
                    // Complex particles like Redstone Dust require extra data (color) which we don't have in the JSON.
                    if (type instanceof SimpleParticleType simpleParticle) {

                        int count = params.count().orElse(10);
                        double speed = params.speed().orElse(0.1);

                        // Reuse 'radius' as the Delta/Spread area (default to small 0.5 burst)
                        double spread = params.radius().orElse(0.5);

                        // 3. Send to Client
                        // sendParticles(particle, x, y, z, count, deltaX, deltaY, deltaZ, speed)
                        level.sendParticles(
                                simpleParticle,
                                pos.x, pos.y, pos.z,
                                count,
                                spread, spread, spread, // Delta X, Y, Z
                                speed
                        );
                    } else {
                        Memento.LOGGER.warn("Echo rule {} tried to use complex particle {}", rule.id(), params.id().get());
                    }
                }
            }
        }
    }
}