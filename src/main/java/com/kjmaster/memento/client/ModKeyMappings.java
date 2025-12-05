package com.kjmaster.memento.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import org.lwjgl.glfw.GLFW;

public class ModKeyMappings {
    public static final KeyMapping TOGGLE_COMPACT_MODE = new KeyMapping(
            "key.memento.toggle_compact",
            KeyConflictContext.GUI, // Primarily used when looking at tooltips in inventory
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_Z,
            "key.categories.memento"
    );
}