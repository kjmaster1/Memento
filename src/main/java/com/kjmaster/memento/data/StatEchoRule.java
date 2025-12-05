package com.kjmaster.memento.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Optional;

public record StatEchoRule(
        ResourceLocation id,
        Trigger trigger,
        Action action,
        List<Condition> conditions,
        Parameters parameters,
        int cooldownTicks,
        Optional<List<ResourceLocation>> items // Renamed from optimizedItems
) {
    public static final Codec<StatEchoRule> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Trigger.CODEC.fieldOf("trigger").forGetter(StatEchoRule::trigger),
            Action.CODEC.fieldOf("action").forGetter(StatEchoRule::action),
            Condition.CODEC.listOf().fieldOf("conditions").forGetter(StatEchoRule::conditions),
            Parameters.CODEC.optionalFieldOf("parameters", Parameters.EMPTY).forGetter(StatEchoRule::parameters),
            Codec.INT.optionalFieldOf("cooldown", 20).forGetter(StatEchoRule::cooldownTicks),
            ResourceLocation.CODEC.listOf().optionalFieldOf("items").forGetter(StatEchoRule::items)
    ).apply(instance, (trigger, action, conditions, params, cool, items) ->
            new StatEchoRule(ResourceLocation.parse("memento:runtime"), trigger, action, conditions, params, cool, items)));

    public enum Trigger implements StringRepresentable {
        ATTACK, MINE, JUMP, LAND, BLOCK_INTERACT;
        public static final Codec<Trigger> CODEC = StringRepresentable.fromEnum(Trigger::values);
        @Override public @NotNull String getSerializedName() { return name().toLowerCase(); }
    }

    public enum Action implements StringRepresentable {
        EXPLOSION, LIGHTNING, SPAWN_ENTITY, PLAY_SOUND, PARTICLE_BURST;
        public static final Codec<Action> CODEC = StringRepresentable.fromEnum(Action::values);
        @Override public @NotNull String getSerializedName() { return name().toLowerCase(); }
    }

    public record Condition(ResourceLocation stat, long min) {
        public static final Codec<Condition> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                ResourceLocation.CODEC.fieldOf("stat").forGetter(Condition::stat),
                Codec.LONG.fieldOf("min").forGetter(Condition::min)
        ).apply(instance, Condition::new));
    }

    public record Parameters(
            Optional<Double> radius,
            Optional<Boolean> causesFire,
            Optional<String> blockInteraction,
            Optional<ResourceLocation> id,
            Optional<Integer> count,
            Optional<Float> volume,
            Optional<Float> pitch,
            Optional<Double> speed
    ) {
        public static final Parameters EMPTY = new Parameters(Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty());

        public static final Codec<Parameters> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.DOUBLE.optionalFieldOf("radius").forGetter(Parameters::radius),
                Codec.BOOL.optionalFieldOf("causes_fire").forGetter(Parameters::causesFire),
                Codec.STRING.optionalFieldOf("block_interaction").forGetter(Parameters::blockInteraction),
                ResourceLocation.CODEC.optionalFieldOf("id").forGetter(Parameters::id),
                Codec.INT.optionalFieldOf("count").forGetter(Parameters::count),
                Codec.FLOAT.optionalFieldOf("volume").forGetter(Parameters::volume),
                Codec.FLOAT.optionalFieldOf("pitch").forGetter(Parameters::pitch),
                Codec.DOUBLE.optionalFieldOf("speed").forGetter(Parameters::speed)
        ).apply(instance, Parameters::new));

        public Level.ExplosionInteraction getExplosionInteraction() {
            return blockInteraction.map(s -> {
                try { return Level.ExplosionInteraction.valueOf(s.toUpperCase()); }
                catch (Exception e) { return Level.ExplosionInteraction.BLOCK; }
            }).orElse(Level.ExplosionInteraction.BLOCK);
        }
    }
}