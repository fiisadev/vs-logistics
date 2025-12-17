package com.fiisadev.vs_logistics.client.event;

import com.fiisadev.vs_logistics.VSLogistics;
import com.fiisadev.vs_logistics.client.utils.HoseUtils;
import com.fiisadev.vs_logistics.content.fluid_pump.FluidPumpPlayerDataProvider;
import com.fiisadev.vs_logistics.registry.LogisticsItems;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderHandEvent;
import net.minecraftforge.client.event.RenderPlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = VSLogistics.MOD_ID, value = Dist.CLIENT)
public class NozzleRenderer {
    private static float startUsingTime = 0;

    @SubscribeEvent
    public static void renderFirstPerson(RenderHandEvent event) {
        Minecraft mc = Minecraft.getInstance();

        if (mc.player == null || mc.level == null) return;

        mc.player.getCapability(FluidPumpPlayerDataProvider.FLUID_PUMP_PLAYER_DATA).ifPresent((playerData) -> {
            BlockPos fluidPumpPos = playerData.getFluidPumpPos();
            if (fluidPumpPos == null) return;

            mc.player.swinging = false;

            event.setCanceled(true);

            ItemRenderer renderer = Minecraft.getInstance().getItemRenderer();
            PoseStack poseStack = event.getPoseStack();

            poseStack.pushPose();
            poseStack.translate(1.4, -1, -1.7);
            poseStack.mulPose(Axis.YP.rotationDegrees(7));
            poseStack.mulPose(Axis.XP.rotationDegrees(22.5f));
            poseStack.scale(1.6f, 1.6f, 1.6f);

            if (startUsingTime == 0 && InputEvent.isUseHeld) {
                startUsingTime = mc.level.getGameTime() + event.getPartialTick();
            }

            if (InputEvent.isUseHeld) {
                float time = mc.level.getGameTime() + event.getPartialTick() - startUsingTime;
                poseStack.translate(0, 0.05 * Math.sin(time / 6f), 0);
            } else {
                startUsingTime = 0;
            }

            ItemStack item = new ItemStack(LogisticsItems.NOZZLE.get());
            BakedModel bakedModel = renderer.getModel(item, mc.level, null, 0);

            renderer.render(
                    item,
                    ItemDisplayContext.FIRST_PERSON_RIGHT_HAND,
                    false,
                    poseStack,
                    event.getMultiBufferSource(),
                    LightTexture.FULL_BRIGHT,
                    OverlayTexture.NO_OVERLAY,
                    bakedModel
            );

            poseStack.popPose();
        });
    }

    @SubscribeEvent
    public static void renderThirdPerson(RenderPlayerEvent.Pre event) {
        Player player = event.getEntity();

        player.getCapability(FluidPumpPlayerDataProvider.FLUID_PUMP_PLAYER_DATA).ifPresent(playerData -> {
            BlockPos fluidPumpPos = playerData.getFluidPumpPos();

            if (fluidPumpPos == null) return;

            player.swinging = false;

            PoseStack ps = event.getPoseStack();
            MultiBufferSource buffers = event.getMultiBufferSource();
            ItemRenderer itemRenderer = Minecraft.getInstance().getItemRenderer();
            int packedLight = event.getPackedLight();

            ps.pushPose();

            Vec3 pos = HoseUtils.getNozzleHandlePosition(player, event.getPartialTick());
            ps.translate(pos.x, pos.y, pos.z);


            float bodyRotation = player.yBodyRotO + (player.yBodyRot - player.yBodyRotO) * event.getPartialTick();
            ps.mulPose(Axis.YN.rotationDegrees(bodyRotation + 180));
            ps.mulPose(Axis.XP.rotationDegrees(22.5f));

            if (player.isCrouching())
                ps.translate(0, 0.13, -0.06);

            ps.translate(0, -0.03, -0.3);

            ItemStack nozzle = new ItemStack(LogisticsItems.NOZZLE.get());
            BakedModel baked = itemRenderer.getModel(nozzle, player.level(), null, 0);

            itemRenderer.render(
                    nozzle,
                    ItemDisplayContext.THIRD_PERSON_RIGHT_HAND,
                    false,
                    ps,
                    buffers,
                    packedLight,
                    OverlayTexture.NO_OVERLAY,
                    baked
            );

            ps.popPose();
        });
    }
}
