package com.kjmaster.memento;

import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.List;

public class Config {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    // List of stats whose DEFAULT hardcoded logic should be disabled.
    private static final ModConfigSpec.ConfigValue<List<? extends String>> DISABLED_DEFAULTS = BUILDER
            .comment("List of Memento Stat IDs to disable default tracking logic for.",
                    "Use this if you want to replace the default logic with your own JSON triggers.",
                    "Example: [\"memento:blocks_broken\"]")
            .defineListAllowEmpty(
                    "disabled_default_stats",
                    List.of(),
                    () -> "memento:blocks_broken", // Supplier: Used by GUI to create new empty entries
                    s -> s instanceof String       // Validator
            );

    public static final ModConfigSpec.ConfigValue<List<? extends String>> VISUAL_STATS = BUILDER
            .comment("List of Stat IDs to expose as Item Properties for Resource Packs.",
                    "This allows you to change item textures based on stat values using 'overrides' in model JSONs.",
                    "Default Memento stats are included automatically.")
            .defineListAllowEmpty(
                    "visual_stats",
                    List.of(),
                    () -> "mypack:custom_stat",
                    s -> s instanceof String
            );


    static final ModConfigSpec SPEC = BUILDER.build();

    public static boolean isDefaultEnabled(ResourceLocation statId) {
        return !DISABLED_DEFAULTS.get().contains(statId.toString());
    }
}
