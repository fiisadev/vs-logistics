package com.fiisadev.vs_logistics.utils;

import net.minecraft.world.phys.Vec3;
import org.joml.Vector3d;
import org.valkyrienskies.core.api.ships.ClientShip;
import org.valkyrienskies.core.api.ships.Ship;
import org.valkyrienskies.core.api.ships.properties.ShipTransform;

public class ShipUtils {
    private static ShipTransform getTransform(Ship ship) {
        return ship instanceof ClientShip ? ((ClientShip) ship).getRenderTransform() : ship.getTransform();
    }

    public static Vec3 worldToShip(Ship ship, Vec3 pos) {
        Vector3d t = getTransform(ship).getWorldToShip().transformPosition(new Vector3d(pos.x, pos.y, pos.z));
        return new Vec3(t.x, t.y, t.z);
    }

    public static Vec3 shipToWorld(Ship ship, Vec3 pos) {
        Vector3d t = getTransform(ship).getShipToWorld().transformPosition(new Vector3d(pos.x, pos.y, pos.z));
        return new Vec3(t.x, t.y, t.z);
    }

    public static Vec3 dirToShip(Ship ship, Vec3 dir) {
        Vector3d t = getTransform(ship).getWorldToShip().transformDirection(new Vector3d(dir.x, dir.y, dir.z));
        return new Vec3(t.x, t.y, t.z);
    }

    public static Vec3 dirToWorld(Ship ship, Vec3 dir) {
        Vector3d t = getTransform(ship).getShipToWorld().transformDirection(new Vector3d(dir.x, dir.y, dir.z));
        return new Vec3(t.x, t.y, t.z);
    }
}
