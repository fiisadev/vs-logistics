package com.fiisadev.vs_logistics.client.utils;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3d;
import org.valkyrienskies.core.api.ships.Ship;
import org.valkyrienskies.mod.api.ValkyrienSkies;

public class HoseUtils {
    // TODO config
    public static final int SEGMENTS = 32;
    public static final int RADIAL_SEGMENTS = 8;
    public static final float RADIUS = 0.05f;

    private static Vec3 bezier(Vec3 p0, Vec3 p1, Vec3 p2, Vec3 p3, float t) {
        float u = 1 - t;
        float tt = t * t;
        float uu = u * u;
        float uuu = uu * u;
        float ttt = tt * t;

        Vec3 p = p0.scale(uuu);          // u^3 * P0
        p = p.add(p1.scale(3 * uu * t)); // 3u^2 t * P1
        p = p.add(p2.scale(3 * u * tt)); // 3u t^2 * P2
        p = p.add(p3.scale(ttt));        // t^3 * P3

        return p;
    }

    public static Vec3[] generateHoseSegments(Vec3 start, Vec3 end, Vec3 p1, Vec3 p2, double dist, BlockPos originPos) {
        Level level = Minecraft.getInstance().level;
        if (level == null) return new Vec3[SEGMENTS + 1];

        Vec3[] centers = new Vec3[SEGMENTS + 1];

        Ship ship = ValkyrienSkies.getShipManagingBlock(level, originPos);

        // 1. Generate initial Bezier curve
        for (int i = 0; i <= SEGMENTS; i++) {
            float t = (float) i / SEGMENTS;
            Vec3 point = bezier(start, p1, p2, end, t);
            float slackAmount = (float) (Math.sin(Math.PI * t) * dist * 0.2);
            centers[i] = point.add(0, -slackAmount, 0);
        }

        // 2. Terrain-aware adjustment
        for (int i = 1; i <= SEGMENTS; i++) {
            Vec3 localPoint = centers[i];
            Vec3 lastPoint = centers[i - 1];

            Vec3 worldPoint, worldLastPoint;
            if (ship != null) {
                Vector3d jomlPos = new Vector3d(
                        localPoint.x + originPos.getX(),
                        localPoint.y + originPos.getY(),
                        localPoint.z + originPos.getZ()
                );
                jomlPos = ship.getShipToWorld().transformPosition(jomlPos);
                worldPoint = new Vec3(jomlPos.x, jomlPos.y, jomlPos.z);

                Vector3d jomlLastPos = new Vector3d(
                        lastPoint.x + originPos.getX(),
                        lastPoint.y + originPos.getY(),
                        lastPoint.z + originPos.getZ()
                );
                jomlLastPos = ship.getShipToWorld().transformPosition(jomlLastPos);
                worldLastPoint = new Vec3(jomlLastPos.x, jomlLastPos.y, jomlLastPos.z);
            } else {
                worldPoint = localPoint.add(Vec3.atLowerCornerOf(originPos));
                worldLastPoint = lastPoint.add(Vec3.atLowerCornerOf(originPos));
            }

            // Raycast in World Space
            Vec3 rayStart = worldPoint.multiply(1, 0, 1).add(0, worldLastPoint.y, 0);
            Vec3 rayEnd = worldPoint.subtract(0, RADIUS, 0);

            var hit = level.clip(new ClipContext(
                    rayStart,
                    rayEnd,
                    ClipContext.Block.COLLIDER,
                    ClipContext.Fluid.NONE,
                    null
            ));

            if (hit.getType() == net.minecraft.world.phys.HitResult.Type.BLOCK) {
                Vec3 worldHitPos = hit.getLocation();
                Vec3 adjustedLocal;

                if (ship != null) {
                    // Convert World Hit back to Ship Local
                    Vector3d jomlHit = new Vector3d(worldHitPos.x, worldHitPos.y, worldHitPos.z);
                    ship.getWorldToShip().transformPosition(jomlHit);
                    // Subtract origin to get back to renderer-relative space
                    adjustedLocal = new Vec3(
                            jomlHit.x - originPos.getX(),
                            jomlHit.y - originPos.getY() + RADIUS,
                            jomlHit.z - originPos.getZ()
                    );
                } else {
                    adjustedLocal = new Vec3(
                            worldHitPos.x - originPos.getX(),
                            worldHitPos.y - originPos.getY() + RADIUS,
                            worldHitPos.z - originPos.getZ()
                    );
                }
                centers[i] = adjustedLocal;
            }
        }

        // 3. Smooth the curve to remove sharp kinks
        Vec3[] smoothed = new Vec3[SEGMENTS + 1];
        for (int i = 0; i <= SEGMENTS; i++) {
            Vec3 prev = i == 0 ? centers[i] : centers[i - 1];
            Vec3 next = i == SEGMENTS ? centers[i] : centers[i + 1];
            smoothed[i] = new Vec3(
                    centers[i].x,
                    (prev.y + centers[i].y + next.y) / 3f,
                    centers[i].z
            );
        }
        centers = smoothed;

        return centers;
    }
}

