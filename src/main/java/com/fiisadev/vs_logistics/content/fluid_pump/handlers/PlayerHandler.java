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
import org.valkyrienskies.mod.common.VSGameUtilsKt;

import java.util.Optional;
import java.util.UUID;

public record PlayerHandler(FluidPumpBlockEntity fluidPump, UUID playerId) implements IFluidPumpHandler {
    @Override
    public boolean equals(Object obj) {
        return obj instanceof PlayerHandler handler && playerId.equals(handler.playerId) && fluidPump.getBlockPos().equals(handler.fluidPump.getBlockPos());
    }

    public boolean is(Object object) { return object instanceof Player p && p.getUUID().equals(playerId); }

    public void write(CompoundTag tag) {
        tag.putString("UserType", "PLAYER");
        tag.putString("UserId", playerId.toString());
    }

    public Vec3 getHoseEnd(float partialTicks) {
        if (fluidPump.getLevel() == null)
            return Vec3.ZERO;

        Player player = fluidPump.getLevel().getPlayerByUUID(playerId);
        if (player == null)
            return Vec3.ZERO;

        return player
                .getPosition(partialTicks)
                .add(HoseUtils.
                        getNozzleHandlePosition(player, partialTicks));
    }

    public Vec3 getHoseDir(float partialTicks) {
        if (fluidPump.getLevel() == null)
            return Vec3.ZERO;

        Player player = fluidPump.getLevel().getPlayerByUUID(playerId);
        if (player == null)
            return Vec3.ZERO;

        float bodyRotation = player.yBodyRotO + (player.yBodyRot - player.yBodyRotO) * partialTicks;
        return new Vec3(0, 0, 1)
                .yRot((float)Math.toRadians(-bodyRotation))
                .normalize();
    }

    private Optional<IFluidHandler> getFluidTank() {
        if (fluidPump.getLevel() == null)
            return Optional.empty();

        Player player = fluidPump.getLevel().getPlayerByUUID(playerId);
        if (player == null)
            return Optional.empty();

        HitResult result = player.pick(5f, 0f, true);

        if (result instanceof BlockHitResult hit) {
            BlockEntity be = player.level().getBlockEntity(hit.getBlockPos());

            return Optional.ofNullable(be instanceof FluidPortBlockEntity fluidPort ? fluidPort.getFluidHandler() : null);
        }

        return Optional.empty();
    }

    public void pushFluid() {
        if (fluidPump.getLevel() == null)
            return;

        Player player = fluidPump.getLevel().getPlayerByUUID(playerId);
        if (player == null)
            return;

        if (FluidPumpHandler.isNozzleKeyDown(player.getUUID()) && !player.isShiftKeyDown()) {
            getFluidTank().ifPresent(fluidPump::pushFluid);
        }
    }

    public void pullFluid() {
        if (fluidPump.getLevel() == null)
            return;

        Player player = fluidPump.getLevel().getPlayerByUUID(playerId);
        if (player == null)
            return;

        if (FluidPumpHandler.isNozzleKeyDown(player.getUUID()) && !player.isShiftKeyDown()) {
            getFluidTank().ifPresent(fluidPump::pullFluid);
        }
    }

    public void tick() {
        if (fluidPump.getLevel() == null)
            return;

        Player player = fluidPump.getLevel().getPlayerByUUID(playerId);
        if (player == null)
            return;

        if (VSGameUtilsKt.toWorldCoordinates(fluidPump.getLevel(), fluidPump.getBlockPos()).distanceToSqr(player.position()) > Math.pow(24, 2)) {
            fluidPump.breakHose();
        }
    }

    public void syncCapability(BlockPos fluidPumpPos) {
        if (fluidPump.getLevel() == null)
            return;

        Player player = fluidPump.getLevel().getPlayerByUUID(playerId);
        if (player == null)
            return;

        LogisticsNetwork.CHANNEL.send(
                PacketDistributor.ALL.noArg(),
                new SyncFluidPumpPlayerCapPacket(fluidPumpPos, player.getUUID())
        );
    }

    @Override
    public void onStartUsing() {
        if (fluidPump.getLevel() == null)
            return;

        Player player = fluidPump.getLevel().getPlayerByUUID(playerId);
        if (player == null)
            return;

        player.getCapability(FluidPumpPlayerDataProvider.FLUID_PUMP_PLAYER_DATA).ifPresent((playerData) -> {
            playerData.setFluidPumpPos(fluidPump.getBlockPos());
            syncCapability(fluidPump.getBlockPos());
        });
    }

    public void onStopUsing() {
        if (fluidPump.getLevel() == null)
            return;

        Player player = fluidPump.getLevel().getPlayerByUUID(playerId);
        if (player == null)
            return;

        player.getCapability(FluidPumpPlayerDataProvider.FLUID_PUMP_PLAYER_DATA).ifPresent((playerData) -> {
            playerData.setFluidPumpPos(null);
            syncCapability(null);
        });
    }
}
