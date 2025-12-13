package com.fiisadev.vs_logistics.content.nozzle;

import com.fiisadev.vs_logistics.registry.LogisticsItems;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

public class NozzleEntityRenderer extends EntityRenderer<NozzleEntity> {
    public NozzleEntityRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
    }

    @Override
    public void render(NozzleEntity entity, float yaw, float pt, PoseStack ms, MultiBufferSource buffer, int light) {
        ms.pushPose();

        ms.translate(0.0, 0.5, 0.0);
        ms.scale(1.0f, 1.0f, 1.0f);

        Minecraft.getInstance().getItemRenderer().renderStatic(
                new ItemStack(LogisticsItems.NOZZLE.get()),
                ItemDisplayContext.NONE,
                light,
                OverlayTexture.NO_OVERLAY,
                ms,
                buffer,
                entity.level(),
                entity.getId()
        );

        ms.popPose();
    }

    @Override
    public ResourceLocation getTextureLocation(NozzleEntity entity) {
        return null;
    }
}
