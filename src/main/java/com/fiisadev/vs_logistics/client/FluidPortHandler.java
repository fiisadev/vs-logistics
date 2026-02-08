package com.fiisadev.vs_logistics.client;

import com.fiisadev.vs_logistics.content.fluid_port.FluidPortBlockEntity;
import com.fiisadev.vs_logistics.content.fluid_port.FluidPortTarget;
import com.fiisadev.vs_logistics.content.pipe_wrench.PipeWrenchItem;
import com.fiisadev.vs_logistics.network.FluidPortPacket;
import com.fiisadev.vs_logistics.registry.LogisticsBlocks;
import com.fiisadev.vs_logistics.registry.LogisticsItems;
import com.fiisadev.vs_logistics.registry.LogisticsNetwork;
import com.simibubi.create.AllItems;
import com.simibubi.create.foundation.blockEntity.IMultiBlockEntityContainer;
import net.createmod.catnip.data.Pair;
import net.createmod.catnip.outliner.Outliner;
import net.createmod.catnip.theme.Color;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.jetbrains.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
@Mod.EventBusSubscriber(value = Dist.CLIENT)
public class FluidPortHandler {
    @Nullable
    static BlockPos selectedSource;

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (event.getHand() != InteractionHand.MAIN_HAND || !event.getLevel().isClientSide) return;

        BlockPos pos = event.getPos();
        Player player = event.getEntity();
        Level level = event.getLevel();
        boolean isHoldingWrench = player.getMainHandItem().is(PipeWrenchItem.getPipeWrench());

        if (level.getBlockState(pos).is(LogisticsBlocks.FLUID_PORT.get())) {
            if (isHoldingWrench && !player.isShiftKeyDown()) {
                selectedSource = pos;
                return;
            }
        }

        BlockEntity be = level.getBlockEntity(pos);
        if (be == null) return;

        if (selectedSource != null) {
            LogisticsNetwork.CHANNEL.sendToServer(new FluidPortPacket(selectedSource, pos));
        }
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        Player player = Minecraft.getInstance().player;
        if (player == null) return;

        boolean isHoldingWrench = player.getMainHandItem().is(PipeWrenchItem.getPipeWrench());

        if (!isHoldingWrench)
            selectedSource = null;

        if (selectedSource != null) {
            if (!(player.level().getBlockEntity(selectedSource) instanceof FluidPortBlockEntity fluidPort)) return;

            Outliner outliner = Outliner.getInstance();

            outliner.showAABB(Pair.of("fluidPort", selectedSource), AABB.unitCubeFromLowerCorner(Vec3.atLowerCornerOf(selectedSource)))
                .lineWidth(1 / 16f)
                .colored(Color.SPRING_GREEN);

            for (FluidPortTarget target : fluidPort.getTargets().values()) {
                BlockPos pos = target.getPos();
                AABB aabb = AABB.unitCubeFromLowerCorner(Vec3.atLowerCornerOf(pos));

                if (player.level().getBlockEntity(pos) instanceof IMultiBlockEntityContainer.Fluid multiBlock) {
                    Vec3 size;
                    Direction.Axis axis = multiBlock.getMainConnectionAxis();

                    if (axis == Direction.Axis.Y)
                        size = new Vec3(multiBlock.getWidth(), multiBlock.getHeight(), multiBlock.getWidth());
                    else if (axis == Direction.Axis.X)
                        size = new Vec3(multiBlock.getHeight(), multiBlock.getWidth(), multiBlock.getWidth());
                    else
                        size = new Vec3(multiBlock.getWidth(), multiBlock.getWidth(), multiBlock.getHeight());

                    aabb = new AABB(0, 0, 0, size.x, size.y, size.z).move(Vec3.atLowerCornerOf(pos));
                }

                outliner.showAABB(Pair.of(Pair.of("target", selectedSource), pos), aabb)
                        .lineWidth(1 / 16f)
                        .colored(target.getMode().color);
            }
        }
    }
}
