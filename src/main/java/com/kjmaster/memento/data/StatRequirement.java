package com.kjmaster.memento.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.advancements.critereon.ItemPredicate;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.StringRepresentable;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

public record StatRequirement(
        ItemPredicate restrictedItem,        // The item that is gated
        ResourceLocation stat,               // The stat required
        long min,                            // The minimum value
        RequirementScope scope,              // SELF (check this item) or INVENTORY (check if player has another item)
        Optional<ItemPredicate> sourceMatcher, // If INVENTORY scope, which item must have the stat?
        Optional<String> failureMessage      // Translation key for error
) {
    public enum RequirementScope implements StringRepresentable {
        SELF,
        INVENTORY;

        public static final Codec<RequirementScope> CODEC = StringRepresentable.fromEnum(RequirementScope::values);

        @Override
        public @NotNull String getSerializedName() {
            return name().toLowerCase();
        }
    }

    public static final Codec<StatRequirement> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ItemPredicate.CODEC.fieldOf("restricted_item").forGetter(StatRequirement::restrictedItem),
            ResourceLocation.CODEC.fieldOf("stat").forGetter(StatRequirement::stat),
            Codec.LONG.fieldOf("min").forGetter(StatRequirement::min),
            RequirementScope.CODEC.optionalFieldOf("scope", RequirementScope.SELF).forGetter(StatRequirement::scope),
            ItemPredicate.CODEC.optionalFieldOf("source_matcher").forGetter(StatRequirement::sourceMatcher),
            Codec.STRING.optionalFieldOf("failure_message").forGetter(StatRequirement::failureMessage)
    ).apply(instance, StatRequirement::new));
}