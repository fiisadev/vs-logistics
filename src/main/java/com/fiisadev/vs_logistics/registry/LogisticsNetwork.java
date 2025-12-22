package com.fiisadev.vs_logistics.registry;

import com.fiisadev.vs_logistics.VSLogistics;
import com.fiisadev.vs_logistics.network.BreakHosePacket;
import com.fiisadev.vs_logistics.network.FluidPortPacket;
import com.fiisadev.vs_logistics.network.SyncFluidPumpPlayerCapPacket;
import com.fiisadev.vs_logistics.network.FluidPumpUsePacket;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

public class LogisticsNetwork {
    private static final String PROTOCOL = "1";
    public static SimpleChannel CHANNEL;

    private static int id = 0;

    public static void register() {
        CHANNEL = NetworkRegistry.newSimpleChannel(
                VSLogistics.asResource("main"),
                () -> PROTOCOL,
                PROTOCOL::equals,
                PROTOCOL::equals
        );

        CHANNEL.registerMessage(
                id++,
                SyncFluidPumpPlayerCapPacket.class,
                SyncFluidPumpPlayerCapPacket::toBytes,
                SyncFluidPumpPlayerCapPacket::new,
                SyncFluidPumpPlayerCapPacket::handle
        );

        CHANNEL.registerMessage(
                id++,
                FluidPumpUsePacket.class,
                FluidPumpUsePacket::toBytes,
                FluidPumpUsePacket::new,
                FluidPumpUsePacket::handle
        );

        CHANNEL.registerMessage(
                id++,
                FluidPortPacket.class,
                FluidPortPacket::toBytes,
                FluidPortPacket::new,
                FluidPortPacket::handle
        );

        CHANNEL.registerMessage(
                id++,
                BreakHosePacket.class,
                BreakHosePacket::toBytes,
                BreakHosePacket::new,
                BreakHosePacket::handle
        );
    }
}
