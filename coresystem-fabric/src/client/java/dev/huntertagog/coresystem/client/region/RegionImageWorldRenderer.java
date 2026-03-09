package dev.huntertagog.coresystem.client.region;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.huntertagog.coresystem.common.log.Logger;
import dev.huntertagog.coresystem.common.log.LoggerFactory;
import dev.huntertagog.coresystem.fabric.common.region.RegionImageDef;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

public final class RegionImageWorldRenderer {

    private static Logger LOG = LoggerFactory.get("RegionImageWorldRenderer");

    private static final float EPS = 0.01f;
    private static final double MAX_RENDER_DISTANCE = 96.0; // Business-Default, nach Bedarf

    private RegionImageWorldRenderer() {
    }

    public static void init() {
        WorldRenderEvents.LAST.register(RegionImageWorldRenderer::renderAll);
    }

    private static void renderAll(WorldRenderContext ctx) {
        var client = MinecraftClient.getInstance();
        if (client.world == null) return;

        var frustum = ctx.frustum();
        var camera = ctx.camera();
        Vec3d camPos = camera.getPos();

        for (RegionImageDef def : RegionImageClientCache.snapshot().values()) {
            if (!def.worldId().equals(client.world.getRegistryKey().getValue())) continue;

            // 1) Frustum Box (leicht “aufblasen”, damit nix flackert)
            Box box = def.bounds().expand(0.25);

            if (frustum != null && !frustum.isVisible(box)) continue;

            // 2) Distanz-Gate (Kosten senken)
            Vec3d center = box.getCenter();
            if (center.squaredDistanceTo(camPos) > (MAX_RENDER_DISTANCE * MAX_RENDER_DISTANCE)) continue;

            renderOne(ctx, def, camPos);
        }
        var consumers = ctx.consumers();
        if (consumers instanceof VertexConsumerProvider.Immediate immediate) {
            immediate.draw(); // <- Ohne das bleibt es oft “unsichtbar”
        }
    }

    private static void renderOne(WorldRenderContext ctx, RegionImageDef def, Vec3d camPos) {
        Identifier tex = RegionImageClientMapper.textureIdentifier(String.valueOf(def.textureId()));
        LOG.info("[RegionImage] Binding texture {}", tex);

        MatrixStack matrices = ctx.matrixStack();

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        // MVP: depth test optional – du kannst das später togglen
        // RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);

        RenderLayer layer = RenderLayer.getEntityTranslucentCull(tex);
        VertexConsumerProvider consumers = ctx.consumers();
        assert consumers != null;
        VertexConsumer vc = consumers.getBuffer(layer);

        assert matrices != null;
        matrices.push();
        matrices.translate(-camPos.x, -camPos.y, -camPos.z);

        drawRegionQuad(matrices, vc, def);

        matrices.pop();

        RenderSystem.depthMask(true);
        RenderSystem.disableBlend();
    }

