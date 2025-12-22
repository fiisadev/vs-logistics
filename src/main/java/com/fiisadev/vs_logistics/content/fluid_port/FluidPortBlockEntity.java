package com.fiisadev.vs_logistics.content.fluid_port;

import com.fiisadev.vs_logistics.content.fluid_pump.FluidPumpBlockEntity;
import com.fiisadev.vs_logistics.registry.LogisticsBlocks;
import com.fiisadev.vs_logistics.managers.JointManager;
import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.utility.CreateLang;
import net.createmod.catnip.lang.LangBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
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
import org.valkyrienskies.core.api.ships.ServerShip;
import org.valkyrienskies.mod.common.VSGameUtilsKt;

import java.util.*;

public class FluidPortBlockEntity extends SmartBlockEntity implements IHaveGoggleInformation {
    private final Set<BlockPos> targetSet = new HashSet<>();

    @Nullable
    private FluidPortFluidHandler fluidHandler;

    private LazyOptional<IFluidHandler> fluidCapability = LazyOptional.empty();

    private final List<FluidPortFluidHandler.AggregatedFluid> cachedFluids = new ArrayList<>();

    @Nullable
    private BlockPos fluidPumpPos;

    private static final int SYNC_RATE = 8;
    protected int syncCooldown;
    protected boolean queuedSync;

    public FluidPortBlockEntity(BlockEntityType<?> pType, BlockPos pPos, BlockState pBlockState) {
        super(pType, pPos, pBlockState);
    }

    public @Nullable BlockPos getFluidPumpPos() {
        return fluidPumpPos;
    }

    public void setFluidPumpPos(BlockPos fluidPumpPos) {
        this.fluidPumpPos = fluidPumpPos;
        sendDataImmediately();
    }

    public Set<BlockPos> getTargets() {
        return Set.copyOf(targetSet);
    }

    public boolean isValidTarget(BlockPos pos) {
        if (!(level instanceof ServerLevel serverLevel) || pos == null) return false;
        BlockEntity be = level.getBlockEntity(pos);
        if (be == null) return false;
        if (be.getBlockState().is(LogisticsBlocks.FLUID_PORT.get())) return false;
        if (!be.getCapability(ForgeCapabilities.FLUID_HANDLER).isPresent()) return false;

        ServerShip shipA = VSGameUtilsKt.getShipManagingPos(serverLevel, getBlockPos());
        ServerShip shipB = VSGameUtilsKt.getShipManagingPos(serverLevel, pos);
        if (shipA == null || shipB == null) return false;
        if (shipA.getId() == shipB.getId()) return true;
        return JointManager.isShipConnectedToShip(shipB, shipA);
    }

    public void addTarget(BlockPos pos) {
        if (!isValidTarget(pos))
            return;

        targetSet.add(pos);
        sendDataImmediately();
    }

    public void removeTarget(BlockPos pos) {
        targetSet.remove(pos);
        sendDataImmediately();
    }

    @Override
    public void tick() {
        super.tick();
        if (syncCooldown > 0) {
            syncCooldown--;
            if (syncCooldown == 0 && queuedSync)
                sendData();
        }
    }

    public @Nullable IFluidHandler getFluidHandler() {
        return fluidHandler;
    }

    @Override
    protected void read(CompoundTag tag, boolean clientPacket) {
        super.read(tag, clientPacket);

        fluidPumpPos = null;

        if (tag.contains("FluidPumpPos"))
            fluidPumpPos = NbtUtils.readBlockPos(tag.getCompound("FluidPumpPos"));

        targetSet.clear();
        ListTag targets = tag.getList("Targets", Tag.TAG_COMPOUND);
        for (Tag target : targets)
            targetSet.add(NbtUtils.readBlockPos((CompoundTag)target));

        cachedFluids.clear();
        ListTag list = tag.getList("Fluids", Tag.TAG_COMPOUND);
        for (Tag base : list) {
            CompoundTag t = (CompoundTag) base;
            FluidStack fluidStack = FluidStack.loadFluidStackFromNBT(t);
            int capacity = t.getInt("Capacity");
            cachedFluids.add(new FluidPortFluidHandler.AggregatedFluid(fluidStack, capacity));
        }
    }

    @Override
    protected void write(CompoundTag tag, boolean clientPacket) {
        super.write(tag, clientPacket);

        if (fluidPumpPos != null)
            tag.put("FluidPumpPos", NbtUtils.writeBlockPos(fluidPumpPos));

        ListTag targets = new ListTag();
        for (BlockPos target : targetSet)
            targets.add(NbtUtils.writeBlockPos(target));
        tag.put("Targets", targets);

        if (fluidHandler != null) {
            ListTag list = new ListTag();
            for (int i = 0; i < fluidHandler.getTanks(); i++) {
                CompoundTag t = new CompoundTag();
                fluidHandler.getFluidInTank(i).writeToNBT(t);
                t.putInt("Capacity", fluidHandler.getTankCapacity(i));
                list.add(t);
            }
            tag.put("Fluids", list);
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
    public void onLoad() {
        super.onLoad();

        if (fluidCapability == null || !fluidCapability.isPresent()) {
            fluidHandler = new FluidPortFluidHandler(targetSet, level, this::onFluidStackChanged);
            fluidCapability = LazyOptional.of(() -> fluidHandler);
        }
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        fluidCapability.invalidate();
    }

    private void onFluidStackChanged() {
        if (level != null && !level.isClientSide)
            notifyUpdate();
    }

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.FLUID_HANDLER) {
            return fluidCapability.cast();
        }

        return super.getCapability(cap, side);
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) { }

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        if (level == null)
            return false;

        if (cachedFluids.isEmpty())
            return false;

        LangBuilder mb = CreateLang.translate("generic.unit.millibuckets");
        CreateLang.translate("gui.goggles.fluid_container")
                .forGoggles(tooltip);

        boolean isEmpty = true;
        for (FluidPortFluidHandler.AggregatedFluid cachedFluid : cachedFluids) {
            FluidStack fluidStack = cachedFluid.stack;
            if (fluidStack.isEmpty())
                continue;

            CreateLang.fluidName(fluidStack)
                    .style(ChatFormatting.GRAY)
                    .forGoggles(tooltip, 1);

            CreateLang.builder()
                    .add(CreateLang.number(fluidStack.getAmount())
                            .add(mb)
                            .style(ChatFormatting.GOLD))
                    .text(ChatFormatting.GRAY, " / ")
                    .add(CreateLang.number(cachedFluid.capacity)
                            .add(mb)
                            .style(ChatFormatting.DARK_GRAY))
                    .forGoggles(tooltip, 1);

            isEmpty = false;
        }

        if (cachedFluids.size() > 1) {
            if (isEmpty)
                tooltip.remove(tooltip.size() - 1);
            return true;
        }

        if (isEmpty) {
            int capacity = 0;
            for (FluidPortFluidHandler.AggregatedFluid cachedFluid : cachedFluids)
                capacity += cachedFluid.capacity;

            CreateLang.translate("gui.goggles.fluid_container.capacity")
                    .add(CreateLang.number(capacity)
                            .add(mb)
                            .style(ChatFormatting.GOLD))
                    .style(ChatFormatting.GRAY)
                    .forGoggles(tooltip, 1);
        }

        return true;
    }
}
