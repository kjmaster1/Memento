package com.kjmaster.memento.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.StringRepresentable;
import org.jetbrains.annotations.NotNull;

public record StatBehavior(
        ResourceLocation stat,
        MergeStrategy mergeStrategy
) {
    public enum MergeStrategy implements StringRepresentable {
        SUM, // Add both values (e.g. Total Kills)
        MAX, // Take the higher value (e.g. Longest Shot)
        MIN, // Take the lower value (e.g. Fastest Run)
        AVERAGE; // Weighted Average (e.g. Average Damage)

        public static final Codec<MergeStrategy> CODEC = StringRepresentable.fromEnum(MergeStrategy::values);

        @Override
        public @NotNull String getSerializedName() {
            return name().toLowerCase();
        }
    }

    public static final Codec<StatBehavior> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ResourceLocation.CODEC.fieldOf("stat").forGetter(StatBehavior::stat),
            MergeStrategy.CODEC.optionalFieldOf("merge_strategy", MergeStrategy.SUM).forGetter(StatBehavior::mergeStrategy)
    ).apply(instance, StatBehavior::new));
}