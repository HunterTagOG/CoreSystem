package dev.huntertagog.coresystem.client.region;

import com.google.common.collect.Lists;
import dev.huntertagog.coresystem.common.log.Logger;
import dev.huntertagog.coresystem.common.log.LoggerFactory;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;

import java.util.List;

public final class ClientRegionOutlineManager {

    private static final Logger LOG = LoggerFactory.get("RegionOutlineClient");

    private static ClientRegionOutlineManager INSTANCE;

    private static final class Outline {
        final Identifier worldId;
        final Box box;
        final int color;
        long expireAt; // world time in ticks

        Outline(Identifier worldId, Box box, int color, long expireAt) {
            this.worldId = worldId;
            this.box = box;
            this.color = color;
            this.expireAt = expireAt;
        }
    }

    private final List<Outline> outlines = Lists.newCopyOnWriteArrayList();

    private ClientRegionOutlineManager() {
        WorldRenderEvents.AFTER_ENTITIES.register(this::onRender);
    }

    public static void init() {
        if (INSTANCE == null) {
            INSTANCE = new ClientRegionOutlineManager();
            LOG.info("ClientRegionOutlineManager initialized.");
        }
    }

    public static ClientRegionOutlineManager getInstance() {
        return INSTANCE;
    }

    public void addOutline(Identifier worldId,
                           BlockPos min,
                           BlockPos max,
                           int colorArgb,
                           int durationTicks) {

        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.world == null) return;

        int minX = Math.min(min.getX(), max.getX());
        int minY = Math.min(min.getY(), max.getY());
        int minZ = Math.min(min.getZ(), max.getZ());
        int maxX = Math.max(min.getX(), max.getX()) + 1;
        int maxY = Math.max(min.getY(), max.getY()) + 1;
        int maxZ = Math.max(min.getZ(), max.getZ()) + 1;

        Box box = new Box(minX, minY, minZ, maxX, maxY, maxZ);
        long now = mc.world.getTime();
        long expireAt = now + durationTicks;

        outlines.add(new Outline(worldId, box, colorArgb, expireAt));
    }

    private void onRender(WorldRenderContext ctx) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.world == null || mc.player == null) return;

        Identifier currentWorldId = mc.world.getRegistryKey().getValue();
        long time = mc.world.getTime();

        Camera camera = ctx.camera();
        MatrixStack matrices = ctx.matrixStack();

        // consumers() ist als VertexConsumerProvider getypt
        VertexConsumerProvider provider = ctx.consumers();
        if (!(provider instanceof VertexConsumerProvider.Immediate vcp)) {
            // sollte in der Praxis nie passieren, aber defensiv.
            return;
        }

        VertexConsumer consumer = vcp.getBuffer(RenderLayer.getLines());

        double camX = camera.getPos().x;
        double camY = camera.getPos().y;
        double camZ = camera.getPos().z;

        // abgelaufene Outlines rauswerfen
        outlines.removeIf(o -> o.expireAt <= time);

        for (Outline o : outlines) {
            if (!o.worldId.equals(currentWorldId)) continue;

            assert matrices != null;
            matrices.push();

            double x1 = o.box.minX - camX;
            double y1 = o.box.minY - camY;
            double z1 = o.box.minZ - camZ;
            double x2 = o.box.maxX - camX;
            double y2 = o.box.maxY - camY;
            double z2 = o.box.maxZ - camZ;

            float a = ((o.color >> 24) & 0xFF) / 255.0f;
            float r = ((o.color >> 16) & 0xFF) / 255.0f;
            float g = ((o.color >> 8) & 0xFF) / 255.0f;
            float b = (o.color & 0xFF) / 255.0f;

            WorldRenderer.drawBox(matrices, consumer, x1, y1, z1, x2, y2, z2, r, g, b, a);
            matrices.pop();
        }

        // Buffer flushen
        vcp.draw();
    }
}
