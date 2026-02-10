package com.fiisadev.vs_logistics.content.fluid_pump;

import com.fiisadev.vs_logistics.config.LogisticsCommonConfig;
import com.fiisadev.vs_logistics.content.fluid_pump.handlers.FluidPortHandler;
import com.fiisadev.vs_logistics.content.fluid_pump.handlers.PlayerHandler;
import com.fiisadev.vs_logistics.network.BreakHosePacket;
import com.fiisadev.vs_logistics.registry.LogisticsIcons;
import com.fiisadev.vs_logistics.registry.LogisticsNetwork;
import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueBoxTransform;
import com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.INamedIconOptions;
import com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.ScrollOptionBehaviour;
import com.simibubi.create.foundation.fluid.SmartFluidTank;
import com.simibubi.create.foundation.gui.AllIcons;
import dev.engine_room.flywheel.lib.transform.TransformStack;
import net.createmod.catnip.lang.Lang;
import net.createmod.catnip.math.AngleHelper;
import net.createmod.catnip.math.VecHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.network.PacketDistributor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3d;
import org.valkyrienskies.mod.api.ValkyrienSkies;
import org.valkyrienskies.mod.common.VSGameUtilsKt;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;

public class FluidPumpBlockEntity extends SmartBlockEntity implements IHaveGoggleInformation {
    public FluidPumpBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    private static final int SYNC_RATE = 8;
    protected int syncCooldown;
    protected boolean queuedSync;

    private @Nullable IFluidPumpHandler pumpHandler;

    public @Nullable IFluidPumpHandler getPumpHandler() {
        return pumpHandler;
    }

    public void setPumpHandler(IFluidPumpHandler pumpHandler) {
        if (this.pumpHandler == null && pumpHandler == null) return;
        if (this.pumpHandler != null && this.pumpHandler.equals(pumpHandler)) return;

        if (level instanceof ServerLevel) {
            if (this.pumpHandler != null)
                this.pumpHandler.onStopUsing();

            this.pumpHandler = pumpHandler;

            if (this.pumpHandler != null)
                this.pumpHandler.onStartUsing();

            sendDataImmediately();
            setChanged();
        } else {
            this.pumpHandler = pumpHandler;
        }
    }

    private final SmartFluidTank fluidTank = new SmartFluidTank(8000, this::onFluidStackChange);
    private final LazyOptional<IFluidHandler> fluidCapability = LazyOptional.of(() -> fluidTank);

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

