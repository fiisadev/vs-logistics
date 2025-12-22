package com.fiisadev.vs_logistics.content.fluid_pump.handlers;

import com.fiisadev.vs_logistics.content.fluid_port.FluidPortBlock;
import com.fiisadev.vs_logistics.content.fluid_port.FluidPortBlockEntity;
import com.fiisadev.vs_logistics.content.fluid_pump.FluidPumpBlockEntity;
import com.fiisadev.vs_logistics.content.fluid_pump.IFluidPumpHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.fluids.capability.IFluidHandler;
import org.joml.Vector3d;
import org.valkyrienskies.mod.common.VSGameUtilsKt;

import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

public record FluidPortHandler(FluidPumpBlockEntity fluidPump, BlockPos fluidPortPos) implements IFluidPumpHandler {
    @Override
    public boolean equals(Object obj) {
        return obj instanceof FluidPortHandler handler && fluidPortPos.equals(handler.fluidPortPos) && fluidPump.getBlockPos().equals(handler.fluidPump.getBlockPos());
    }

    public boolean is(Object object) { return object instanceof FluidPortBlockEntity fluidPort && fluidPort.getBlockPos().equals(fluidPortPos); }

    public void write(CompoundTag tag) {
        tag.putString("UserType", "FLUID_PORT");
        tag.putString("UserId", Long.toString(fluidPortPos.asLong()));
    }

    private void withFluidPortDo(Consumer<FluidPortBlockEntity> action) {
        if (fluidPump.getLevel() == null)
            return;

        if (!(fluidPump.getLevel().getBlockEntity(fluidPortPos) instanceof FluidPortBlockEntity fluidPort))
            return;

        action.accept(fluidPort);
    }

    public Vec3 getHoseEnd(float partialTicks) {
        AtomicReference<Vec3> value = new AtomicReference<>(fluidPump.getBlockPos().getCenter());

        withFluidPortDo((fluidPort) -> {
            Level level = fluidPump.getLevel();
            BlockPos blockPos = fluidPort.getBlockPos();
            Direction facing = fluidPort.getBlockState().getValue(FluidPortBlock.FACING);

            Vec3 pos = blockPos.getCenter().add(Vec3.atLowerCornerOf(facing.getNormal()).scale(1f));

            if (VSGameUtilsKt.getShipManagingPos(level, blockPos) != null)
                value.set(VSGameUtilsKt.toWorldCoordinates(level, pos));
            else
                value.set(pos);
        });

        return value.get();
    }

    public Vec3 getHoseDir(float partialTicks) {
        AtomicReference<Vec3> value = new AtomicReference<>(Vec3.ZERO);
        withFluidPortDo((fluidPort) -> {
            BlockPos blockPos = fluidPort.getBlockPos();
            Direction facing = fluidPort.getBlockState().getValue(FluidPortBlock.FACING);

            Vec3 dir = Vec3.atLowerCornerOf(facing.getNormal()).scale(-1);

            var ship = VSGameUtilsKt.getShipManagingPos(fluidPump.getLevel(), blockPos);
            if (ship != null) {
                Vector3d worldDir = ship.getTransform().getShipToWorldRotation().transform(new Vector3d(dir.x, dir.y, dir.z));
                value.set(new Vec3(worldDir.x, worldDir.y, worldDir.z));
            } else {
                value.set(dir);
            }
        });

        return value.get();
    }

    public void pushFluid() {
        withFluidPortDo((fluidPort) -> {
            IFluidHandler fluidHandler = fluidPort.getFluidHandler();
            if (fluidHandler == null) return;

            fluidPump.pushFluid(fluidHandler);
        });

    }

    public void pullFluid() {
        withFluidPortDo((fluidPort) -> {
            IFluidHandler fluidHandler = fluidPort.getFluidHandler();
            if (fluidHandler == null) return;

            fluidPump.pullFluid(fluidHandler);
        });
    }

    public void onStartUsing() {
        withFluidPortDo((fluidPort) -> {
            fluidPort.setFluidPumpPos(fluidPump.getBlockPos());
        });
    }

    public void onStopUsing() {
        withFluidPortDo((fluidPort) -> {
            fluidPort.setFluidPumpPos(null);
        });
    }

    public void tick() {
        withFluidPortDo((fluidPort) -> {
            Vec3 fluidPumpPos = VSGameUtilsKt.toWorldCoordinates(fluidPump.getLevel(), fluidPump.getBlockPos().getCenter());
            Vec3 fluidPortPos = VSGameUtilsKt.toWorldCoordinates(fluidPump.getLevel(), fluidPort.getBlockPos());

            if (fluidPumpPos.distanceToSqr(fluidPortPos) > Math.pow(24, 2)) {
                fluidPump.breakHose();
            }
        });
    }
}
