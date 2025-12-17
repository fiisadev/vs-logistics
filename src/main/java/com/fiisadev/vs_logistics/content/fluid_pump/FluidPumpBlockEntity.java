package com.fiisadev.vs_logistics.content.fluid_pump;

import com.fiisadev.vs_logistics.client.utils.HoseUtils;
import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueBoxTransform;
import com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.ScrollOptionBehaviour;
import com.simibubi.create.foundation.fluid.SmartFluidTank;
import dev.engine_room.flywheel.lib.transform.TransformStack;
import net.createmod.catnip.data.Pair;
import net.createmod.catnip.math.AngleHelper;
import net.createmod.catnip.math.VecHelper;
import net.createmod.catnip.outliner.Outliner;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fml.DistExecutor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

public class FluidPumpBlockEntity extends SmartBlockEntity {
    public FluidPumpBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    private @Nullable IUserInfo userInfo;

    public @Nullable IUserInfo getUserInfo() {
        return userInfo;
    }

    public void setUserInfo(IUserInfo userInfo) {
        if (this.userInfo != null)
            this.userInfo.onStopUsing();

        this.userInfo = userInfo;

        if (this.userInfo != null)
            this.userInfo.onStartUsing();

        notifyUpdate();
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

    public void breakHose() {
        if (level == null) return;

        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
            if (userInfo == null)
                return;

            Direction facing = getBlockState().getValue(FluidPumpBlock.FACING);

            Vec3 center = getBlockPos().getCenter();
            Vec3 pumpPos = center.relative(facing, 0.5).add(0, -0.5 + 5 / 16f, 0);
            Vec3 userPos = userInfo.getHoseEnd(Minecraft.getInstance().getPartialTick());

            double dist = pumpPos.distanceTo(userPos);

            Vec3 p1 = pumpPos.add(new Vec3(facing.getStepX(), facing.getStepY(), facing.getStepZ()).scale(dist * 0.3f));
            Vec3 p2 = userPos.subtract(userInfo.getHoseDir(Minecraft.getInstance().getPartialTick()).scale(dist * 0.3f));

            Vec3[] centers = HoseUtils.generateHoseSegments(pumpPos, userPos, p1, p2, dist);

            Minecraft.getInstance().player.playSound(SoundEvents.WOOL_PLACE, 1.0f, 1.0f);
            for (Vec3 pos : centers) {
                Minecraft.getInstance().level.addParticle(
                        new BlockParticleOption(ParticleTypes.BLOCK, Blocks.BLACK_WOOL.defaultBlockState()),
                        pos.x, pos.y, pos.z,
                        0, 0.05f, 0
                );
            }
        });

        if (!level.isClientSide) {
            setUserInfo(null);
        }
    }

    public void pushFluid(IFluidHandler dest) {
        FluidStack simulatedExtract = fluidTank.drain(60, IFluidHandler.FluidAction.SIMULATE);
        if (simulatedExtract.isEmpty()) return;

        int accepted = dest.fill(simulatedExtract, IFluidHandler.FluidAction.SIMULATE);
        if (accepted <= 0) return;

        FluidStack realExtract = fluidTank.drain(accepted, IFluidHandler.FluidAction.EXECUTE);
        if (realExtract.isEmpty()) return;

        dest.fill(realExtract, IFluidHandler.FluidAction.EXECUTE);
    }

    public void pullFluid(IFluidHandler source) {
        FluidStack simulatedExtract = source.drain(60, IFluidHandler.FluidAction.SIMULATE);
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

        if (level == null || level.isClientSide)
            return;

        if (userInfo == null)
            return;

        userInfo.tick();

        if (userInfo == null)
            return;

        if (level.hasNeighborSignal(getBlockPos()))
            return;

        switch (getMode()) {
            case PULL -> userInfo.pullFluid();
            case PUSH -> userInfo.pushFluid();
        }
    }

    @Override
    protected void write(CompoundTag tag, boolean clientPacket) {
        super.write(tag, clientPacket);

        if (userInfo != null) {
            userInfo.write(tag);
        } else {
            tag.remove("UserType");
            tag.remove("UserId");
        }
    }

    @Override
    protected void read(CompoundTag tag, boolean clientPacket) {
        super.read(tag, clientPacket);

        userInfo = null;

        if (level != null && tag.contains("UserType")) {
            switch (tag.getString("UserType")) {
                case "PLAYER":
                    userInfo = PlayerUserInfo.from(this, tag.getString("UserId"), level);
                case "NOZZLE":
                    userInfo = FluidPortUserInfo.from(this, tag.getString("UserId"), level);
            }
        }
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

    public static void withBlockEntityDo(BlockGetter level, BlockPos pos, Consumer<FluidPumpBlockEntity> action) {
        Optional.ofNullable(level.getBlockEntity(pos)).ifPresent((be) -> {
            if (be instanceof FluidPumpBlockEntity fluidPump) {
                action.accept(fluidPump);
            }
        });
    }
}
