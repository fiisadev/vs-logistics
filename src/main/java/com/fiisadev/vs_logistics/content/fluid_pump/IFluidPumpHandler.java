package com.fiisadev.vs_logistics.content.fluid_pump;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.phys.Vec3;

public interface IFluidPumpHandler {
    boolean equals(Object object);

    boolean is(Object object);

    void write(CompoundTag tag);

    Vec3 getHoseEnd(float partialTicks);

    Vec3 getHoseDir(float partialTicks);

    void pushFluid();

    void pullFluid();

    default void tick() {}

    default void onStartUsing() {}

    default void onStopUsing() {}
}
