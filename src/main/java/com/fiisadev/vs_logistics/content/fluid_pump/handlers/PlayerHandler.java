package com.fiisadev.vs_logistics.content.fluid_pump.handlers;

import com.fiisadev.vs_logistics.client.utils.HoseUtils;
import com.fiisadev.vs_logistics.content.fluid_port.FluidPortBlockEntity;
import com.fiisadev.vs_logistics.content.fluid_pump.FluidPumpBlockEntity;
import com.fiisadev.vs_logistics.content.fluid_pump.FluidPumpPlayerDataProvider;
import com.fiisadev.vs_logistics.content.fluid_pump.IFluidPumpHandler;
import com.fiisadev.vs_logistics.event.FluidPumpHandler;
import com.fiisadev.vs_logistics.network.SyncFluidPumpPlayerCapPacket;
import com.fiisadev.vs_logistics.registry.LogisticsNetwork;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.network.PacketDistributor;

import java.util.Optional;
import java.util.UUID;

public record PlayerHandler(FluidPumpBlockEntity fluidPump, Player player) implements IFluidPumpHandler {
    public boolean is(Object object) {
        return object instanceof Player p && p.is(player);
    }

    public void write(CompoundTag tag) {
        tag.putString("UserType", "PLAYER");
        tag.putString("UserId", player.getUUID().toString());
    }

    public Vec3 getHoseEnd(float partialTicks) {
        return player
                .getPosition(partialTicks)
                .add(HoseUtils.
                        getNozzleHandlePosition(player, partialTicks));
    }

    public Vec3 getHoseDir(float partialTicks) {
        float bodyRotation = player.yBodyRotO + (player.yBodyRot - player.yBodyRotO) * partialTicks;
        return new Vec3(0, 0, 1)
                .yRot((float)Math.toRadians(-bodyRotation))
                .normalize();
    }

    private Optional<IFluidHandler> getFluidTank() {
        HitResult result = player.pick(5f, 0f, true);

        if (result instanceof BlockHitResult hit) {
            BlockEntity be = player.level().getBlockEntity(hit.getBlockPos());

            return Optional.ofNullable(be instanceof FluidPortBlockEntity fluidPort ? fluidPort.getFluidHandler() : null);
        }

        return Optional.empty();
    }

    public void pushFluid() {
        if (FluidPumpHandler.isNozzleKeyDown(player.getUUID()) && !player.isShiftKeyDown()) {
            getFluidTank().ifPresent(fluidPump::pushFluid);
        }
    }

    public void pullFluid() {
        if (FluidPumpHandler.isNozzleKeyDown(player.getUUID()) && !player.isShiftKeyDown()) {
            getFluidTank().ifPresent(fluidPump::pullFluid);
        }
    }

    public void tick() {
        if (player.position().distanceToSqr(fluidPump.getBlockPos().getCenter()) > Math.pow(24, 2)) {
            fluidPump.breakHose();
        }
    }

    public void syncCapability(BlockPos fluidPumpPos) {
        LogisticsNetwork.CHANNEL.send(
                PacketDistributor.ALL.noArg(),
                new SyncFluidPumpPlayerCapPacket(fluidPumpPos, player.getUUID())
        );
    }

    @Override
    public void onStartUsing() {
        player.getCapability(FluidPumpPlayerDataProvider.FLUID_PUMP_PLAYER_DATA).ifPresent((playerData) -> {
            playerData.setFluidPumpPos(fluidPump.getBlockPos());
            syncCapability(fluidPump.getBlockPos());
        });
    }

    public void onStopUsing() {
        player.getCapability(FluidPumpPlayerDataProvider.FLUID_PUMP_PLAYER_DATA).ifPresent((playerData) -> {
            playerData.setFluidPumpPos(null);
            syncCapability(null);
        });
    }

    public static PlayerHandler from(FluidPumpBlockEntity fluidPump, String id, Level level) {
        return new PlayerHandler(fluidPump, level.getPlayerByUUID(UUID.fromString(id)));
    }
}
