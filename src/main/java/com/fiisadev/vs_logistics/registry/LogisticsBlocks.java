package com.fiisadev.vs_logistics.registry;

import com.fiisadev.vs_logistics.VSLogistics;
import com.fiisadev.vs_logistics.content.fluid_port.FluidPortBlock;
import com.fiisadev.vs_logistics.content.fluid_pump.FluidPumpBlock;
import com.simibubi.create.foundation.data.BlockStateGen;
import com.simibubi.create.foundation.data.CreateRegistrate;
import com.tterrag.registrate.util.entry.BlockEntry;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;

import static com.simibubi.create.foundation.data.TagGen.*;
import static com.simibubi.create.foundation.data.ModelGen.customItemModel;

public class LogisticsBlocks {
    private static final CreateRegistrate REGISTRATE = VSLogistics.registrate();

    static {
        REGISTRATE.setCreativeTab(LogisticsCreativeModeTabs.BASE_CREATIVE_TAB);
    }

    public static BlockEntry<FluidPortBlock> FLUID_PORT = REGISTRATE.block("fluid_port", FluidPortBlock::new)
            .initialProperties(() -> Blocks.IRON_BLOCK)
            .properties(BlockBehaviour.Properties::requiresCorrectToolForDrops)
            .transform(axeOrPickaxe())
            .blockstate(BlockStateGen.directionalBlockProvider(true))
            .item()
            .transform(customItemModel())
            .register();

    public static BlockEntry<FluidPumpBlock> FLUID_PUMP = REGISTRATE.block("fluid_pump", FluidPumpBlock::new)
            .initialProperties(() -> Blocks.IRON_BLOCK)
            .properties(BlockBehaviour.Properties::requiresCorrectToolForDrops)
            .properties(BlockBehaviour.Properties::noOcclusion)
            .transform(axeOrPickaxe())
            .blockstate(BlockStateGen.horizontalBlockProvider(true))
            .item()
            .transform(customItemModel())
            .register();

    public static void register() {}
}
