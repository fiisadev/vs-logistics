package com.fiisadev.vs_logistics.content.fluid_pump;

import com.fiisadev.vs_logistics.client.utils.HoseUtils;
import com.fiisadev.vs_logistics.content.fluid_pump.handlers.FluidPortHandler;
import com.fiisadev.vs_logistics.content.fluid_pump.handlers.PlayerHandler;
import com.fiisadev.vs_logistics.registry.LogisticsIcons;
import com.mojang.blaze3d.vertex.PoseStack;
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

    private @Nullable IFluidPumpHandler pumpHandler;

    public @Nullable IFluidPumpHandler getPumpHandler() {
        return pumpHandler;
    }

    public void setPumpHandler(IFluidPumpHandler pumpHandler) {
        if (this.pumpHandler != null)
            this.pumpHandler.onStopUsing();

        this.pumpHandler = pumpHandler;

        if (this.pumpHandler != null)
            this.pumpHandler.onStartUsing();

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
            if (pumpHandler == null)
                return;

            Direction facing = getBlockState().getValue(FluidPumpBlock.FACING);

            Vec3 center = getBlockPos().getCenter();
            Vec3 pumpPos = center.relative(facing, 0.5).add(0, -0.5 + 5 / 16f, 0);
            Vec3 userPos = pumpHandler.getHoseEnd(Minecraft.getInstance().getPartialTick());

            double dist = pumpPos.distanceTo(userPos);

            Vec3 p1 = pumpPos.add(new Vec3(facing.getStepX(), facing.getStepY(), facing.getStepZ()).scale(dist * 0.3f));
            Vec3 p2 = userPos.subtract(pumpHandler.getHoseDir(Minecraft.getInstance().getPartialTick()).scale(dist * 0.3f));

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
            setPumpHandler(null);
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

        if (level == null || level.isClientSide) return;
        if (pumpHandler == null) return;

        pumpHandler.tick();

        if (pumpHandler == null) return;

        if (level.hasNeighborSignal(getBlockPos()))
            return;

        switch (getMode()) {
            case SUCTION -> pumpHandler.pullFluid();
            case DISCHARGE -> pumpHandler.pushFluid();
        }
    }

    @Override
    protected void write(CompoundTag tag, boolean clientPacket) {
        super.write(tag, clientPacket);

        if (pumpHandler != null) {
            pumpHandler.write(tag);
        }
    }

    @Override
    protected void read(CompoundTag tag, boolean clientPacket) {
        super.read(tag, clientPacket);

        pumpHandler = null;

        if (level != null && tag.contains("UserType")) {
            switch (tag.getString("UserType")) {
                case "PLAYER":
                    pumpHandler = PlayerHandler.from(this, tag.getString("UserId"), level);
                case "NOZZLE":
                    pumpHandler = FluidPortHandler.from(this, tag.getString("UserId"), level);
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
        if (pos == null || level == null) return;
        Optional.ofNullable(level.getBlockEntity(pos)).ifPresent((be) -> {
            if (be instanceof FluidPumpBlockEntity fluidPump) {
                action.accept(fluidPump);
            }
        });
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
}
