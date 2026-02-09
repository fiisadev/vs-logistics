package com.fiisadev.vs_logistics.mixin.createaddition;

import com.mrh0.createaddition.blocks.connector.base.AbstractConnectorBlockEntity;
import com.mrh0.createaddition.energy.IWireNode;
import com.mrh0.createaddition.energy.LocalNode;
import com.mrh0.createaddition.energy.WireType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.valkyrienskies.core.api.ships.Ship;
import org.valkyrienskies.mod.api.ValkyrienSkies;

@Mixin(AbstractConnectorBlockEntity.class)
public abstract class ConnectorTickMixin extends BlockEntity {
    public ConnectorTickMixin(BlockEntityType<?> pType, BlockPos pPos, BlockState pBlockState) {
        super(pType, pPos, pBlockState);
    }

    @Inject(method = "tick", at = @At("HEAD"), remap = false)
    public void vs_logistics$tick(CallbackInfo ci) {
        if (level.isClientSide) return;

        IWireNode node = (IWireNode)this;
        Ship nodeShip = ValkyrienSkies.getShipManagingBlock(level, node.getPos());

        for (int i = 0; i < node.getNodeCount(); i++) {
            if (!node.hasConnection(i)) continue;

            IWireNode other = node.getWireNode(i);
            Ship otherShip = ValkyrienSkies.getShipManagingBlock(level, node.getPos());

            if (other == null) continue;
            if (nodeShip == null && otherShip == null) continue;

            Vec3 p1 = ValkyrienSkies.positionToWorld(level, node.getPos().getCenter());
            Vec3 p2 = ValkyrienSkies.positionToWorld(level, other.getPos().getCenter());

            int maxLength = Math.min(node.getMaxWireLength(), other.getMaxWireLength());

            if (p1.distanceToSqr(p2) > maxLength * maxLength) {
                LocalNode otherLocal = node.getLocalNode(i);
                if (otherLocal != null) {
                    node.dropWire(level, otherLocal);
                    IWireNode.disconnect(level, node.getPos(), other.getPos());
                }
            }
        }
    }
}
