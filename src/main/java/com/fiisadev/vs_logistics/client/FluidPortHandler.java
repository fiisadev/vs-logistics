package com.fiisadev.vs_logistics.client;

import com.fiisadev.vs_logistics.content.fluid_port.FluidPortBlock;
import com.fiisadev.vs_logistics.content.fluid_port.FluidPortBlockEntity;
import com.fiisadev.vs_logistics.network.FluidPortPacket;
import com.fiisadev.vs_logistics.registry.LogisticsBlocks;
import com.fiisadev.vs_logistics.registry.LogisticsNetwork;
import com.simibubi.create.AllItems;
import net.createmod.catnip.data.Pair;
import net.createmod.catnip.outliner.Outliner;
import net.createmod.catnip.theme.Color;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
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
        BlockPos pos = event.getPos();
        Player player = event.getEntity();
        Level level = event.getLevel();
        boolean isHoldingWrench = player.getMainHandItem().is(AllItems.WRENCH.get());

        if (level.getBlockState(pos).is(LogisticsBlocks.FLUID_PORT.get())) {
            if (isHoldingWrench && !player.isShiftKeyDown()) {
                selectedSource = pos;
                event.setCanceled(true);
                return;
            }
        }

        BlockEntity be = level.getBlockEntity(pos);
        if (be == null) return;

        if (selectedSource != null && FluidPortBlock.isValidTarget(be)) {
            LogisticsNetwork.CHANNEL.sendToServer(new FluidPortPacket(selectedSource, pos));
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        Player player = Minecraft.getInstance().player;
        if (player == null) return;

        boolean isHoldingWrench = player.getMainHandItem().is(AllItems.WRENCH.get());

        if (!isHoldingWrench)
            selectedSource = null;

        if (selectedSource != null) {
            if (!(player.level().getBlockEntity(selectedSource) instanceof FluidPortBlockEntity fluidPort)) return;

            Outliner outliner = Outliner.getInstance();

            outliner.showAABB(Pair.of("fluidPort", selectedSource), AABB.unitCubeFromLowerCorner(Vec3.atLowerCornerOf(selectedSource)))
                .lineWidth(1 / 16f)
                .colored(Color.SPRING_GREEN);

            for (BlockPos pos : fluidPort.getTargets()) {
                outliner.showLine(Pair.of(Pair.of("connection", selectedSource), pos), selectedSource.getCenter(), pos.getCenter());
                outliner.showAABB(Pair.of(Pair.of("target", selectedSource), pos), AABB.unitCubeFromLowerCorner(Vec3.atLowerCornerOf(pos)))
                        .lineWidth(1 / 16f)
                        .colored(ChatFormatting.YELLOW.getColor());
            }
        }
    }
}
