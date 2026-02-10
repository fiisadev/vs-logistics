package com.fiisadev.vs_logistics.content.fluid_port;

import com.fiisadev.vs_logistics.config.LogisticsCommonConfig;
import com.fiisadev.vs_logistics.content.fluid_pump.FluidPumpBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.function.Supplier;

public class FluidPortFluidHandler implements IFluidHandler {
    private final Level level;
    private final Map<BlockPos, FluidPortTarget> targetMap;
    private final Runnable updateCallback;
    private final Supplier<BlockPos> fluidPumpSup;

    public FluidPortFluidHandler(Map<BlockPos, FluidPortTarget> targetMap, Level level, Runnable updateCallback, Supplier<BlockPos> fluidPumpSup) {
        this.targetMap = targetMap;
        this.level = level;
        this.updateCallback = updateCallback;
        this.fluidPumpSup = fluidPumpSup;
    }

    private List<IFluidHandler> getHandlers() {
        List<IFluidHandler> handlers = new ArrayList<>();

        for (FluidPortTarget target : targetMap.values()) {
            BlockEntity be = level.getBlockEntity(target.getPos());
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

        BlockPos pumpPos = fluidPumpSup.get();
        FluidPumpBlockEntity pump = null;
        int currentRateLimit = Integer.MAX_VALUE;

        if (pumpPos != null && level.getBlockEntity(pumpPos) instanceof FluidPumpBlockEntity p) {
            if (p.isDisabled()) return 0;
            pump = p;
            currentRateLimit = LogisticsCommonConfig.PUMP_RATE.get();
        }

        int maxToFill = Math.min(resource.getAmount(), currentRateLimit);
        if (maxToFill <= 0) return 0;

        int remaining = maxToFill;
        int filledTotal = 0;

        if (pump != null) {
            IFluidHandler pumpHandler = pump.getFluidTank();

            FluidStack toFillPump = resource.copy();
            toFillPump.setAmount(remaining);

            int filledInPump = pumpHandler.fill(toFillPump, action);
            filledTotal += filledInPump;
            remaining -= filledInPump;
        }

        if (remaining > 0) {
            for (IFluidHandler handler : getHandlers()) {
                if (remaining <= 0) break;

                FluidStack toFillNetwork = resource.copy();
                toFillNetwork.setAmount(remaining);

                int filled = handler.fill(toFillNetwork, action);
                if (filled > 0) {
                    filledTotal += filled;
                    remaining -= filled;
                }
            }
        }

        if (filledTotal > 0 && action.execute()) {
            updateCallback.run();
        }

        return filledTotal;
    }

    @Override
    public @NotNull FluidStack drain(FluidStack resource, FluidAction action) {
        if (resource.isEmpty()) return FluidStack.EMPTY;

        BlockPos pumpPos = fluidPumpSup.get();
        FluidPumpBlockEntity pump = null;
        int currentRateLimit = Integer.MAX_VALUE;

        if (pumpPos != null && level.getBlockEntity(pumpPos) instanceof FluidPumpBlockEntity p) {
            if (p.isDisabled()) return FluidStack.EMPTY;
            pump = p;
            currentRateLimit = LogisticsCommonConfig.PUMP_RATE.get();
        }

        int maxToDrain = Math.min(resource.getAmount(), currentRateLimit);
        if (maxToDrain <= 0) return FluidStack.EMPTY;

        int remaining = maxToDrain;
        FluidStack drainedTotal = FluidStack.EMPTY;

        if (pump != null) {
            IFluidHandler pumpHandler = pump.getFluidTank();

            FluidStack pumpRequest = resource.copy();
            pumpRequest.setAmount(remaining);

            FluidStack fromPump = pumpHandler.drain(pumpRequest, action);
            if (!fromPump.isEmpty()) {
                drainedTotal = fromPump.copy();
                remaining -= fromPump.getAmount();
            }
        }

        if (remaining > 0) {
            for (IFluidHandler handler : getHandlers()) {
                if (remaining <= 0) break;

                FluidStack networkRequest = resource.copy();
                networkRequest.setAmount(remaining);

                FluidStack drained = handler.drain(networkRequest, action);
                if (drained.isEmpty()) continue;

                if (drainedTotal.isEmpty()) {
                    drainedTotal = drained.copy();
                } else {
                    drainedTotal.grow(drained.getAmount());
                }

                remaining -= drained.getAmount();
            }
        }

        if (!drainedTotal.isEmpty() && action.execute()) {
            updateCallback.run();
        }

        return drainedTotal;
    }

    @Override
    public @NotNull FluidStack drain(int maxDrain, FluidAction action) {
        if (maxDrain <= 0) return FluidStack.EMPTY;

        int remaining = maxDrain;
        FluidStack drainedTotal = FluidStack.EMPTY;

        if (fluidPumpSup.get() != null) {
            if (level.getBlockEntity(fluidPumpSup.get()) instanceof FluidPumpBlockEntity fluidPump) {
                if (fluidPump.isDisabled()) return FluidStack.EMPTY; // Pump is off, no flow allowed

                remaining = Math.min(remaining, LogisticsCommonConfig.PUMP_RATE.get());
            }

            if (level.getBlockEntity(fluidPumpSup.get()) instanceof FluidPumpBlockEntity fluidPump) {
                IFluidHandler pumpHandler = fluidPump.getFluidTank();
                FluidStack fromPump = pumpHandler.drain(remaining, action);

                if (!fromPump.isEmpty()) {
                    drainedTotal = fromPump.copy();
                    remaining -= fromPump.getAmount();
                }
            }
        }

        if (remaining > 0) {
            for (IFluidHandler handler : getHandlers()) {
                if (remaining <= 0) break;

                FluidStack drained;
                if (drainedTotal.isEmpty()) {
                    drained = handler.drain(remaining, action);
                } else {
                    FluidStack filter = drainedTotal.copy();
                    filter.setAmount(remaining);
                    drained = handler.drain(filter, action);
                }

                if (!drained.isEmpty()) {
                    if (drainedTotal.isEmpty()) {
                        drainedTotal = drained.copy();
                    } else {
                        drainedTotal.grow(drained.getAmount());
                    }
                    remaining -= drained.getAmount();
                }
            }
        }

        if (!drainedTotal.isEmpty() && action.execute()) {
            updateCallback.run();
        }

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
