package com.fiisadev.vs_logistics.content.fluid_port;

import com.fiisadev.vs_logistics.content.fluid_pump.FluidPumpBlockEntity;
import com.fiisadev.vs_logistics.registry.LogisticsBlockEntities;
import com.simibubi.create.foundation.block.IBE;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import org.jetbrains.annotations.Nullable;

public class FluidPortBlock extends DirectionalBlock implements IBE<FluidPortBlockEntity> {
    public FluidPortBlock(Properties properties) {
        super(properties);
    }

    public static boolean isFluidTank(BlockEntity target) {
        if (target == null) return false;
        if (target instanceof FluidPortBlockEntity) return false;
        return target.getCapability(ForgeCapabilities.FLUID_HANDLER).isPresent();
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!level.isClientSide) {
            withBlockEntityDo(level, pos, (be) -> {
                if (player.isShiftKeyDown()) {
                    be.onUse(player);
                }
            });
        }

        return InteractionResult.sidedSuccess(level.isClientSide);
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
