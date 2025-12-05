package com.kjmaster.memento.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EquipmentSlotGroup;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Optional;

public record StatEffect(
        ResourceLocation stat,
        long minInfo,
        MobEffect effect,
        int durationTicks,
        int amplifier,
        float chance,
        Target target,
        EffectContext context,       // WHEN does this apply?
        EquipmentSlotGroup slots,    // WHERE must the item be?
        Optional<List<ResourceLocation>> optimizedItems // Explicit list of items for O(1) lookup
) {
    public enum Target implements StringRepresentable {
        USER, ATTACK_TARGET;

        @Override
        public @NotNull String getSerializedName() {
            return name().toLowerCase();
        }
    }

    public enum EffectContext implements StringRepresentable {
        ATTACK,         // On Hit (Attacker)
        DEFEND,         // On Hit (Victim)
        KILL,           // On Death (Attacker)
        MINE,           // On Block Break
        PASSIVE;        // Constant (Tick)

        @Override
        public @NotNull String getSerializedName() {
            return name().toLowerCase();
        }

        public static final Codec<EffectContext> CODEC = StringRepresentable.fromEnum(EffectContext::values);
    }

    public static final Codec<StatEffect> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ResourceLocation.CODEC.fieldOf("stat").forGetter(StatEffect::stat),
            Codec.LONG.fieldOf("min_value").forGetter(StatEffect::minInfo),
            BuiltInRegistries.MOB_EFFECT.byNameCodec().fieldOf("effect").forGetter(StatEffect::effect),
            Codec.INT.optionalFieldOf("duration", 100).forGetter(StatEffect::durationTicks),
            Codec.INT.optionalFieldOf("amplifier", 0).forGetter(StatEffect::amplifier),
            Codec.FLOAT.optionalFieldOf("chance", 1.0f).forGetter(StatEffect::chance),
            Codec.STRING.xmap(Target::valueOf, Target::name).optionalFieldOf("target", Target.ATTACK_TARGET).forGetter(StatEffect::target),
            EffectContext.CODEC.optionalFieldOf("context", EffectContext.ATTACK).forGetter(StatEffect::context),
            EquipmentSlotGroup.CODEC.optionalFieldOf("slots", EquipmentSlotGroup.ANY).forGetter(StatEffect::slots),
            ResourceLocation.CODEC.listOf().optionalFieldOf("optimized_items").forGetter(StatEffect::optimizedItems)
    ).apply(instance, StatEffect::new));

    public MobEffectInstance createInstance() {
        return new MobEffectInstance(Holder.direct(effect), durationTicks, amplifier);
    }
}