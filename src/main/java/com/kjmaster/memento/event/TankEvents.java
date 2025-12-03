package com.kjmaster.memento.event;

import com.kjmaster.memento.api.MementoAPI;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;

import static com.kjmaster.memento.registry.ModStats.DAMAGE_TAKEN;

public class TankEvents {

    @SubscribeEvent
    public static void onLivingDamage(LivingDamageEvent.Post event) {
        // We use .Post to ensure the damage actually happened (wasn't blocked/invulnerable)
        if (event.getEntity() instanceof ServerPlayer player) {
            float damageAmount = event.getNewDamage();

            long scaledDamage = (long) (damageAmount * 100);

            if (scaledDamage <= 0) return;

            for (ItemStack stack : player.getArmorSlots()) {
                if (!stack.isEmpty()) {
                    // Distribute the stat to every piece of armor worn
                    MementoAPI.incrementStat(player, stack, DAMAGE_TAKEN, scaledDamage);
                }
            }
        }
    }
}