    private static void drawRegionQuad(MatrixStack matrices, VertexConsumer vc, RegionImageDef def) {
        final int light = 0xF000F0; // fullbright

        // bounds kommen bei dir als double
        final double minX = def.bounds().minX;
        final double minY = def.bounds().minY;
        final double minZ = def.bounds().minZ;
        final double maxX = def.bounds().maxX;
        final double maxY = def.bounds().maxY;
        final double maxZ = def.bounds().maxZ;

        final Matrix4f mat = matrices.peek().getPositionMatrix();

        // -------------------------
        // FLOOR (oben korrekt, unten UV-flip)
        // -------------------------
        if (def.mode() == dev.huntertagog.coresystem.common.region.RegionImageMode.FLOOR) {
            final float yTop = (float) def.baseY() + EPS;
            final float yBottom = yTop - EPS * 2f;

            final float xL = (float) minX;
            final float xR = (float) maxX + 1f;
            final float zN = (float) minZ;
            final float zS = (float) maxZ + 1f;

            // TOP (normal +Y) UV normal
            quadUv(vc, mat,
                    xL, yTop, zN, 0f, 0f,
                    xR, yTop, zN, 1f, 0f,
                    xR, yTop, zS, 1f, 1f,
                    xL, yTop, zS, 0f, 1f,
                    light, 0f, 1f, 0f);

            // BOTTOM (normal -Y) -> U flip (1-u)
            quadUv(vc, mat,
                    xL, yBottom, zN, 1f, 0f,
                    xL, yBottom, zS, 1f, 1f,
                    xR, yBottom, zS, 0f, 1f,
                    xR, yBottom, zN, 0f, 0f,
                    light, 0f, -1f, 0f);

            return;
        }

        // -------------------------
        // WALL (Front normal, Back U-flip)
        // -------------------------
        Direction facing = Direction.byName(String.valueOf(def.facing()));
        if (facing == null || facing.getAxis().isVertical()) facing = Direction.NORTH;

        final float xL = (float) minX;
        final float xR = (float) maxX + 1f;
        final float yB = (float) minY;
        final float yT = (float) maxY + 1f;
        final float zN = (float) minZ;
        final float zS = (float) maxZ + 1f;

        switch (facing) {
            case NORTH -> {
                final float zFront = zN - EPS;
                final float zBack = zFront + EPS * 2f;

                // FRONT (normal +Z)
                quadUv(vc, mat,
                        xL, yT, zFront, 0f, 0f,
                        xR, yT, zFront, 1f, 0f,
                        xR, yB, zFront, 1f, 1f,
                        xL, yB, zFront, 0f, 1f,
                        light, 0f, 0f, 1f);

                // BACK (normal -Z) -> U flip
                quadUv(vc, mat,
                        xL, yB, zBack, 1f, 1f,
                        xR, yB, zBack, 0f, 1f,
                        xR, yT, zBack, 0f, 0f,
                        xL, yT, zBack, 1f, 0f,
                        light, 0f, 0f, -1f);
            }

            case SOUTH -> {
                final float zFront = zS + EPS;
                final float zBack = zFront - EPS * 2f;

                // FRONT (normal -Z) (Player sieht SOUTH-Seite)
                quadUv(vc, mat,
                        xR, yT, zFront, 0f, 0f,
                        xL, yT, zFront, 1f, 0f,
                        xL, yB, zFront, 1f, 1f,
                        xR, yB, zFront, 0f, 1f,
                        light, 0f, 0f, -1f);

                // BACK (normal +Z) -> U flip
                quadUv(vc, mat,
                        xR, yB, zBack, 1f, 1f,
                        xL, yB, zBack, 0f, 1f,
                        xL, yT, zBack, 0f, 0f,
                        xR, yT, zBack, 1f, 0f,
                        light, 0f, 0f, 1f);
            }

            case WEST -> {
                final float xFront = xL - EPS;
                final float xBack = xFront + EPS * 2f;

                // FRONT (normal +X)
                quadUv(vc, mat,
                        xFront, yT, zN, 0f, 0f,
                        xFront, yT, zS, 1f, 0f,
                        xFront, yB, zS, 1f, 1f,
                        xFront, yB, zN, 0f, 1f,
                        light, 1f, 0f, 0f);

                // BACK (normal -X) -> U flip
                quadUv(vc, mat,
                        xBack, yB, zN, 1f, 1f,
                        xBack, yB, zS, 0f, 1f,
                        xBack, yT, zS, 0f, 0f,
                        xBack, yT, zN, 1f, 0f,
                        light, -1f, 0f, 0f);
            }

            case EAST -> {
                final float xFront = xR + EPS;
                final float xBack = xFront - EPS * 2f;

                // FRONT (normal -X)
                quadUv(vc, mat,
                        xFront, yT, zS, 0f, 0f,
                        xFront, yT, zN, 1f, 0f,
                        xFront, yB, zN, 1f, 1f,
                        xFront, yB, zS, 0f, 1f,
                        light, -1f, 0f, 0f);

                // BACK (normal +X) -> U flip
                quadUv(vc, mat,
                        xBack, yB, zS, 1f, 1f,
                        xBack, yB, zN, 0f, 1f,
                        xBack, yT, zN, 0f, 0f,
                        xBack, yT, zS, 1f, 0f,
                        light, 1f, 0f, 0f);
            }
        }
    }

    private static void quadUv(VertexConsumer vc,
                               Matrix4f mat,
                               float x1, float y1, float z1, float u1, float v1,
                               float x2, float y2, float z2, float u2, float v2,
                               float x3, float y3, float z3, float u3, float v3,
                               float x4, float y4, float z4, float u4, float v4,
                               int light,
                               float nx, float ny, float nz) {

        vc.vertex(mat, x1, y1, z1).color(255, 255, 255, 255).texture(u1, v1).overlay(OverlayTexture.DEFAULT_UV).light(light).normal(nx, ny, nz);
        vc.vertex(mat, x2, y2, z2).color(255, 255, 255, 255).texture(u2, v2).overlay(OverlayTexture.DEFAULT_UV).light(light).normal(nx, ny, nz);
        vc.vertex(mat, x3, y3, z3).color(255, 255, 255, 255).texture(u3, v3).overlay(OverlayTexture.DEFAULT_UV).light(light).normal(nx, ny, nz);
        vc.vertex(mat, x4, y4, z4).color(255, 255, 255, 255).texture(u4, v4).overlay(OverlayTexture.DEFAULT_UV).light(light).normal(nx, ny, nz);
    }
}
