package com.fiisadev.vs_logistics.content.fluid_port;

import com.fiisadev.vs_logistics.config.LogisticsCommonConfig;
import com.fiisadev.vs_logistics.registry.LogisticsBlocks;
import com.fiisadev.vs_logistics.managers.JointManager;
import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import com.simibubi.create.foundation.blockEntity.IMultiBlockEntityContainer;
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
import net.minecraft.world.level.material.Fluid;
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
    private final Map<BlockPos, FluidPortTarget> targetMap = new HashMap<>();

    @Nullable
    private FluidPortFluidHandler fluidHandler;

    private LazyOptional<IFluidHandler> fluidCapability = LazyOptional.empty();

    private final List<FluidPortFluidHandler.AggregatedFluid> cachedFluids = new ArrayList<>();

    @Nullable
    private BlockPos fluidPumpPos;

    private static final int SYNC_RATE = 8;
    private static final int LAZY_TICK_RATE = 4;
    protected int syncCooldown;
    protected boolean queuedSync;


    public FluidPortBlockEntity(BlockEntityType<?> pType, BlockPos pPos, BlockState pBlockState) {
        super(pType, pPos, pBlockState);
        setLazyTickRate(LAZY_TICK_RATE);
    }

    public @Nullable BlockPos getFluidPumpPos() {
        return fluidPumpPos;
    }

    public void setFluidPumpPos(BlockPos fluidPumpPos) {
        this.fluidPumpPos = fluidPumpPos;
        sendDataImmediately();
    }

    public Map<BlockPos, FluidPortTarget> getTargets() {
        return targetMap;
    }

    public boolean isValidTarget(BlockPos pos) {
        if (!(level instanceof ServerLevel serverLevel) || pos == null) return false;
        BlockEntity be = level.getBlockEntity(pos);
        if (be == null) return false;
        if (be.getBlockState().is(LogisticsBlocks.FLUID_PORT.get())) return false;
        if (!be.getCapability(ForgeCapabilities.FLUID_HANDLER).isPresent()) return false;
        if (be instanceof IMultiBlockEntityContainer.Fluid multiBlock)
            if (!multiBlock.isController()) return false;

        ServerShip shipA = VSGameUtilsKt.getShipManagingPos(serverLevel, getBlockPos());
        ServerShip shipB = VSGameUtilsKt.getShipManagingPos(serverLevel, pos);
        if (shipA == null || shipB == null) return false;
        if (shipA.getId() != shipB.getId() || !JointManager.isShipConnectedToShip(shipB, shipA)) return false;
        return VSGameUtilsKt.toWorldCoordinates(serverLevel, getBlockPos()).distanceToSqr(VSGameUtilsKt.toWorldCoordinates(serverLevel, pos)) <= Math.pow(LogisticsCommonConfig.MAX_FLUID_PORT_LINK_DISTANCE.get(), 2);
    }

    public void addTarget(BlockPos pos) {
        if (!isValidTarget(pos))
            return;

        targetMap.put(pos, new FluidPortTarget(pos));
    }

    private List<FluidPortTarget> getTargetsByMode(FluidPortTarget.Mode mode) {
        List<FluidPortTarget> list = new ArrayList<>();
        for (FluidPortTarget target : targetMap.values())
            if (target.getMode() == mode)
                list.add(target);
        return list;
    }

    private void distributeBetweenGroups(List<FluidPortTarget> sources, List<FluidPortTarget> destinations) {
        for (FluidPortTarget dest : destinations) {
            dest.getFluidHandler(level).ifPresent(destHandler -> {
                for (FluidPortTarget src : sources) {
                    src.getFluidHandler(level).ifPresent(srcHandler -> {
                        for (int i = 0; i < srcHandler.getTanks(); i++) {
                            FluidStack srcFluid = srcHandler.getFluidInTank(i);
                            if (srcFluid.isEmpty()) continue;

                            FluidStack toDrain = srcHandler.drain(srcFluid, IFluidHandler.FluidAction.SIMULATE);
                            if (toDrain.isEmpty()) continue;

                            int toFill = destHandler.fill(toDrain, IFluidHandler.FluidAction.SIMULATE);
                            if (toFill <= 0) continue;

                            FluidStack drained = srcHandler.drain(new FluidStack(toDrain.getFluid(), Math.min(toFill, LogisticsCommonConfig.PUMP_RATE.get() * LAZY_TICK_RATE)), IFluidHandler.FluidAction.EXECUTE);
                            if (!drained.isEmpty()) {
                                destHandler.fill(drained, IFluidHandler.FluidAction.EXECUTE);
                                sendData();
                            }
                        }
                    });
                }
            });
        }
    }

    private void equalizeTanks(List<FluidPortTarget> targets) {
        List<IFluidHandler> handlers = targets.stream()
                .map(t -> t.getFluidHandler(level))
                .flatMap(opt -> opt.resolve().stream())
                .toList();

        if (handlers.isEmpty()) return;

        List<FluidTankSlot> allSlots = new ArrayList<>();
        for (IFluidHandler handler : handlers) {
            for (int i = 0; i < handler.getTanks(); i++) {
                allSlots.add(new FluidTankSlot(handler, i));
            }
        }

        Map<Fluid, Integer> totalAmount = new HashMap<>();
        Map<Fluid, List<FluidTankSlot>> fluidSlots = new HashMap<>();
        for (FluidTankSlot slot : allSlots) {
            FluidStack stack = slot.getFluid();
            if (!stack.isEmpty()) {
                Fluid fluid = stack.getFluid();
                totalAmount.merge(fluid, stack.getAmount(), Integer::sum);
                fluidSlots.computeIfAbsent(fluid, k -> new ArrayList<>()).add(slot);
            }
        }

        for (Fluid fluid : fluidSlots.keySet()) {
            List<FluidTankSlot> compatible = allSlots.stream()
                    .filter(slot -> {
                        FluidStack fs = slot.getFluid();
                        return fs.getFluid() == fluid || (fs.isEmpty() && slot.canAccept(fluid));
                    })
                    .sorted(Comparator
                            .comparingInt(slot -> System.identityHashCode(((FluidTankSlot)slot).handler))
                            .thenComparingInt(slot -> ((FluidTankSlot)slot).tankIndex))
                    .toList();

            if (compatible.isEmpty()) continue;

            int drainedTotal = 0;
            for (FluidTankSlot slot : allSlots) {
                FluidStack fs = slot.getFluid();
                if (fs.getFluid() == fluid) {
                    FluidStack drained = slot.handler.drain(
                            new FluidStack(fluid, fs.getAmount()),
                            IFluidHandler.FluidAction.EXECUTE
                    );
                    drainedTotal += drained.getAmount();
                }
            }
            if (drainedTotal <= 0) continue;

            int n = compatible.size();
            int average = drainedTotal / n;
            int remainder = drainedTotal % n;

            for (FluidTankSlot slot : compatible) {
                int capacity = slot.handler.getTankCapacity(slot.tankIndex);
                int amount = Math.min(average + (remainder-- > 0 ? 1 : 0), capacity);
                if (amount > 0) {
                    slot.handler.fill(new FluidStack(fluid, amount), IFluidHandler.FluidAction.EXECUTE);
                }
            }
        }
    }

    private static class FluidTankSlot {
        final IFluidHandler handler;
        final int tankIndex;

        FluidTankSlot(IFluidHandler handler, int tankIndex) {
            this.handler = handler;
            this.tankIndex = tankIndex;
        }

        FluidStack getFluid() {
            return handler.getFluidInTank(tankIndex);
        }

        boolean canAccept(Fluid fluid) {
            return handler.isFluidValid(tankIndex, new FluidStack(fluid, 1));
        }
    }

    private void distributeFluids() {
        if (level == null || level.isClientSide)
            return;

        if (getTargets().isEmpty())
            return;

        List<FluidPortTarget> pullList = getTargetsByMode(FluidPortTarget.Mode.PULL);
        List<FluidPortTarget> pushList = getTargetsByMode(FluidPortTarget.Mode.PUSH);
        List<FluidPortTarget> equalizeList = getTargetsByMode(FluidPortTarget.Mode.EQUALIZE);

        List<FluidPortTarget> merged = new ArrayList<>();
        merged.addAll(pullList);
        merged.addAll(equalizeList);

        distributeBetweenGroups(merged, pushList);
        distributeBetweenGroups(pullList, equalizeList);
        equalizeTanks(equalizeList);
    }

    private void removeInvalidTargets() {
        if (level == null || level.isClientSide)
            return;

        List<BlockPos> toRemove = new ArrayList<>();

        for (FluidPortTarget target : targetMap.values()) {
            if (!isValidTarget(target.getPos())) {
                toRemove.add(target.getPos());
            }
        }

        for (BlockPos pos : toRemove) {
            targetMap.remove(pos);
            sendData();
        }
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

    @Override
    public void lazyTick() {
        super.lazyTick();
        removeInvalidTargets();
        distributeFluids();
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

        targetMap.clear();
        for (Tag base : tag.getList("Targets", Tag.TAG_COMPOUND)) {
            CompoundTag t = (CompoundTag)base;
            BlockPos pos = NbtUtils.readBlockPos(t.getCompound("Pos"));
            FluidPortTarget.Mode mode = FluidPortTarget.MODES[t.getInt("Mode")];
            targetMap.put(pos, new FluidPortTarget(pos, mode));
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
            tag.put("FluidPumpPos", NbtUtils.writeBlockPos(fluidPumpPos));

        ListTag targets = new ListTag();
        for (FluidPortTarget target : targetMap.values()) {
            CompoundTag t = new CompoundTag();
            t.put("Pos", NbtUtils.writeBlockPos(target.getPos()));
            t.putInt("Mode", target.getMode().ordinal());
            targets.add(t);
        }
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
            fluidHandler = new FluidPortFluidHandler(targetMap, level, this::onFluidStackChanged);
            fluidCapability = LazyOptional.of(() -> fluidHandler);
        }
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        fluidCapability.invalidate();
    }

    private void onFluidStackChanged() {
        if (level != null && !level.isClientSide) {
            setChanged();
            sendData();
        }
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
