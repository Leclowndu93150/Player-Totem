package com.leclowndu93150.myminime;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;

public class PTConfig {
    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();
    private static final ForgeConfigSpec SPEC;

    public static final ForgeConfigSpec.BooleanValue RENDER_INSTEAD_OF_ROCKET;
    public static final ForgeConfigSpec.BooleanValue FLIP_HELD_ITEM_POSITION;
    public static final ForgeConfigSpec.DoubleValue REVIVAL_HEALTH;

    static {
        BUILDER.push("My Mini-Me Settings");

        RENDER_INSTEAD_OF_ROCKET = BUILDER.comment("If true, renders player totem on the right instead of the left hand")
                .define("renderInsteadOfRocket", false);

        FLIP_HELD_ITEM_POSITION = BUILDER.comment("If true, flips the totem's held item position relative to player's main hand setting")
                .define("flipHeldItemPosition", false);

        REVIVAL_HEALTH = BUILDER.comment("Amount of health (in half hearts) to revive the player with")
                .defineInRange("revivalHealth", 1.0, 0.5, 20.0);

        BUILDER.pop();
        SPEC = BUILDER.build();
    }

    public static void register() {
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, SPEC);
    }

    public static boolean shouldRenderInsteadOfRocket() {
        return RENDER_INSTEAD_OF_ROCKET.get();
    }

    public static boolean shouldFlipHeldItemPosition() {
        return FLIP_HELD_ITEM_POSITION.get();
    }

    public static double getRevivalHealth() {
        return REVIVAL_HEALTH.get();
    }
}