package com.fiisadev.vs_logistics.content.fluid_pump;

import com.fiisadev.vs_logistics.client.utils.HoseUtils;import com.mojang.blaze3d.vertex.*;
import com.simibubi.create.foundation.blockEntity.renderer.SafeBlockEntityRenderer;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3d;
import org.valkyrienskies.core.api.ships.Ship;
import org.valkyrienskies.mod.api.ValkyrienSkies;
import org.valkyrienskies.mod.common.VSGameUtilsKt;

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
        IFluidPumpHandler handler = be.getPumpHandler();
        if (handler == null) return;

        ms.pushPose();

        Vec3 hoseStart = be.getHoseStart();
        Vec3 hoseEnd = handler.getHoseEnd(partialTicks);
        Vec3 hoseEndDir = handler.getHoseDir(partialTicks);

        Player player = Minecraft.getInstance().player;
        if (handler.is(player) && Minecraft.getInstance().options.getCameraType() == CameraType.FIRST_PERSON) {
            float bodyRotation = player.yBodyRotO + (player.yBodyRot - player.yBodyRotO) * partialTicks;
            hoseEnd = player.getPosition(partialTicks).add(new Vec3(-0.5, 1, -3).yRot((float)Math.toRadians(-bodyRotation)));
        }

        Ship fluidPumpShip = ValkyrienSkies.getShipManagingBlock(be.getLevel(), be.getBlockPos());
        if (fluidPumpShip != null) {
            Vector3d posJOML = new Vector3d(hoseEnd.x, hoseEnd.y, hoseEnd.z);
            posJOML = fluidPumpShip.getWorldToShip().transformPosition(posJOML);
            hoseEnd = new Vec3(posJOML.x, posJOML.y, posJOML.z);

            Vector3d dirJOML = new Vector3d(hoseEndDir.x, hoseEndDir.y, hoseEndDir.z);
            dirJOML = fluidPumpShip.getWorldToShip().transformDirection(dirJOML);
            hoseEndDir = new Vec3(dirJOML.x, dirJOML.y, dirJOML.z);
        }

        hoseEnd = hoseEnd.subtract(Vec3.atLowerCornerOf(be.getBlockPos()));

        double dist = hoseStart.distanceTo(hoseEnd);

        Vec3 p1 = hoseStart.add(be.getDirection().scale(dist * 0.3f));
        Vec3 p2 = hoseEnd.subtract(hoseEndDir.scale(dist * 0.3f));

        VertexConsumer builder = buffer.getBuffer(RenderType.debugQuads());
        renderCurvedHose(builder, ms, hoseStart, hoseEnd, p1, p2, dist, be.getBlockPos());

        ms.popPose();
    }

    private static void renderCurvedHose(VertexConsumer builder, PoseStack ms, Vec3 start, Vec3 end, Vec3 p1, Vec3 p2, double dist, BlockPos originPos) {
        Vec3[] centers = HoseUtils.generateHoseSegments(start, end, p1, p2, dist, originPos);

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
                    Vec3 a = prevRing[j];
                    Vec3 b = prevRing[next];
                    Vec3 c = currRing[next];
                    Vec3 d = currRing[j];

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
