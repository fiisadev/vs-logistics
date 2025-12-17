package com.fiisadev.vs_logistics.registry;

import com.fiisadev.vs_logistics.VSLogistics;
import com.fiisadev.vs_logistics.content.fluid_port.FluidPortBlockEntity;
import com.fiisadev.vs_logistics.content.fluid_port.FluidPortRenderer;
import com.fiisadev.vs_logistics.content.fluid_pump.FluidPumpBlockEntity;
import com.fiisadev.vs_logistics.content.fluid_pump.FluidPumpRenderer;
import com.simibubi.create.foundation.data.CreateRegistrate;
import com.tterrag.registrate.util.entry.BlockEntityEntry;

public class LogisticsBlockEntities {
    private static final CreateRegistrate REGISTRATE = VSLogistics.registrate();

    public static final BlockEntityEntry<FluidPortBlockEntity> FLUID_PORT = REGISTRATE
            .blockEntity("fluid_port", FluidPortBlockEntity::new)
            .renderer(() -> FluidPortRenderer::new)
            .validBlocks(LogisticsBlocks.FLUID_PORT)
            .register();

    public static final BlockEntityEntry<FluidPumpBlockEntity> FLUID_PUMP = REGISTRATE
            .blockEntity("fluid_pump", FluidPumpBlockEntity::new)
            .renderer(() -> FluidPumpRenderer::new)
            .validBlocks(LogisticsBlocks.FLUID_PUMP)
            .register();

    public static void register() {}
}
