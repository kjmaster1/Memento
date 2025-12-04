package com.kjmaster.memento.api.event;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.ICancellableEvent;
import org.jetbrains.annotations.Nullable;

/**
 * Fired when a Memento stat is about to change or has changed.
 */
public abstract class StatChangeEvent extends Event {
    private final LivingEntity entity;
    private final ItemStack stack;
    private final ResourceLocation statId;
    private final long originalValue;
    private final long newValue;

    protected StatChangeEvent(LivingEntity entity, ItemStack stack, ResourceLocation statId, long originalValue, long newValue) {
        this.entity = entity;
        this.stack = stack;
        this.statId = statId;
        this.originalValue = originalValue;
        this.newValue = newValue;
    }

    public LivingEntity getEntity() { return entity; }

    @Nullable
    public ServerPlayer getPlayer() {
        return (entity instanceof ServerPlayer player) ? player : null;
    }

    public ItemStack getItem() { return stack; }
    public ResourceLocation getStatId() { return statId; }
    public long getOriginalValue() { return originalValue; }
    public long getNewValue() { return newValue; }

    /**
     * Fired BEFORE the stat is updated.
     * Cancel this event to prevent the change.
     */
    public static class Pre extends StatChangeEvent implements ICancellableEvent {
        public Pre(LivingEntity entity, ItemStack stack, ResourceLocation statId, long originalValue, long newValue) {
            super(entity, stack, statId, originalValue, newValue);
        }
    }

    /**
     * Fired AFTER the stat is updated.
     * Use this for Achievements, Quests, or UI updates.
     */
    public static class Post extends StatChangeEvent {
        public Post(LivingEntity entity, ItemStack stack, ResourceLocation statId, long originalValue, long newValue) {
            super(entity, stack, statId, originalValue, newValue);
        }
    }
}