package com.fiisadev.vs_logistics.mixin.createaddition;

import com.mojang.blaze3d.vertex.PoseStack;
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
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.valkyrienskies.core.api.ships.Ship;
import org.valkyrienskies.mod.api.ValkyrienSkies;
import org.valkyrienskies.mod.common.VSGameUtilsKt;

@Mixin(WireNodeRenderer.class)
public abstract class WireNodeRendererMixin<T extends BlockEntity> implements BlockEntityRenderer<T> {

    @Shadow private float time;

    @Shadow public static float distanceFromZero(float x, float y, float z) {
        throw new IllegalStateException("This is shadow method and should never be called");
    }

    @Shadow
    public static void wireRender(BlockEntity tileEntityIn, BlockPos other, PoseStack stack, MultiBufferSource buffer, float x, float y, float z, WireType type, float dis) {
    }

    public WireNodeRendererMixin(BlockEntityRendererProvider.Context context) {}

    /**
     * @author fiisadev
     * @reason VS
     */
    @Overwrite
    public void render(T be, float partialTicks, PoseStack matrixStackIn, MultiBufferSource bufferIn,
                       int combinedLightIn, int combinedOverlayIn) {
        IWireNode te = (IWireNode) be;

        time += partialTicks;

        for (int i = 0; i < te.getNodeCount(); i++) {
            if (!te.hasConnection(i)) continue;
            Vec3 d1 = te.getNodeOffset(i);
            float ox1 = ((float) d1.x());
            float oy1 = ((float) d1.y());
            float oz1 = ((float) d1.z());

            IWireNode wn = te.getWireNode(i);
            if (wn == null) return;

            Vec3 d2 = wn.getNodeOffset(te.getOtherNodeIndex(i)); // get other
            float ox2 = ((float) d2.x());
            float oy2 = ((float) d2.y());
            float oz2 = ((float) d2.z());

            Vec3 pos = Vec3.atLowerCornerOf(te.getPos());
            Ship ship = ValkyrienSkies.getShipManagingBlock(be.getLevel(), pos);
            if (ship != null) {
                pos = VSGameUtilsKt.toWorldCoordinates(ship, pos);
            }

            BlockPos other = te.getNodePos(i);
            Vec3 otherPos = Vec3.atLowerCornerOf(other).add(0.5f, 0.5f, 0.5f);
            Ship otherShip = ValkyrienSkies.getShipManagingBlock(be.getLevel(), other);
            if (otherShip != null) {
                otherPos = VSGameUtilsKt.toWorldCoordinates(otherShip, otherPos);
            }

            float tx = (float)(otherPos.x - pos.x);
            float ty = (float)(otherPos.y - pos.y);
            float tz = (float)(otherPos.z - pos.z);
            matrixStackIn.pushPose();

            float dis = distanceFromZero(tx, ty, tz);

            matrixStackIn.translate(tx + ox2 + 0.5f, ty + oy2 + 0.5f, tz + oz2 + 0.5f);
            wireRender(
                    be,
                    other,
                    matrixStackIn,
                    bufferIn,
                    -tx - ox2 + ox1,
                    -ty - oy2 + oy1,
                    -tz - oz2 + oz1,
                    te.getNodeType(i),
                    dis
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

            float dis = distanceFromZero(tx, ty, tz);

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
