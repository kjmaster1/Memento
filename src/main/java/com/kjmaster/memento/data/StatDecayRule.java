package com.kjmaster.memento.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.advancements.critereon.ItemPredicate;
import net.minecraft.core.HolderSet;
import net.minecraft.core.RegistryCodecs;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.Item;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

public record StatDecayRule(
        Trigger trigger,
        ResourceLocation stat,
        Operation operation,
        double value,
        Optional<Integer> frequency,
        Optional<ItemPredicate> itemFilter,
        Optional<HolderSet<Item>> items
) {
    public static final Codec<StatDecayRule> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Trigger.CODEC.fieldOf("trigger").forGetter(StatDecayRule::trigger),
            ResourceLocation.CODEC.fieldOf("stat").forGetter(StatDecayRule::stat),
            Operation.CODEC.fieldOf("operation").forGetter(StatDecayRule::operation),
            Codec.DOUBLE.fieldOf("value").forGetter(StatDecayRule::value),
            Codec.INT.optionalFieldOf("frequency").forGetter(StatDecayRule::frequency),
            ItemPredicate.CODEC.optionalFieldOf("item_filter").forGetter(StatDecayRule::itemFilter),
            RegistryCodecs.homogeneousList(Registries.ITEM).optionalFieldOf("items").forGetter(StatDecayRule::items)
    ).apply(instance, StatDecayRule::new));

    public enum Trigger implements StringRepresentable {
        TICK, DEATH, REPAIR;
        public static final Codec<Trigger> CODEC = StringRepresentable.fromEnum(Trigger::values);

        @Override
        public @NotNull String getSerializedName() {
            return name().toLowerCase();
        }
    }

    public enum Operation implements StringRepresentable {
        SUBTRACT, MULTIPLY;
        public static final Codec<Operation> CODEC = StringRepresentable.fromEnum(Operation::values);

        @Override
        public @NotNull String getSerializedName() {
            return name().toLowerCase();
        }
    }
}