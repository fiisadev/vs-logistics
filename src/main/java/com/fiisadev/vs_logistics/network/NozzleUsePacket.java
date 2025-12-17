package com.fiisadev.vs_logistics.network;

import com.fiisadev.vs_logistics.content.fluid_pump.FluidPumpBlockEntity;
import com.fiisadev.vs_logistics.content.fluid_pump.FluidPumpPlayerDataProvider;
import com.fiisadev.vs_logistics.event.NozzleUseHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class NozzleUsePacket {

    private final boolean isKeyDown;
    private final boolean dropNozzle;

    public NozzleUsePacket(boolean isKeyDown, boolean dropNozzle) {
        this.isKeyDown = isKeyDown;
        this.dropNozzle = dropNozzle;
    }

    public NozzleUsePacket(FriendlyByteBuf buf) {
        this.isKeyDown = buf.readBoolean();
        this.dropNozzle = buf.readBoolean();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeBoolean(isKeyDown);
        buf.writeBoolean(dropNozzle);
    }

    public void handle(Supplier<NetworkEvent.Context> ctxSup) {
        NetworkEvent.Context ctx = ctxSup.get();

        ctx.enqueueWork(() -> {
            Player player = ctx.getSender();
            if (player == null) return;

            NozzleUseHandler.set(player.getUUID(), isKeyDown);

            if (dropNozzle) {
                player.getCapability(FluidPumpPlayerDataProvider.FLUID_PUMP_PLAYER_DATA).ifPresent((playerData) -> {
                    FluidPumpBlockEntity.withBlockEntityDo(player.level(), playerData.getFluidPumpPos(), FluidPumpBlockEntity::breakHose);
                });
            }
        });

        ctx.setPacketHandled(true);
    }
}
