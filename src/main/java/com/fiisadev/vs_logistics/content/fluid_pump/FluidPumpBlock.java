package com.fiisadev.vs_logistics.content.fluid_pump;

import com.fiisadev.vs_logistics.registry.LogisticsBlockEntities;
import com.simibubi.create.AllShapes;
import com.simibubi.create.foundation.block.IBE;
import net.createmod.catnip.math.VoxelShaper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

public class FluidPumpBlock extends HorizontalDirectionalBlock implements IBE<FluidPumpBlockEntity> {
    private static final VoxelShaper SHAPER = new AllShapes.Builder(Shapes.or(
        Block.box(0, 0, 0, 16, 16, 11),
        Block.box(0, 0, 11, 16, 11, 16),
        Block.box(0, 0, 11, 16, 15, 12),
        Block.box(0, 0, 12, 16, 14, 13),
        Block.box(0, 0, 13, 16, 13, 14),
        Block.box(0, 0, 14, 16, 12, 15)
    )).forHorizontal(Direction.UP);

    public FluidPumpBlock(Properties properties) { super(properties); }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return SHAPER.get(state.getValue(FACING));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
        super.createBlockStateDefinition(builder);
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return defaultBlockState().setValue(FACING, ctx.getHorizontalDirection().getOpposite());
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!level.isClientSide) {
            withBlockEntityDo(level, pos, (be) -> {
                IUserInfo userInfo = be.getUserInfo();

                if (userInfo != null && !userInfo.is(player)) return;

                player.getCapability(FluidPumpPlayerDataProvider.FLUID_PUMP_PLAYER_DATA).ifPresent((playerData) -> {
                    BlockPos fluidPumpPos = playerData.getFluidPumpPos();

                    if (fluidPumpPos == null) {
                        be.setUserInfo(new PlayerUserInfo(be, player));
                        return;
                    }

                    if (fluidPumpPos.equals(pos))
                        be.setUserInfo(null);
                });
            });
        }

        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        withBlockEntityDo(level, pos, (be) -> {
            IUserInfo userInfo = be.getUserInfo();

            if (userInfo == null)
                return;

            be.breakHose();
        });

        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    @Override
    public Class<FluidPumpBlockEntity> getBlockEntityClass() {
        return FluidPumpBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends FluidPumpBlockEntity> getBlockEntityType() {
        return LogisticsBlockEntities.FLUID_PUMP.get();
    }
}
