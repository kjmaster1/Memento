package com.kjmaster.memento.advancement;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.advancements.critereon.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

public class StatChangedTrigger extends SimpleCriterionTrigger<StatChangedTrigger.TriggerInstance> {

    @Override
    public @NotNull Codec<TriggerInstance> codec() {
        return TriggerInstance.CODEC;
    }

    public void trigger(ServerPlayer player, ItemStack stack, ResourceLocation statId, long value) {
        this.trigger(player, instance -> instance.matches(stack, statId, value));
    }

    public record TriggerInstance(
            Optional<ContextAwarePredicate> player,
            ResourceLocation statId,
            MinMaxBounds.Ints range,
            Optional<ItemPredicate> item
    ) implements SimpleCriterionTrigger.SimpleInstance {

        public static final Codec<TriggerInstance> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                EntityPredicate.ADVANCEMENT_CODEC.optionalFieldOf("player").forGetter(TriggerInstance::player),
                ResourceLocation.CODEC.fieldOf("stat").forGetter(TriggerInstance::statId),
                MinMaxBounds.Ints.CODEC.optionalFieldOf("range", MinMaxBounds.Ints.ANY).forGetter(TriggerInstance::range),
                ItemPredicate.CODEC.optionalFieldOf("item").forGetter(TriggerInstance::item)
        ).apply(instance, TriggerInstance::new));

        public boolean matches(ItemStack stack, ResourceLocation statId, long value) {
            if (!this.statId.equals(statId)) return false;
            if (!this.range.matches((int) value)) return false;
            return this.item.isEmpty() || this.item.get().test(stack);
        }
    }
}