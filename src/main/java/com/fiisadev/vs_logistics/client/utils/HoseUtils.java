package com.fiisadev.vs_logistics.client.utils;

import com.fiisadev.vs_logistics.content.fluid_pump.FluidPumpBlock;
import com.fiisadev.vs_logistics.content.fluid_pump.FluidPumpBlockEntity;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

public class HoseUtils {
    // TODO config
    public static final int SEGMENTS = 32;
    public static final int RADIAL_SEGMENTS = 8;
    public static final float RADIUS = 0.05f;

    public static Vec3 getNozzleHandlePosition(Player player, float partialTicks){
        Minecraft minecraft = Minecraft.getInstance();
        float bodyRotation = player.yBodyRotO + (player.yBodyRot - player.yBodyRotO) * partialTicks;

        if(player.equals(minecraft.player) && minecraft.options.getCameraType() == CameraType.FIRST_PERSON)
        {
            return new Vec3(-0.5, 1, -3).yRot((float)Math.toRadians(-bodyRotation));
        }

        Vec3 nozzlePos = new Vec3(-0.38, 0.783, -0.03);

        if(player instanceof AbstractClientPlayer ap && "slim".equals(ap.getModelName()))
            nozzlePos = nozzlePos.add(0.03, -0.03, 0.0);

        if (player.isCrouching())
            nozzlePos = nozzlePos.add(0, -0.33, 0);

        return nozzlePos.yRot((float)Math.toRadians(-bodyRotation));
    }

    public static Vec3 getNozzleHandleDir(Player player, float partialTicks){
        float bodyRotation = player.yBodyRotO + (player.yBodyRot - player.yBodyRotO) * partialTicks;
        return new Vec3(0, 0, 1)
                .yRot((float)Math.toRadians(-bodyRotation))
                .normalize();
    }

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

    public static Vec3[] generateHoseSegments(Vec3 start, Vec3 end, Vec3 p1, Vec3 p2, double dist) {
        Level level = Minecraft.getInstance().level;
        Vec3[] centers = new Vec3[SEGMENTS + 1];

        // 1. Generate initial Bezier curve with slack
        for (int i = 0; i <= SEGMENTS; i++) {
            float t = (float) i / SEGMENTS;
            Vec3 point = bezier(start, p1, p2, end, t);

            float slackAmount = (float) (Math.sin(Math.PI * t) * dist * 0.2);
            point = point.add(0, -slackAmount, 0);

            centers[i] = point;
        }

        // 2. Terrain-aware adjustment
        for (int i = 1; i <= SEGMENTS; i++) {
            Vec3 point = centers[i];

            Vec3 rayStart = new Vec3(point.x, centers[i - 1].y, point.z);
            Vec3 rayEnd = point.add(0, -RADIUS, 0);

            var hit = level.clip(new ClipContext(
                    rayStart,
                    rayEnd,
                    ClipContext.Block.COLLIDER,
                    ClipContext.Fluid.NONE,
                    null
            ));

            if (hit.getType() == net.minecraft.world.phys.HitResult.Type.BLOCK) {
                // Clamp to ground with a small offset (hose radius)
                point = new Vec3(point.x, hit.getLocation().y + RADIUS, point.z);
                centers[i] = point;
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

