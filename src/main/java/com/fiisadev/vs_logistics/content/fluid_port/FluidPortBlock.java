package com.fiisadev.vs_logistics.content.fluid_port;

import com.fiisadev.vs_logistics.content.fluid_pump.handlers.FluidPortHandler;
import com.fiisadev.vs_logistics.content.fluid_pump.FluidPumpBlockEntity;
import com.fiisadev.vs_logistics.content.fluid_pump.FluidPumpPlayerDataProvider;
import com.fiisadev.vs_logistics.content.fluid_pump.handlers.PlayerHandler;
import com.fiisadev.vs_logistics.registry.LogisticsBlockEntities;
import com.simibubi.create.content.equipment.wrench.IWrenchable;
import com.simibubi.create.content.fluids.transfer.GenericItemEmptying;
import com.simibubi.create.content.fluids.transfer.GenericItemFilling;
import com.simibubi.create.foundation.block.IBE;
import com.simibubi.create.foundation.fluid.FluidHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.atomic.AtomicBoolean;

public class FluidPortBlock extends DirectionalBlock implements IWrenchable, IBE<FluidPortBlockEntity> {
    public FluidPortBlock(Properties properties) {
        super(properties);
    }

    public static boolean isValidTarget(BlockEntity be) {
        if (be == null) return false;
        if (be instanceof FluidPortBlockEntity) return false;
        return be.getCapability(ForgeCapabilities.FLUID_HANDLER).isPresent();
    }

    @Override
    public InteractionResult onWrenched(BlockState state, UseOnContext context) {
        return InteractionResult.PASS;
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!level.isClientSide) {
            AtomicBoolean shouldReturn = new AtomicBoolean();
            withBlockEntityDo(level, pos, (be) -> {
                if (!player.isShiftKeyDown())
                    return;

                player.getCapability(FluidPumpPlayerDataProvider.FLUID_PUMP_PLAYER_DATA).ifPresent((playerData) -> {
                    boolean canPickNozzle = be.getFluidPumpPos() != null && playerData.getFluidPumpPos() == null;
                    boolean canInsertNozzle = be.getFluidPumpPos() == null && playerData.getFluidPumpPos() != null;

                    if (canPickNozzle) {
                        FluidPumpBlockEntity.withBlockEntityDo(level, be.getFluidPumpPos(), (fluidPump) ->
                            fluidPump.setPumpHandler(new PlayerHandler(fluidPump, player))
                        );
                    }

                    if (canInsertNozzle) {
                        FluidPumpBlockEntity.withBlockEntityDo(level, playerData.getFluidPumpPos(), (fluidPump) ->
                            fluidPump.setPumpHandler(new FluidPortHandler(fluidPump, be))
                        );
                    }

                    shouldReturn.set(canPickNozzle || canInsertNozzle);
                });
            });

            if (shouldReturn.get()) return InteractionResult.SUCCESS;
        }

        ItemStack heldItem = player.getItemInHand(hand);
        if (!(level.getBlockEntity(pos) instanceof FluidPortBlockEntity be))
            return InteractionResult.FAIL;

        LazyOptional<IFluidHandler> cap = be.getCapability(ForgeCapabilities.FLUID_HANDLER);
        if (!cap.isPresent())
            return InteractionResult.PASS;
        IFluidHandler fluidTank = cap.resolve().orElse(null);
        if (fluidTank == null)
            return InteractionResult.FAIL;

        if (!FluidHelper.tryEmptyItemIntoBE(level, player, hand, heldItem, be)) {
            if (GenericItemEmptying.canItemBeEmptied(level, heldItem)
                    || GenericItemFilling.canItemBeFilled(level, heldItem))
                return InteractionResult.SUCCESS;
            return InteractionResult.PASS;
        }

        FluidStack fluidInItem = GenericItemEmptying.emptyItem(level, heldItem, true).getFirst();
        int capacity = 1;
        FluidStack fluidInTank = FluidStack.EMPTY;

        for (int i = 0; i < fluidTank.getTanks(); i++) {
            if (fluidTank.getFluidInTank(i).isFluidEqual(fluidInItem)) {
                capacity = fluidTank.getTankCapacity(i);
                fluidInTank = fluidTank.getFluidInTank(i);
            }
        }

        SoundEvent soundevent = FluidHelper.getEmptySound(fluidInTank);

        if (soundevent != null && !level.isClientSide) {
            float pitch = Mth
                    .clamp(1 - (1f * fluidInTank.getAmount() / (capacity * 16)), 0, 1);
            pitch /= 1.5f;
            pitch += .5f;
            pitch += (level.random.nextFloat() - .5f) / 4f;
            level.playSound(null, pos, soundevent, SoundSource.BLOCKS, .5f, pitch);
        }

        return InteractionResult.SUCCESS;
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        withBlockEntityDo(level, pos, (be) ->
            FluidPumpBlockEntity.withBlockEntityDo(level, be.getFluidPumpPos(), FluidPumpBlockEntity::breakHose)
        );

        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return defaultBlockState().setValue(FACING, ctx.getNearestLookingDirection().getOpposite());
    }

    @Override
    public Class<FluidPortBlockEntity> getBlockEntityClass() {
        return FluidPortBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends FluidPortBlockEntity> getBlockEntityType() {
        return LogisticsBlockEntities.FLUID_PORT.get();
    }
}
