package com.fiisadev.vs_logistics.registry;

import com.fiisadev.vs_logistics.content.fluid_port.FluidPortBlockEntity;
import com.fiisadev.vs_logistics.content.fluid_pump.FluidPumpBlockEntity;
import com.fiisadev.vs_logistics.content.fluid_pump.handlers.FluidPortHandler;
import com.fiisadev.vs_logistics.content.pipe_wrench.PipeWrenchItem;
import com.simibubi.create.AllFluids;
import com.simibubi.create.content.fluids.tank.FluidTankBlockEntity;
import com.simibubi.create.foundation.ponder.CreateSceneBuilder;
import net.createmod.catnip.math.Pointing;
import net.createmod.ponder.api.PonderPalette;
import net.createmod.ponder.api.scene.SceneBuilder;
import net.createmod.ponder.api.scene.SceneBuildingUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;

public class LogisticsPonderScenes {
    public static void fluidPortTutorial(SceneBuilder builder, SceneBuildingUtil util) {
        CreateSceneBuilder scene = new CreateSceneBuilder(builder);
        scene.title("fluid_port", "How to Fluid Port");
        scene.showBasePlate();

        BlockPos portPos = util.grid().at(2, 1, 2);
        BlockPos tankAPos = util.grid().at(4, 1, 2);
        BlockPos tankBPos = util.grid().at(0, 1, 2);

        scene.world().showSection(util.select().position(portPos), Direction.DOWN);
        builder.idle(10);
        scene.overlay().showText(40)
                .text("Fluid Ports allow compact fluid transfer on VS ships.")
                .pointAt(util.vector().topOf(portPos))
                .placeNearTarget();
        builder.idle(55);

        scene.world().showSection(util.select().position(tankAPos), Direction.DOWN);
        scene.world().showSection(util.select().position(tankBPos), Direction.DOWN);
        scene.world().showSection(util.select().position(tankAPos.above()), Direction.DOWN);
        scene.world().showSection(util.select().position(tankBPos.above()), Direction.DOWN);
        builder.idle(30);

        scene.addKeyframe();

        scene.overlay().showControls(util.vector().blockSurface(portPos, Direction.UP), Pointing.RIGHT, 60)
                .withItem(new ItemStack(PipeWrenchItem.getPipeWrench()))
                .rightClick();
        builder.idle(80);

        scene.overlay().showOutline(PonderPalette.GREEN, "ponder-fluid-port", util.select().position(2, 1, 2), 130);
        builder.idle(30);

        scene.overlay().showOutline(PonderPalette.OUTPUT, "ponder-fluid-port-tank-1", util.select().fromTo(4, 1, 2, 4, 2, 2), 1200);
        builder.idle(20);
        scene.overlay().showOutline(PonderPalette.OUTPUT, "ponder-fluid-port-tank-2", util.select().fromTo(0, 1, 2, 0, 2, 2), 1200);
        builder.idle(20);

        scene.overlay().showText(30)
                .text("Link the Port to any Fluid Storage.")
                .pointAt(util.vector().blockSurface(tankBPos, Direction.WEST))
                .attachKeyFrame()
                .placeNearTarget();

        builder.idle(60);

        builder.addKeyframe();

        builder.idle(10);

        scene.world().showSection(util.select().fromTo(4, 1, 0, 4, 2, 0), Direction.DOWN);
        builder.idle(40);

        scene.world().modifyBlockEntity(portPos, FluidPortBlockEntity.class, be -> {
            be.setFluidPumpPos(new BlockPos(4, 2, 0));
        });

        scene.world().modifyBlockEntity(util.grid().at(4, 2, 0), FluidPumpBlockEntity.class, be -> {
            be.setPumpHandler(new FluidPortHandler(be, util.grid().at(2, 1, 2)));
        });

        scene.overlay().showText(32*2+15)
                .text("PUSH")
                .pointAt(util.vector().topOf(util.grid().at(4, 2, 2)))
                .placeNearTarget();

        scene.overlay().showText(32*4+25)
                .text("PUSH")
                .pointAt(util.vector().topOf(util.grid().at(0, 2, 2)))
                .placeNearTarget();

        FluidStack fluid = new FluidStack(AllFluids.HONEY.get(), 500);
        for (int i = 0; i < 32; i++) {
            scene.world().modifyBlockEntity(util.grid().at(4, 1, 2), FluidTankBlockEntity.class, be -> {
                be.getTankInventory().fill(fluid, IFluidHandler.FluidAction.EXECUTE);
            });

            builder.idle(2);
        }

        builder.idle(10);

        scene.world().modifyBlockEntity(portPos, FluidPortBlockEntity.class, be -> {
            be.setFluidPumpPos(null);
        });
        scene.world().modifyBlockEntity(util.grid().at(4, 2, 0), FluidPumpBlockEntity.class, be -> {
            be.setPumpHandler(null);
        });

        builder.idle(5);
        builder.addKeyframe();
        builder.idle(5);

        scene.overlay().showText(32*2+5)
                .text("PULL")
                .pointAt(util.vector().topOf(util.grid().at(4, 2, 2)))
                .placeNearTarget();
        scene.overlay().showOutline(PonderPalette.MEDIUM, "ponder-fluid-port-tank-1", util.select().fromTo(4, 1, 2, 4, 2, 2), 1200);

        for (int i = 0; i < 32; i++) {
            scene.world().modifyBlockEntity(util.grid().at(4, 1, 2), FluidTankBlockEntity.class, be -> {
                be.getTankInventory().drain(fluid, IFluidHandler.FluidAction.EXECUTE);
            });
            scene.world().modifyBlockEntity(util.grid().at(0, 1, 2), FluidTankBlockEntity.class, be -> {
                be.getTankInventory().fill(fluid, IFluidHandler.FluidAction.EXECUTE);
            });

            builder.idle(2);
        }

        builder.idle(5);
        builder.addKeyframe();
        builder.idle(5);

        scene.overlay().showOutline(PonderPalette.GREEN, "ponder-fluid-port-tank-1", util.select().fromTo(4, 1, 2, 4, 2, 2), 1200);
        scene.overlay().showOutline(PonderPalette.GREEN, "ponder-fluid-port-tank-2", util.select().fromTo(0, 1, 2, 0, 2, 2), 1200);

        scene.overlay().showText(42)
                .text("EQUALIZE")
                .pointAt(util.vector().topOf(util.grid().at(4, 2, 2)))
                .placeNearTarget();

        scene.overlay().showText(42)
                .text("EQUALIZE")
                .pointAt(util.vector().topOf(util.grid().at(0, 2, 2)))
                .placeNearTarget();

        for (int i = 0; i < 16; i++) {
            scene.world().modifyBlockEntity(util.grid().at(4, 1, 2), FluidTankBlockEntity.class, be -> {
                be.getTankInventory().fill(fluid, IFluidHandler.FluidAction.EXECUTE);
            });
            scene.world().modifyBlockEntity(util.grid().at(0, 1, 2), FluidTankBlockEntity.class, be -> {
                be.getTankInventory().drain(fluid, IFluidHandler.FluidAction.EXECUTE);
            });

            builder.idle(2);
        }

        builder.idle(20);

        scene.markAsFinished();
    }