    private ScrollOptionBehaviour<PumpMode> pumpMode;

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
        pumpMode = new ScrollOptionBehaviour<>(PumpMode.class,
                Component.translatable("block.vs_logistics.fluid_pump_mode"), this, new PumpValueBox());
        behaviours.add(pumpMode);
    }

    public PumpMode getMode() {
        return pumpMode.get();
    }

    public Vec3 getHoseDir() {
        BlockPos blockPos = getBlockPos();
        Direction facing = getBlockState().getValue(FluidPumpBlock.FACING);

        Vec3 dir = Vec3.atLowerCornerOf(facing.getNormal());

        var ship = VSGameUtilsKt.getShipManagingPos(getLevel(), blockPos);
        if (ship != null) {
            Vector3d worldDir = ship.getTransform().getShipToWorldRotation().transform(new Vector3d(dir.x, dir.y, dir.z));
            return new Vec3(worldDir.x, worldDir.y, worldDir.z).normalize();
        }

        return dir.normalize();
    }

    public Direction getFacing() {
        return getBlockState().getValue(FluidPumpBlock.FACING);
    }

    public Vec3 getDirection() {
        return Vec3.atLowerCornerOf(getFacing().getNormal());
    }

    public Vec3 getHoseStart() {
        return Vec3.atLowerCornerOf(getFacing().getNormal()).scale(0.5f).add(0.5, 5 / 16f, 0.5);
    }

    public Vec3 getHoseStartWorld() {
        Vec3 offset = getHoseStart();
        Vec3 pos = Vec3.atLowerCornerOf(getBlockPos()).add(offset);
        if (ValkyrienSkies.getShipManagingBlock(level, getBlockPos()) != null)
            return ValkyrienSkies.positionToWorld(level, pos);
        return pos;
    }

    public void breakHose() {
        if (level instanceof ServerLevel) {
            if (pumpHandler == null)
                return;

            LogisticsNetwork.CHANNEL.send(
                PacketDistributor.TRACKING_CHUNK.with(() -> level.getChunkAt(getBlockPos())),
                new BreakHosePacket(
                    getHoseStartWorld(),
                    pumpHandler.getHoseEnd(0),
                    getHoseDir(),
                    pumpHandler.getHoseDir(0)
                )
            );

            setPumpHandler(null);
        }
    }

    public SmartFluidTank getFluidTank() {
        return fluidTank;
    }

    public boolean isDisabled() {
        return level.hasNeighborSignal(getBlockPos());
    }

    public void pushFluid(@NotNull IFluidHandler dest) {
        FluidStack simulatedExtract = fluidTank.drain(LogisticsCommonConfig.PUMP_RATE.get(), IFluidHandler.FluidAction.SIMULATE);
        if (simulatedExtract.isEmpty()) return;

        int accepted = dest.fill(simulatedExtract, IFluidHandler.FluidAction.SIMULATE);
        if (accepted <= 0) return;

        FluidStack realExtract = fluidTank.drain(accepted, IFluidHandler.FluidAction.EXECUTE);
        if (realExtract.isEmpty()) return;

        dest.fill(realExtract, IFluidHandler.FluidAction.EXECUTE);
    }

    public void pullFluid(@NotNull IFluidHandler source) {
        FluidStack simulatedExtract = source.drain(LogisticsCommonConfig.PUMP_RATE.get(), IFluidHandler.FluidAction.SIMULATE);
        if (simulatedExtract.isEmpty()) return;

        int accepted = fluidTank.fill(simulatedExtract, IFluidHandler.FluidAction.SIMULATE);
        if (accepted <= 0) return;

        FluidStack realExtract = source.drain(accepted, IFluidHandler.FluidAction.EXECUTE);
        if (realExtract.isEmpty()) return;

        fluidTank.fill(realExtract, IFluidHandler.FluidAction.EXECUTE);
    }

    @Override
    public void tick() {
        super.tick();

        if (syncCooldown > 0) {
            syncCooldown--;
            if (syncCooldown == 0 && queuedSync)
                sendData();
        }

        if (level == null || level.isClientSide) return;
        if (pumpHandler == null) return;

        pumpHandler.tick();

        if (pumpHandler == null) return;

        if (isDisabled())
            return;

        switch (getMode()) {
            case SUCTION -> pumpHandler.pullFluid();
            case DISCHARGE -> pumpHandler.pushFluid();
        }
    }

    @Override
    protected void write(CompoundTag tag, boolean clientPacket) {
        super.write(tag, clientPacket);

        tag.put("FluidStack", fluidTank.writeToNBT(new CompoundTag()));

        if (pumpHandler != null) {
            pumpHandler.write(tag);
        }
    }

    @Override
    protected void read(CompoundTag tag, boolean clientPacket) {
        super.read(tag, clientPacket);

        fluidTank.readFromNBT(tag.getCompound("FluidStack"));

        if (tag.contains("UserType")) {
            switch (tag.getString("UserType")) {
                case "PLAYER":
                    setPumpHandler(new PlayerHandler(this, UUID.fromString(tag.getString("UserId"))));
                    break;
                case "FLUID_PORT":
                    setPumpHandler(new FluidPortHandler(this, BlockPos.of(Long.parseLong(tag.getString("UserId")))));
                    break;
                default:
                    break;
            }
        } else {
            setPumpHandler(null);
        }
    }

    public void sendDataImmediately() {
        syncCooldown = 0;
        queuedSync = false;
        sendData();
    }

    @Override
    public void sendData() {
        if (syncCooldown > 0) {
            queuedSync = true;
            return;
        }
        super.sendData();
        queuedSync = false;
        syncCooldown = SYNC_RATE;
    }

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        if (level == null) return false;
        return containedFluidTooltip(tooltip, isPlayerSneaking, fluidCapability);
    }

    static class PumpValueBox extends ValueBoxTransform.Sided {

        @Override
        protected Vec3 getSouthLocation() {
            return VecHelper.voxelSpace(8, 8, 14.5);
        }

        @Override
        public Vec3 getLocalOffset(LevelAccessor level, BlockPos pos, BlockState state) {
            Direction facing = state.getValue(FluidPumpBlock.FACING);
            return super.getLocalOffset(level, pos, state).add(Vec3.atLowerCornerOf(facing.getNormal())
                    .scale(-1 / 16f));
        }

        @Override
        public void rotate(LevelAccessor level, BlockPos pos, BlockState state, PoseStack ms) {
            super.rotate(level, pos, state, ms);
            Direction facing = state.getValue(FluidPumpBlock.FACING);
            if (facing.getAxis() == Direction.Axis.Y)
                return;
            if (getSide() != Direction.UP)
                return;
            TransformStack.of(ms)
                    .rotateZDegrees(-AngleHelper.horizontalAngle(facing) + 180);
        }

        @Override
        protected boolean isSideActive(BlockState state, Direction direction) {
            return state.getValue(FluidPumpBlock.FACING).getOpposite() == direction;
        }
    }

    public enum PumpMode implements INamedIconOptions {
        DISCHARGE(LogisticsIcons.I_DISCHARGE),
        SUCTION(LogisticsIcons.I_SUCTION);

        private final String translationKey;
        private final AllIcons icon;

        PumpMode(AllIcons icon) {
            this.icon = icon;
            translationKey = "block.vs_logistics.pump_mode." + Lang.asId(name());
        }

        @Override
        public AllIcons getIcon() {
            return icon;
        }

        @Override
        public String getTranslationKey() {
            return translationKey;
        }
    }

    public static void withBlockEntityDo(BlockGetter level, BlockPos pos, Consumer<FluidPumpBlockEntity> action) {
        if (pos == null || level == null) return;
        Optional.ofNullable(level.getBlockEntity(pos)).ifPresent((be) -> {
            if (be instanceof FluidPumpBlockEntity fluidPump) {
                action.accept(fluidPump);
            }
        });
    }
}
