package com.fiisadev.vs_logistics.mixin.createaddition;

import com.fiisadev.vs_logistics.utils.ShipUtils;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mrh0.createaddition.energy.IWireNode;
import com.mrh0.createaddition.energy.WireType;
import com.mrh0.createaddition.event.ClientEventHandler;
import com.mrh0.createaddition.rendering.WireNodeRenderer;
import com.mrh0.createaddition.util.ClientMinecraftWrapper;
import com.mrh0.createaddition.util.Util;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.valkyrienskies.core.api.ships.ClientShip;
import org.valkyrienskies.mod.api.ValkyrienSkies;

@Mixin(WireNodeRenderer.class)
public abstract class WireNodeRendererMixin<T extends BlockEntity> implements BlockEntityRenderer<T> {

    @Shadow private float time;

    @Shadow private static float hang(float t, float dis) { return 0f; }
    @Shadow private static float divf(int a, int b) { return 0f; }
    @Shadow public static void wireRender(BlockEntity tileEntityIn, BlockPos other, PoseStack stack, MultiBufferSource buffer, float x, float y, float z, WireType type, float dis) {
    }

    private static Vec3 currentGravity = new Vec3(0, -1, 0);

    public WireNodeRendererMixin(BlockEntityRendererProvider.Context context) {}

    // W Gemini Pro
    @Inject(
            method = "wireVert",
            at = @At("HEAD"),
            cancellable = true,
            remap = false
    )
    private static void onWireVert(VertexConsumer vertBuilder, Matrix4f matrix, int light, float x, float y, float z,
                                   float a, float b, int count, int index, boolean sw, float o1, float o2,
                                   WireType type, float dis, BlockState state, PoseStack stack, int lightOffset, float hangFactor,
                                   org.spongepowered.asm.mixin.injection.callback.CallbackInfo ci) {

        int cr = type.getRed();
        int cg = type.getGreen();
        int cb = type.getBlue();

        if (index % 2 == 0) {
            cr *= 0.7F;
            cg *= 0.7F;
            cb *= 0.7F;
        }

        float part = (float) index / (float) count;

        float fx = x * part;
        float fy = y * part;
        float fz = z * part;

        float hangMag = -hangFactor * hang(divf(index, count), dis);

        fx += (float) (currentGravity.x * hangMag);
        fy += (float) (currentGravity.y * hangMag);
        fz += (float) (currentGravity.z * hangMag);

        if(Math.abs(x) + Math.abs(z) < Math.abs(y)) {
            boolean p = b > 0;
            float c = 0.015f;

            if (!sw) {
                vertBuilder.vertex(matrix, fx -c, fy, fz + (p?-c:c)).color(cr, cg, cb, 255).uv2(light).endVertex();
            }

            vertBuilder.vertex(matrix, fx + c, fy, fz + (p?c:-c)).color(cr, cg, cb, 255).uv2(light).endVertex();
            if (sw) {
                vertBuilder.vertex(matrix, fx -c, fy, fz + (p?-c:c)).color(cr, cg, cb, 255).uv2(light).endVertex();
            }
        }
        else {
            if (!sw) {
                vertBuilder.vertex(matrix, fx + o1, fy + a - b, fz - o2).color(cr, cg, cb, 255).uv2(light).endVertex();
            }

            vertBuilder.vertex(matrix, fx - o1, fy + b, fz + o2).color(cr, cg, cb, 255).uv2(light).endVertex();
            if (sw) {
                vertBuilder.vertex(matrix, fx + o1, fy + a - b, fz - o2).color(cr, cg, cb, 255).uv2(light).endVertex();
            }
        }
        ci.cancel();
    }

    /**
     * @author fiisadev
     * @reason yes
     */
    @Overwrite
    public void render(T be, float partialTicks, PoseStack matrixStackIn, MultiBufferSource bufferIn,
                       int combinedLightIn, int combinedOverlayIn) {
        IWireNode te = (IWireNode) be;
        time += partialTicks;

        Vec3 sourceCenter = te.getPos().getCenter();
        ClientShip sourceShip = (ClientShip)ValkyrienSkies.getShipManagingBlock(be.getLevel(), be.getBlockPos());

        if (sourceShip != null)
            currentGravity = ShipUtils.dirToShip(sourceShip, new Vec3(0, -1, 0));
        else
            currentGravity = new Vec3(0, -1, 0);

        for (int i = 0; i < te.getNodeCount(); i++) {
            if (!te.hasConnection(i)) continue;

            IWireNode wn = te.getWireNode(i);
            if (wn == null) return;

            Vec3 o1 = te.getNodeOffset(i);
            Vec3 o2 = wn.getNodeOffset(te.getOtherNodeIndex(i));

            BlockPos destBlockPos = te.getNodePos(i);
            Vec3 destCenter = destBlockPos.getCenter();
            ClientShip destShip = (ClientShip)ValkyrienSkies.getShipManagingBlock(be.getLevel(), destBlockPos);

            Vec3 destWorldConn;

            if (destShip != null) {
                Vec3 destCenterWorld = ShipUtils.shipToWorld(destShip, destCenter);
                Vec3 o2World = ShipUtils.dirToWorld(destShip, o2);
                destWorldConn = destCenterWorld.add(o2World);
            } else {
                destWorldConn = destCenter.add(o2);
            }

            Vec3 destLocalConn;
            if (sourceShip != null) {
                destLocalConn = ShipUtils.worldToShip(sourceShip, destWorldConn);
            } else {
                destLocalConn = destWorldConn;
            }

            Vec3 sourceLocalConn = sourceCenter.add(o1);

            Vec3 drawVector = destLocalConn.subtract(sourceLocalConn);

            matrixStackIn.pushPose();
            matrixStackIn.translate(.5f + o1.x, .5f + o1.y, .5f + o1.z);

            wireRender(
                    be,
                    destBlockPos,
                    matrixStackIn,
                    bufferIn,
                    (float) drawVector.x,
                    (float) drawVector.y,
                    (float) drawVector.z,
                    te.getNodeType(i),
                    (float)drawVector.length()
            );
            matrixStackIn.popPose();
        }

        if(ClientEventHandler.clientRenderHeldWire) {
            LocalPlayer player = ClientMinecraftWrapper.getPlayer();
            Util.Triple<BlockPos, Integer, WireType> wireNode = Util.getWireNodeOfSpools(player.getInventory().getSelected());
            if(wireNode == null) return;

            BlockPos nodePos = wireNode.a;
            int nodeIndex = wireNode.b;
            WireType wireType = wireNode.c;
            if(!nodePos.equals(te.getPos())) return;

            Vec3 d1 = te.getNodeOffset(nodeIndex);
            float ox1 = ((float) d1.x());
            float oy1 = ((float) d1.y());
            float oz1 = ((float) d1.z());

            Vec3 playerPos = player.getPosition(partialTicks);
            float tx = (float)playerPos.x - te.getPos().getX();
            float ty = (float)playerPos.y - te.getPos().getY();
            float tz = (float)playerPos.z - te.getPos().getZ();
            matrixStackIn.pushPose();

            float dis = 0;

            matrixStackIn.translate(tx + .5f, ty + .5f, tz + .5f);
            wireRender(
                    be,
                    player.blockPosition(),
                    matrixStackIn,
                    bufferIn,
                    -tx + ox1,
                    -ty + oy1,
                    -tz + oz1,
                    wireType,
                    dis
            );
            matrixStackIn.popPose();
        }
    }

}
