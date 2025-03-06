package com.leclowndu93150.playertotem;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;

public class PTConfig {
    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();
    private static final ForgeConfigSpec SPEC;

    public static final ForgeConfigSpec.BooleanValue RENDER_INSTEAD_OF_ROCKET;

    static {
        BUILDER.push("My Mini-Me Settings");

        RENDER_INSTEAD_OF_ROCKET = BUILDER.comment("If true, renders player totem on the right instead of the left hand")
                .define("renderInsteadOfRocket", false);

        BUILDER.pop();
        SPEC = BUILDER.build();
    }

    public static void register() {
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, SPEC);
    }

    public static boolean shouldRenderInsteadOfRocket() {
        return RENDER_INSTEAD_OF_ROCKET.get();
    }
}