    public static void fluidPort2Tutorial(SceneBuilder builder, SceneBuildingUtil util) {
        CreateSceneBuilder scene = new CreateSceneBuilder(builder);
        scene.title("fluid_port_2", "Ship-to-World Logistics");
        scene.showBasePlate();

        // Define relevant positions based on your section selection
        BlockPos portPos = util.grid().at(3, 1, 2);
        BlockPos pumpPos = util.grid().at(3, 1, 1);

        builder.idle(10);
        // Show the main port assembly
        scene.world().showSection(util.select().fromTo(3, 1, 1, 3, 1, 3), Direction.DOWN);
        builder.idle(10);
        scene.world().showSection(util.select().fromTo(3, 2, 2, 3, 2, 3), Direction.DOWN);

        builder.idle(20);

        // Instruction 1: Linking Constraint
        scene.overlay().showText(60)
                .text("Fluid Port linking only works once the VS Ship has been assembled.")
                .attachKeyFrame()
                .pointAt(util.vector().topOf(portPos))
                .placeNearTarget();

        builder.idle(70);

        // Instruction 2: Ship to World transfer
        scene.overlay().showText(80)
                .text("For ship-to-world transfer, fluids must be actively pumped out of the Port.")
                .attachKeyFrame()
                .pointAt(util.vector().blockSurface(pumpPos, Direction.WEST))
                .placeNearTarget();

        builder.idle(90);

        scene.markAsFinished();
    }
}
