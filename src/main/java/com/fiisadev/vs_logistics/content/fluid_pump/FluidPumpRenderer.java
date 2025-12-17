package com.fiisadev.vs_logistics.content.fluid_pump;

import com.fiisadev.vs_logistics.client.utils.HoseUtils;
import com.mojang.blaze3d.vertex.*;
import com.simibubi.create.foundation.blockEntity.renderer.SafeBlockEntityRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;

public class FluidPumpRenderer extends SafeBlockEntityRenderer<FluidPumpBlockEntity> {
    public static final int SEGMENTS = HoseUtils.SEGMENTS;
    public static final int RADIAL_SEGMENTS = HoseUtils.RADIAL_SEGMENTS;
    public static final float RADIUS = HoseUtils.RADIUS;

    public FluidPumpRenderer(BlockEntityRendererProvider.Context ctx) {
    }

    @Override
    public int getViewDistance() {
        return 128;
    }

    @Override
    public boolean shouldRenderOffScreen(FluidPumpBlockEntity be) {
        return true;
    }

    @Override
    protected void renderSafe(FluidPumpBlockEntity be, float partialTicks, PoseStack ms, MultiBufferSource buffer, int light, int overlay) {
        Minecraft mc = Minecraft.getInstance();

        if (be.getUserInfo() == null)
            return;

        ms.pushPose();

        Vec3 origin = Vec3.atLowerCornerOf(be.getBlockPos());

        VertexConsumer builder = buffer.getBuffer(RenderType.debugQuads());

        Direction facing = be.getBlockState().getValue(FluidPumpBlock.FACING);

        Vec3 center = be.getBlockPos().getCenter();
        Vec3 pumpPos = center.relative(facing, 0.5).add(0, -0.5 + 5 / 16f, 0);
        Vec3 userPos = be.getUserInfo().getHoseEnd(partialTicks);

        double dist = pumpPos.distanceTo(userPos);

        Vec3 p1 = pumpPos.add(new Vec3(facing.getStepX(), facing.getStepY(), facing.getStepZ()).scale(dist * 0.3f));
        Vec3 p2 = userPos.subtract(be.getUserInfo().getHoseDir(partialTicks).scale(dist * 0.3f));

        renderCurvedHose(builder, ms, pumpPos, userPos, p1, p2, dist, origin);
        ms.popPose();
    }

    private static void renderCurvedHose(VertexConsumer builder, PoseStack ms, Vec3 start, Vec3 end, Vec3 p1, Vec3 p2, double dist, Vec3 origin) {
        Vec3[] centers = HoseUtils.generateHoseSegments(start, end, p1, p2, dist);

        Vec3 prevUp = new Vec3(0, 1, 0);
        Vec3[] prevRing = new Vec3[RADIAL_SEGMENTS];
        Vec3[] currRing = new Vec3[RADIAL_SEGMENTS];

        for (int i = 0; i <= SEGMENTS; i++) {
            Vec3 center = centers[i];
            Vec3 tangent = (i < SEGMENTS ? centers[i + 1].subtract(center) : center.subtract(centers[i - 1])).normalize();

            Vec3 right = tangent.cross(prevUp).normalize();
            Vec3 up = right.cross(tangent).normalize();
            prevUp = up;

            for (int j = 0; j < RADIAL_SEGMENTS; j++) {
                double angle = 2 * Math.PI * j / RADIAL_SEGMENTS;
                currRing[j] = center.add(right.scale(Math.cos(angle) * RADIUS).add(up.scale(Math.sin(angle) * RADIUS)));
            }

            if (i > 0) {
                for (int j = 0; j < RADIAL_SEGMENTS; j++) {
                    int next = (j + 1) % RADIAL_SEGMENTS;
                    Vec3 a = prevRing[j].subtract(origin);
                    Vec3 b = prevRing[next].subtract(origin);
                    Vec3 c = currRing[next].subtract(origin);
                    Vec3 d = currRing[j].subtract(origin);

                    builder.vertex(ms.last().pose(), (float)d.x, (float)d.y, (float)d.z).color(15, 15, 15, 255).endVertex();
                    builder.vertex(ms.last().pose(), (float)c.x, (float)c.y, (float)c.z).color(15, 15, 15, 255).endVertex();
                    builder.vertex(ms.last().pose(), (float)b.x, (float)b.y, (float)b.z).color(15, 15, 15, 255).endVertex();
                    builder.vertex(ms.last().pose(), (float)a.x, (float)a.y, (float)a.z).color(15, 15, 15, 255).endVertex();
                }
            }

            Vec3[] temp = prevRing;
            prevRing = currRing;
            currRing = temp;
        }
    }
}
