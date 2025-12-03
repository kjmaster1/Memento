package com.kjmaster.memento.registry;

import com.kjmaster.memento.Memento;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

public class ModDataAttachments {
    public static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES = DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, Memento.MODID);

    // Stores the position (Vec3) from where a projectile was fired
    public static final DeferredHolder<AttachmentType<?>, AttachmentType<Vec3>> PROJECTILE_ORIGIN = ATTACHMENT_TYPES.register(
            "projectile_origin",
            () -> AttachmentType.builder(() -> Vec3.ZERO).serialize(Vec3.CODEC).build()
    );
}