package com.fiisadev.vs_logistics.config;

import net.minecraftforge.common.ForgeConfigSpec;

public class LogisticsClientConfig {
    public static final ForgeConfigSpec SPEC;

    public static ForgeConfigSpec.IntValue HOSE_SEGMENTS;

    public static ForgeConfigSpec.IntValue HOSE_RADIAL_SEGMENTS;

    public static ForgeConfigSpec.DoubleValue HOSE_RADIUS;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();

        builder.push("hose");

        HOSE_RADIUS = builder
                .defineInRange("radius", 0.05D, 0, 8);

        HOSE_SEGMENTS = builder
                .comment("Number of hose \"bending points\"")
                .defineInRange("segments", 32, 2, 128);

        HOSE_RADIAL_SEGMENTS = builder
                .comment("Number of points forming a circle")
                .defineInRange("radialSegments", 5, 3, 16);

        builder.pop();

        SPEC = builder.build();
    }
}
