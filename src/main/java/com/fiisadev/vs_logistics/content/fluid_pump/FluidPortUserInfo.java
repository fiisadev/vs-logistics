package com.fiisadev.vs_logistics.content.fluid_pump;

import com.fiisadev.vs_logistics.content.fluid_port.FluidPortBlock;
import com.fiisadev.vs_logistics.content.fluid_port.FluidPortBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3d;
import org.valkyrienskies.mod.common.VSGameUtilsKt;

public record FluidPortUserInfo(FluidPumpBlockEntity fluidPump, FluidPortBlockEntity fluidPort) implements IUserInfo {
    public boolean is(Object object) {
        return fluidPort.equals(object);
    }

    public void write(CompoundTag tag) {
        tag.putString("UserType", "NOZZLE");
        tag.putString("UserId", Long.toString(fluidPort.getBlockPos().asLong()));
    }

    public Vec3 getHoseEnd(float partialTicks) {
        Level level = fluidPort.getLevel();
        BlockPos blockPos = fluidPort.getBlockPos();
        Direction facing = fluidPort.getBlockState().getValue(FluidPortBlock.FACING);

        Vec3 pos = blockPos.getCenter().add(Vec3.atLowerCornerOf(facing.getNormal()).scale(1f));

        if (VSGameUtilsKt.getShipManagingPos(level, blockPos) != null)
            return VSGameUtilsKt.toWorldCoordinates(level, pos);

        return pos;
    }

    public Vec3 getHoseDir(float partialTicks) {
        Level level = fluidPort.getLevel();
        BlockPos blockPos = fluidPort.getBlockPos();
        Direction facing = fluidPort.getBlockState().getValue(FluidPortBlock.FACING);

        Vec3 dir = Vec3.atLowerCornerOf(facing.getNormal()).scale(-1);

        var ship = VSGameUtilsKt.getShipManagingPos(level, blockPos);
        if (ship != null) {
            Vector3d worldDir = ship.getTransform().getShipToWorldRotation().transform(new Vector3d(dir.x, dir.y, dir.z));
            return new Vec3(worldDir.x, worldDir.y, worldDir.z);
        }

        return dir;
    }

    public void pushFluid() {
        fluidPump.pushFluid(fluidPort.getFirstTank());
    }

    public void pullFluid() {
        fluidPump.pullFluid(fluidPort.getFirstTank());
    }

    public void onStopUsing() {
        fluidPort.setFluidPump(null);
    }

    public static FluidPortUserInfo from(FluidPumpBlockEntity fluidPump, String id, Level level) {
        return level.getBlockEntity(BlockPos.of(Long.parseLong(id))) instanceof FluidPortBlockEntity be ? new FluidPortUserInfo(fluidPump, be) : null;
    }
}
