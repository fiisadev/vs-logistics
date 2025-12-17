package com.fiisadev.vs_logistics.content.fluid_port;

import com.fiisadev.vs_logistics.registry.LogisticsItems;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.simibubi.create.foundation.blockEntity.renderer.SafeBlockEntityRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public class FluidPortRenderer extends SafeBlockEntityRenderer<FluidPortBlockEntity> {
    public FluidPortRenderer(BlockEntityRendererProvider.Context ctx) {
    }

    @Override
    protected void renderSafe(FluidPortBlockEntity be, float partialTicks, PoseStack ms, MultiBufferSource bufferSource, int packedLight, int overlay) {
        Level level = be.getLevel();

        if (level == null || be.getFluidPumpPos() == null)
            return;

        ms.pushPose();

        Direction dir = be.getBlockState().getValue(FluidPortBlock.FACING);

        Vec3 offset = Vec3.atLowerCornerOf(dir.getNormal()).scale(0.7).add(0.5, 0.5, 0.5);
        ms.translate(offset.x, offset.y, offset.z);

        if (dir == Direction.UP) {
            ms.mulPose(Axis.XP.rotationDegrees(-90));
        } else if (dir == Direction.DOWN) {
            ms.mulPose(Axis.XP.rotationDegrees(90));
        } else {
            ms.mulPose(Axis.YP.rotationDegrees(-dir.toYRot()));
        }

        ItemRenderer itemRenderer = Minecraft.getInstance().getItemRenderer();
        ItemStack nozzle = new ItemStack(LogisticsItems.NOZZLE.get());
        BakedModel baked = itemRenderer.getModel(nozzle, be.getLevel(), null, 0);
        int light = LevelRenderer.getLightColor(level, be.getBlockPos().relative(dir));

        Minecraft.getInstance().getItemRenderer().render(
                nozzle,
                ItemDisplayContext.NONE,
                false,
                ms,
                bufferSource,
                light,
                OverlayTexture.NO_OVERLAY,
                baked
        );

        ms.popPose();
    }
}
