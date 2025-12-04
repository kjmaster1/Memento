package com.kjmaster.memento.component;

import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;
import java.util.Map;

public record EchoCooldowns(Map<ResourceLocation, Long> cooldowns) {
    public static final EchoCooldowns EMPTY = new EchoCooldowns(Map.of());

    public static final Codec<EchoCooldowns> CODEC = Codec.unboundedMap(ResourceLocation.CODEC, Codec.LONG)
            .xmap(EchoCooldowns::new, EchoCooldowns::cooldowns);

    public static final StreamCodec<ByteBuf, EchoCooldowns> STREAM_CODEC = ByteBufCodecs.map(
            HashMap::new,
            ResourceLocation.STREAM_CODEC,
            ByteBufCodecs.VAR_LONG
    ).map(EchoCooldowns::new, c -> new HashMap<>(c.cooldowns()));

    public boolean isOnCooldown(ResourceLocation ruleId, long gameTime) {
        return cooldowns.getOrDefault(ruleId, 0L) > gameTime;
    }

    public EchoCooldowns setCooldown(ResourceLocation ruleId, long gameTime, int durationTicks) {
        Map<ResourceLocation, Long> newMap = new HashMap<>(cooldowns);
        newMap.put(ruleId, gameTime + durationTicks);
        return new EchoCooldowns(newMap);
    }
}