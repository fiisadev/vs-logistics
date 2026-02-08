package com.fiisadev.vs_logistics.network;

import com.fiisadev.vs_logistics.client.utils.HoseUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class BreakHosePacket {

    private final Vec3 startPos;
    private final Vec3 endPos;
    private final Vec3 startDir;
    private final Vec3 endDir;

    public BreakHosePacket(Vec3 startPos, Vec3 endPos, Vec3 startDir, Vec3 endDir) {
        this.startPos = startPos;
        this.endPos = endPos;
        this.startDir = startDir;
        this.endDir = endDir;
    }

    public BreakHosePacket(FriendlyByteBuf buf) {
        this.startPos = new Vec3(buf.readVector3f());
        this.endPos = new Vec3(buf.readVector3f());
        this.startDir = new Vec3(buf.readVector3f());
        this.endDir = new Vec3(buf.readVector3f());
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeVector3f(startPos.toVector3f());
        buf.writeVector3f(endPos.toVector3f());
        buf.writeVector3f(startDir.toVector3f());
        buf.writeVector3f(endDir.toVector3f());
    }

    public void handle(Supplier<NetworkEvent.Context> ctxSup) {
        NetworkEvent.Context ctx = ctxSup.get();

        ctx.enqueueWork(() -> {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
                double dist = startPos.distanceTo(endPos);

                Vec3 p1 = startPos.add(startDir.scale(dist * 0.3f));
                Vec3 p2 = endPos.subtract(endDir.scale(dist * 0.3f));

                Vec3[] segments = HoseUtils.generateHoseSegments(startPos, endPos, p1, p2, dist, new BlockPos(0, 0, 0));

                Minecraft.getInstance().player.playSound(SoundEvents.WOOL_PLACE, 1.0f, 1.0f);
                for (Vec3 segment : segments) {
                    Minecraft.getInstance().level.addParticle(
                            new BlockParticleOption(ParticleTypes.BLOCK, Blocks.BLACK_WOOL.defaultBlockState()),
                            segment.x, segment.y, segment.z,
                            0, 0.05f, 0
                    );
                }
            });
        });

        ctx.setPacketHandled(true);
    }
}
