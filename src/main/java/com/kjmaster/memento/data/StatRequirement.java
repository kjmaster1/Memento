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

public record StatRequirement(
        ItemPredicate restrictedItem,
        ResourceLocation stat,
        long min,
        RequirementScope scope,
        Optional<ItemPredicate> sourceMatcher,
        Optional<String> failureMessage,
        Optional<HolderSet<Item>> items
) {
    public enum RequirementScope implements StringRepresentable {
        SELF, INVENTORY;
        public static final Codec<RequirementScope> CODEC = StringRepresentable.fromEnum(RequirementScope::values);
        @Override public @NotNull String getSerializedName() { return name().toLowerCase(); }
    }

    public static final Codec<StatRequirement> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ItemPredicate.CODEC.fieldOf("restricted_item").forGetter(StatRequirement::restrictedItem),
            ResourceLocation.CODEC.fieldOf("stat").forGetter(StatRequirement::stat),
            Codec.LONG.fieldOf("min").forGetter(StatRequirement::min),
            RequirementScope.CODEC.optionalFieldOf("scope", RequirementScope.SELF).forGetter(StatRequirement::scope),
            ItemPredicate.CODEC.optionalFieldOf("source_matcher").forGetter(StatRequirement::sourceMatcher),
            Codec.STRING.optionalFieldOf("failure_message").forGetter(StatRequirement::failureMessage),
            RegistryCodecs.homogeneousList(Registries.ITEM).optionalFieldOf("items").forGetter(StatRequirement::items)
    ).apply(instance, StatRequirement::new));
}