package com.fiisadev.vs_logistics.content.fluid_port;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class FluidPortFluidHandler implements IFluidHandler {
    private final Level level;
    private final Set<BlockPos> targetSet;
    private Runnable updateCallback;

    public FluidPortFluidHandler(Set<BlockPos> targetSet, Level level, Runnable updateCallback) {
        this.targetSet = targetSet;
        this.level = level;
        this.updateCallback = updateCallback;
    }

    private List<IFluidHandler> getHandlers() {
        List<IFluidHandler> handlers = new ArrayList<>();

        for (BlockPos target : targetSet) {
            BlockEntity be = level.getBlockEntity(target);
            if (be == null) continue;
            be.getCapability(ForgeCapabilities.FLUID_HANDLER).ifPresent(handlers::add);
        }

        return handlers;
    }

    private List<AggregatedFluid> buildAggregatedFluids() {
        Map<FluidKey, AggregatedFluid> map = new LinkedHashMap<>();

        for (IFluidHandler handler : getHandlers()) {
            for (int tank = 0; tank < handler.getTanks(); tank++) {
                FluidStack stack = handler.getFluidInTank(tank);
                int capacity = handler.getTankCapacity(tank);

                FluidKey key = stack.isEmpty()
                        ? null
                        : new FluidKey(stack);

                if (key == null) {
                    map.computeIfAbsent(
                            FluidKey.EMPTY,
                            k -> new AggregatedFluid(FluidStack.EMPTY, 0)
                    ).capacity += capacity;
                    continue;
                }

                FluidStack base = stack.copy();
                base.setAmount(0);

                AggregatedFluid aggregated = map.computeIfAbsent(
                        key,
                        k -> new AggregatedFluid(base, 0)
                );

                aggregated.stack.grow(stack.getAmount());
                aggregated.capacity += capacity;
            }
        }

        if (map.size() > 1) {
            map.remove(FluidKey.EMPTY);
        }

        return new ArrayList<>(map.values());
    }

    @Override
    public int getTanks() {
        return buildAggregatedFluids().size();
    }

    @Override
    public @NotNull FluidStack getFluidInTank(int tank) {
        List<AggregatedFluid> fluids = buildAggregatedFluids();
        if (tank < 0 || tank >= fluids.size()) return FluidStack.EMPTY;
        return fluids.get(tank).stack.copy();
    }

    @Override
    public int getTankCapacity(int tank) {
        List<AggregatedFluid> fluids = buildAggregatedFluids();
        if (tank < 0 || tank >= fluids.size()) return 0;
        return fluids.get(tank).capacity;
    }

    @Override
    public boolean isFluidValid(int tank, @NotNull FluidStack stack) {
        List<AggregatedFluid> fluids = buildAggregatedFluids();
        if (tank < 0 || tank >= fluids.size()) return false;

        FluidStack existing = fluids.get(tank).stack;
        return existing.isEmpty() || existing.isFluidEqual(stack);
    }

    @Override
    public int fill(FluidStack resource, FluidAction action) {
        if (resource.isEmpty()) return 0;

        int remaining = resource.getAmount();
        int filledTotal = 0;

        for (IFluidHandler handler : getHandlers()) {
            for (int tank = 0; tank < handler.getTanks(); tank++) {
                if (remaining <= 0) break;

                FluidStack existing = handler.getFluidInTank(tank);

                if (!existing.isEmpty() && !existing.isFluidEqual(resource)) {
                    continue;
                }

                FluidStack toFill = resource.copy();
                toFill.setAmount(remaining);

                int filled = handler.fill(toFill, action);
                if (filled > 0) {
                    remaining -= filled;
                    filledTotal += filled;
                }
            }
        }

        if (filledTotal > 0)
            updateCallback.run();

        return filledTotal;
    }

    @Override
    public @NotNull FluidStack drain(FluidStack resource, FluidAction action) {
        if (resource.isEmpty()) return FluidStack.EMPTY;

        int remaining = resource.getAmount();
        FluidStack drainedTotal = FluidStack.EMPTY;

        for (IFluidHandler handler : getHandlers()) {
            for (int tank = 0; tank < handler.getTanks(); tank++) {
                if (remaining <= 0) break;

                FluidStack existing = handler.getFluidInTank(tank);
                if (!existing.isFluidEqual(resource)) continue;

                FluidStack toDrain = resource.copy();
                toDrain.setAmount(remaining);

                FluidStack drained = handler.drain(toDrain, action);
                if (drained.isEmpty()) continue;

                if (drainedTotal.isEmpty()) {
                    drainedTotal = drained.copy();
                } else {
                    drainedTotal.grow(drained.getAmount());
                }

                remaining -= drained.getAmount();
            }
        }

        if (drainedTotal.getAmount() > 0)
            updateCallback.run();

        return drainedTotal;
    }

    @Override
    public @NotNull FluidStack drain(int maxDrain, FluidAction action) {
        if (maxDrain <= 0) return FluidStack.EMPTY;

        FluidStack drainedTotal = FluidStack.EMPTY;
        int remaining = maxDrain;

        for (IFluidHandler handler : getHandlers()) {
            for (int tank = 0; tank < handler.getTanks(); tank++) {
                if (remaining <= 0) break;

                FluidStack existing = handler.getFluidInTank(tank);
                if (existing.isEmpty()) continue;

                if (!drainedTotal.isEmpty() && !existing.isFluidEqual(drainedTotal)) {
                    continue;
                }

                FluidStack drained = handler.drain(remaining, action);
                if (drained.isEmpty()) continue;

                if (drainedTotal.isEmpty()) {
                    drainedTotal = drained.copy();
                } else {
                    drainedTotal.grow(drained.getAmount());
                }

                remaining -= drained.getAmount();
            }
        }

        if (drainedTotal.getAmount() > 0)
            updateCallback.run();

        return drainedTotal;
    }


    public static class AggregatedFluid {
        FluidStack stack;
        int capacity;

        AggregatedFluid(FluidStack stack, int capacity) {
            this.stack = stack;
            this.capacity = capacity;
        }
    }

    private record FluidKey(FluidStack stack) {
        static final FluidKey EMPTY = new FluidKey(FluidStack.EMPTY);

        private FluidKey(FluidStack stack) {
            this.stack = stack.copy();
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (!(obj instanceof FluidKey other)) return false;
            return stack.isFluidEqual(other.stack)
                    && Objects.equals(stack.getTag(), other.stack.getTag());
        }

        @Override
        public int hashCode() {
            return Objects.hash(
                    stack.getFluid(),
                    stack.getTag()
            );
        }
    }
}
