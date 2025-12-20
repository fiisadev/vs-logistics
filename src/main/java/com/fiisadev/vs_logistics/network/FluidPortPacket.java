package com.fiisadev.vs_logistics.network;

import com.fiisadev.vs_logistics.content.fluid_port.FluidPortBlock;
import com.fiisadev.vs_logistics.content.fluid_port.FluidPortBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class FluidPortPacket {

    private final BlockPos fluidPortPos;
    private final BlockPos targetPos;

    public FluidPortPacket(BlockPos fluidPortPos, BlockPos targetPos) {
        this.fluidPortPos = fluidPortPos;
        this.targetPos = targetPos;
    }

    public FluidPortPacket(FriendlyByteBuf buf) {
        this.fluidPortPos = buf.readBlockPos();
        this.targetPos = buf.readBlockPos();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeBlockPos(fluidPortPos);
        buf.writeBlockPos(targetPos);
    }

    public void handle(Supplier<NetworkEvent.Context> ctxSup) {
        NetworkEvent.Context ctx = ctxSup.get();

        ctx.enqueueWork(() -> {
            Player sender = ctx.getSender();
            if (sender == null) return;

            if (sender.level().getBlockEntity(fluidPortPos) instanceof FluidPortBlockEntity fluidPort) {
                BlockEntity targetBE = sender.level().getBlockEntity(targetPos);

                if (FluidPortBlock.isValidTarget(targetBE))
                    fluidPort.addTarget(targetPos);
            }
        });

        ctx.setPacketHandled(true);
    }
}
