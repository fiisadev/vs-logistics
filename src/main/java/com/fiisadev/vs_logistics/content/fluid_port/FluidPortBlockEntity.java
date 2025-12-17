package com.fiisadev.vs_logistics.content.fluid_port;

import com.fiisadev.vs_logistics.content.fluid_pump.*;
import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.fluid.SmartFluidTank;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.valkyrienskies.core.api.ships.Ship;
import org.valkyrienskies.mod.common.VSGameUtilsKt;

import java.util.List;
import java.util.Optional;

public class FluidPortBlockEntity extends SmartBlockEntity implements IHaveGoggleInformation {
    private BlockPos target;
    private final SmartFluidTank fluidTank = new SmartFluidTank(8000, this::onFluidStackChange);
    private final LazyOptional<IFluidHandler> fluidCapability = LazyOptional.of(() -> fluidTank);

    private BlockPos fluidPumpPos;

    public BlockPos getFluidPumpPos() {
        return fluidPumpPos;
    }

    public void setFluidPump(FluidPumpBlockEntity fluidPump) {
        this.fluidPumpPos = fluidPump != null ? fluidPump.getBlockPos() : null;
    }

    public FluidPortBlockEntity(BlockEntityType<?> pType, BlockPos pPos, BlockState pBlockState) {
        super(pType, pPos, pBlockState);
    }

    public boolean isValid() {
        if (target == null) return false;

        Ship shipA = VSGameUtilsKt.getShipManagingPos(getLevel(), getBlockPos());
        Ship shipB = VSGameUtilsKt.getShipManagingPos(getLevel(), target);

        if (shipA == null || shipB == null) return false;

        return shipA.getId() == shipB.getId();
    }

    public void onUse(Player player) {
        if (level == null)
            return;

        player.getCapability(FluidPumpPlayerDataProvider.FLUID_PUMP_PLAYER_DATA).ifPresent((playerData) -> {
            if (fluidPumpPos != null && playerData.getFluidPumpPos() == null) {
                FluidPumpBlockEntity.withBlockEntityDo(level, fluidPumpPos, (fluidPump) -> {
                    fluidPump.setUserInfo(new PlayerUserInfo(fluidPump, player));
                });

                fluidPumpPos = null;
                notifyUpdate();
                return;
            }

            if (fluidPumpPos == null && playerData.getFluidPumpPos() != null) {
                if (level.getBlockEntity(playerData.getFluidPumpPos()) instanceof FluidPumpBlockEntity fluidPump) {
                    fluidPumpPos = playerData.getFluidPumpPos();
                    fluidPump.setUserInfo(new FluidPortUserInfo(fluidPump, this));
                }
            }
        });
    }

    public void setTarget(@Nullable BlockPos pos) {
        this.target = pos;
    }

    public IFluidHandler getFirstTank() {
        IFluidHandler targetTank = getTargetTank().orElse(null);

        if (targetTank == null)
            return fluidTank;

        return targetTank;
    }

    public Optional<IFluidHandler> getTargetTank() {
        if (level == null) return Optional.empty();
        BlockEntity be = level.getBlockEntity(target);
        if (be == null) return Optional.empty();
        return be.getCapability(ForgeCapabilities.FLUID_HANDLER).resolve();
    }

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        BlockEntity be = level.getBlockEntity(target);
        if (be == null) return false;
        return containedFluidTooltip(tooltip, isPlayerSneaking, be.getCapability(ForgeCapabilities.FLUID_HANDLER));
    }

    @Override
    public void tick() {
        super.tick();

        if (level == null || level.isClientSide || target == null) return;
        if (!isValid()) return;

        BlockEntity be = level.getBlockEntity(target);
        if (!FluidPortBlock.isFluidTank(be)) return;

        be.getCapability(ForgeCapabilities.FLUID_HANDLER).ifPresent((cap) -> {
            int amount = fluidTank.getFluidAmount();
            int transferred = cap.fill(new FluidStack(fluidTank.getFluid(), amount), IFluidHandler.FluidAction.EXECUTE);
            fluidTank.drain(transferred, IFluidHandler.FluidAction.EXECUTE);
        });

        sendData();
    }

    @Override
    protected void read(CompoundTag tag, boolean clientPacket) {
        super.read(tag, clientPacket);

        if (tag.contains("Target"))
            target = BlockPos.of(tag.getLong("Target"));

        if (tag.contains("FluidPumpPos"))
            fluidPumpPos = BlockPos.of(tag.getLong("FluidPumpPos"));
        else
            fluidPumpPos = null;
    }

    @Override
    protected void write(CompoundTag tag, boolean clientPacket) {
        super.write(tag, clientPacket);

        if (target != null)
            tag.putLong("Target", target.asLong());

        if (fluidPumpPos != null)
            tag.putLong("FluidPumpPos", fluidPumpPos.asLong());
    }

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.FLUID_HANDLER) {
            return fluidCapability.cast();
        }

        return super.getCapability(cap, side);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        fluidCapability.invalidate();
    }

    private void onFluidStackChange(FluidStack fluidStack) {
        if (level != null && !level.isClientSide) {
            sendData();
            setChanged();
        }
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) { }
}
