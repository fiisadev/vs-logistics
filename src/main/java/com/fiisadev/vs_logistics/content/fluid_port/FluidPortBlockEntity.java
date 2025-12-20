package com.fiisadev.vs_logistics.content.fluid_port;

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
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
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

import java.util.*;

public class FluidPortBlockEntity extends SmartBlockEntity implements IHaveGoggleInformation {
    private Set<BlockPos> targetSet = new HashSet<>();

    private FluidPortFluidHandler tankInventory;
    private LazyOptional<IFluidHandler> fluidCapability = LazyOptional.empty();

    private List<FluidPortFluidHandler.AggregatedFluid> cachedFluids = new ArrayList<>();

    private BlockPos fluidPumpPos;

    public FluidPortBlockEntity(BlockEntityType<?> pType, BlockPos pPos, BlockState pBlockState) {
        super(pType, pPos, pBlockState);
        setLazyTickRate(20);
    }

    public BlockPos getFluidPumpPos() {
        return fluidPumpPos;
    }

    public void setFluidPumpPos(BlockPos fluidPumpPos) {
        this.fluidPumpPos = fluidPumpPos;
        notifyUpdate();
    }

    public Set<BlockPos> getTargets() {
        return Set.copyOf(targetSet);
    }

    public void addTarget(BlockPos pos) {
        targetSet.add(pos);
        notifyUpdate();
    }

    public void removeTarget(BlockPos pos) {
        targetSet.remove(pos);
        notifyUpdate();
    }

    public IFluidHandler getFluidHandler() {
        return tankInventory;
    }

    @Override
    protected void read(CompoundTag tag, boolean clientPacket) {
        super.read(tag, clientPacket);

        if (tag.contains("FluidPumpPos"))
            fluidPumpPos = BlockPos.of(tag.getLong("FluidPumpPos"));
        else
            fluidPumpPos = null;

        targetSet = new HashSet<>();
        for (long pos : tag.getLongArray("TargetSet")) {
            targetSet.add(BlockPos.of(pos));
        }

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
            tag.putLong("FluidPumpPos", fluidPumpPos.asLong());

        tag.putLongArray("TargetSet", targetSet.stream().map(BlockPos::asLong).toList());

        ListTag list = new ListTag();
        for (int i = 0; i < tankInventory.getTanks(); i++) {
            CompoundTag t = new CompoundTag();
            tankInventory.getFluidInTank(i).writeToNBT(t);
            t.putInt("Capacity", tankInventory.getTankCapacity(i));
            list.add(t);
        }
        tag.put("Fluids", list);
    }

    @Override
    public void onLoad() {
        super.onLoad();

        if (fluidCapability == null || !fluidCapability.isPresent()) {
            tankInventory = new FluidPortFluidHandler(targetSet, level, this::onFluidStackChanged);
            fluidCapability = LazyOptional.of(() -> tankInventory);
        }
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
    public void invalidateCaps() {
        super.invalidateCaps();
        fluidCapability.invalidate();
    }

    @Override
    public void lazyTick() {
        // Update every 20 ticks to update goggle tooltip
        if (level == null || level.isClientSide)
            return;

        notifyUpdate();
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
