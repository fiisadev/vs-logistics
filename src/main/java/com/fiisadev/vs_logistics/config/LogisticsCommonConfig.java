package com.fiisadev.vs_logistics.config;

import net.minecraftforge.common.ForgeConfigSpec;

public class LogisticsCommonConfig {
    public static final ForgeConfigSpec SPEC;

    public static ForgeConfigSpec.IntValue PUMP_RATE;

    public static ForgeConfigSpec.DoubleValue MAX_FLUID_PORT_LINK_DISTANCE;

    public static ForgeConfigSpec.DoubleValue HOSE_BREAK_DISTANCE;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();

        PUMP_RATE = builder
                .comment("Amount of fluid in milibuckets transffered by fluid pump and fluid port per tick")
                .defineInRange("pumpRate", 60, 0, 100000);

        MAX_FLUID_PORT_LINK_DISTANCE = builder
                .comment("Maximal distance from fluid port to connect tank, 0 will disable remote linking")
                .defineInRange("maxFluidPortLinkDistance", 20D, 0, 512);

        HOSE_BREAK_DISTANCE = builder
                .comment("Distance which the fluid pump hose can reach up to")
                .defineInRange("hoseBreakDistance", 24D, 0, 128);

        SPEC = builder.build();
    }
}
