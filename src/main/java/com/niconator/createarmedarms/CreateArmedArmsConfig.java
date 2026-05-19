package com.niconator.createarmedarms;

import net.neoforged.neoforge.common.ModConfigSpec;

final class CreateArmedArmsConfig {
    static final ModConfigSpec SPEC;
    static final ModConfigSpec.DoubleValue RANGED_TARGETING_RANGE;
    static final ModConfigSpec.DoubleValue ANIMATION_BASE_RPM;
    static final ModConfigSpec.DoubleValue WEAPON_STRESS_MULTIPLIER;

    private static final double DEFAULT_RANGED_TARGETING_RANGE = 15.0D;
    private static final double DEFAULT_ANIMATION_BASE_RPM = 32.0D;
    private static final double DEFAULT_WEAPON_STRESS_MULTIPLIER = 2.0D;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        RANGED_TARGETING_RANGE = builder
                .comment("Maximum targeting range in blocks for bows and crossbows. Vanilla skeleton bow range is 15.")
                .translation("config.create_armed_arms.ranged_targeting_range")
                .defineInRange("rangedTargetingRange", DEFAULT_RANGED_TARGETING_RANGE, 0.0D, 128.0D);
        ANIMATION_BASE_RPM = builder
                .comment("Base RPM used for animation/combat speed scaling. Higher values make actions slower at the same arm speed.")
                .translation("config.create_armed_arms.animation_base_rpm")
                .defineInRange("animationBaseRpm", DEFAULT_ANIMATION_BASE_RPM, 1.0D, 512.0D);
        WEAPON_STRESS_MULTIPLIER = builder
                .comment("Stress multiplier applied when a mechanical arm is equipped with a weapon.")
                .translation("config.create_armed_arms.weapon_stress_multiplier")
                .defineInRange("weaponStressMultiplier", DEFAULT_WEAPON_STRESS_MULTIPLIER, 0.0D, 64.0D);
        SPEC = builder.build();
    }

    private CreateArmedArmsConfig() {
    }
}
