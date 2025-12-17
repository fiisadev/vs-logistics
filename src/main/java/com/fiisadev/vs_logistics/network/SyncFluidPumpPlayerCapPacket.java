package com.fiisadev.vs_logistics.network;

import com.fiisadev.vs_logistics.client.utils.HoseUtils;
import com.fiisadev.vs_logistics.content.fluid_pump.FluidPumpBlock;
import com.fiisadev.vs_logistics.content.fluid_pump.FluidPumpBlockEntity;
import com.fiisadev.vs_logistics.content.fluid_pump.FluidPumpPlayerData;
import com.fiisadev.vs_logistics.content.fluid_pump.FluidPumpPlayerDataProvider;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import org.joml.Vector3f;

import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

public class SyncFluidPumpPlayerCapPacket {
    private final BlockPos fluidPumpPos;
    private final UUID playerUUID;

    public SyncFluidPumpPlayerCapPacket(BlockPos fluidPumpPos, UUID playerUUID) {
        this.fluidPumpPos = fluidPumpPos;
        this.playerUUID = playerUUID;
    }

    public SyncFluidPumpPlayerCapPacket(Player player, FluidPumpPlayerData data) {
        this.fluidPumpPos = data.getFluidPumpPos();
        this.playerUUID = player.getUUID();
    }

    public SyncFluidPumpPlayerCapPacket(FriendlyByteBuf buf) {
        this.fluidPumpPos = buf.readOptional(FriendlyByteBuf::readBlockPos).orElse(null);
        this.playerUUID = buf.readUUID();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeOptional(Optional.ofNullable(fluidPumpPos), FriendlyByteBuf::writeBlockPos);
        buf.writeUUID(playerUUID);
    }

    public void handle(Supplier<NetworkEvent.Context> ctxSup) {
        NetworkEvent.Context ctx = ctxSup.get();

        ctx.enqueueWork(() -> {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
                ClientLevel level =  Minecraft.getInstance().level;
                if (level == null) return;

                Player player = Minecraft.getInstance().level.getPlayerByUUID(playerUUID);
                if (player == null) return;

                player.getCapability(FluidPumpPlayerDataProvider.FLUID_PUMP_PLAYER_DATA).ifPresent((playerData) -> {
                    playerData.setFluidPumpPos(fluidPumpPos);
                });
            });
        });

        ctx.setPacketHandled(true);
    }
}
