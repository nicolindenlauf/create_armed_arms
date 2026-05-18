package com.niconator.createarmedarms;

import net.neoforged.neoforge.common.ModConfigSpec;

final class CreateArmedArmsConfig {
    static final ModConfigSpec SPEC;
    static final ModConfigSpec.DoubleValue RANGED_TARGETING_RANGE;

    private static final double SKELETON_BOW_RANGE = 15.0D;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        RANGED_TARGETING_RANGE = builder
                .comment("Maximum targeting range in blocks for bows and crossbows. Vanilla skeleton bow range is 15.")
                .translation("config.create_armed_arms.ranged_targeting_range")
                .defineInRange("rangedTargetingRange", SKELETON_BOW_RANGE, 0.0D, 128.0D);
        SPEC = builder.build();
    }

    private CreateArmedArmsConfig() {
    }
}
