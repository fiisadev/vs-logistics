package com.fiisadev.vs_logistics.mixin.createaddition;

import com.mrh0.createaddition.blocks.connector.ConnectorType;
import com.mrh0.createaddition.energy.IWireNode;
import com.mrh0.createaddition.energy.WireConnectResult;
import com.mrh0.createaddition.energy.WireType;
import com.mrh0.createaddition.item.WireSpool;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.valkyrienskies.mod.api.ValkyrienSkies;

@Pseudo
@Mixin(WireSpool.class)
public class WireSpoolMixin {

    @Redirect(
            method = "useOn",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/mrh0/createaddition/energy/IWireNode;connect(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;ILnet/minecraft/core/BlockPos;ILcom/mrh0/createaddition/energy/WireType;)Lcom/mrh0/createaddition/energy/WireConnectResult;"
            )
    )
    private WireConnectResult connect(
            Level world,
            BlockPos pos1,
            int node1,
            BlockPos pos2,
            int node2,
            WireType type
    ) {
        BlockEntity te1 = world.getBlockEntity(pos1);
        BlockEntity te2 = world.getBlockEntity(pos2);
        if (te1 == null || te2 == null || te1 == te2)
            return WireConnectResult.INVALID;
        if (!(te1 instanceof IWireNode wn1) || !(te2 instanceof IWireNode wn2))
            return WireConnectResult.INVALID;
        if (node1 < 0 || node2 < 0)
            return WireConnectResult.COUNT;

        int maxLength = Math.min(wn1.getMaxWireLength(), wn2.getMaxWireLength());

        Vec3 p1 = ValkyrienSkies.positionToWorld(world, pos1.getCenter());
        Vec3 p2 = ValkyrienSkies.positionToWorld(world, pos2.getCenter());

        if (p1.distanceToSqr(p2) > maxLength * maxLength) return WireConnectResult.LONG;
        if (wn1.hasConnectionTo(pos2)) return WireConnectResult.EXISTS;
        if(wn1.getConnectorType() == ConnectorType.Large && wn2.getConnectorType() == ConnectorType.Large) {
            if(type == WireType.COPPER) return WireConnectResult.REQUIRES_HIGH_CURRENT;
        }

        wn1.setNode(node1, node2, wn2.getPos(), type);
        wn2.setNode(node2, node1, wn1.getPos(), type);
        return WireConnectResult.getLink(wn2.isNodeInput(node2), wn2.isNodeOutput(node2));
    }
}
