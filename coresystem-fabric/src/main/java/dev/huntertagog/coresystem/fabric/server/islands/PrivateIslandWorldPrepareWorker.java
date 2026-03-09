package dev.huntertagog.coresystem.fabric.server.islands;

import dev.huntertagog.coresystem.common.error.CoreError;
import dev.huntertagog.coresystem.common.error.CoreErrorCode;
import dev.huntertagog.coresystem.common.error.CoreErrorSeverity;
import dev.huntertagog.coresystem.common.log.Logger;
import dev.huntertagog.coresystem.common.log.LoggerFactory;
import dev.huntertagog.coresystem.fabric.common.net.payload.BridgePayload;
import dev.huntertagog.coresystem.fabric.server.bridge.VelocityBridgeClient;
import dev.huntertagog.coresystem.platform.bridge.BridgeMessageType;
import dev.huntertagog.coresystem.platform.bridge.codec.BridgeCodec;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

import java.util.UUID;

public final class PrivateIslandWorldPrepareWorker {

    private static final Logger LOG = LoggerFactory.get("IslandPrepareWorker");

    private final VelocityBridgeClient bridge;
    private final String nodeId;

    public PrivateIslandWorldPrepareWorker(VelocityBridgeClient bridge) {
        this.bridge = bridge;
        this.nodeId = System.getenv().getOrDefault("SERVER_ID", "unknown-node");
    }

    /**
     * Inbound: PREPARE_REQUEST kommt als BridgePayload rein (via Velocity PluginMessage).
     * Worker: nur ausführen + Response zurück zum Proxy schicken.
     * Routing übernimmt Velocity über replyToNodeId.
     */
    public void initInbound() {
        ServerPlayNetworking.registerGlobalReceiver(BridgePayload.ID, (payload, context) -> {
            // wir nehmen die Bytes direkt (Codec ist platform-api ByteBuffer based)
            final byte[] data = payload.bytes();

            // minimal sanity check
            if (data == null || data.length < 1) {
                CoreError.of(
                                CoreErrorCode.ISLAND_PREPARE_WORKER_INVALID_MESSAGE,
                                CoreErrorSeverity.WARN,
                                "Invalid island prepare payload (empty)."
                        )
                        .withContextEntry("nodeId", nodeId)
                        .log();
                return;
            }

            // nur PREPARE_REQUEST interessiert diesen Worker
            BridgeMessageType t;
            try {
                t = BridgeMessageType.fromId(data[0]);
            } catch (Exception ex) {
                CoreError.of(
                                CoreErrorCode.ISLAND_PREPARE_WORKER_INVALID_MESSAGE,
                                CoreErrorSeverity.WARN,
                                "Invalid island prepare payload (unknown type)."
                        )
                        .withCause(ex)
                        .withContextEntry("nodeId", nodeId)
                        .withContextEntry("typeByte", String.valueOf(data[0]))
                        .log();
                return;
            }

            if (t != BridgeMessageType.PREPARE_REQUEST) {
                return; // nicht unser Thema (Responses handled der ResponseBus inbound)
            }

            // Ab hier: nur noch Server-Thread
            context.server().execute(() -> handlePrepareRequest(context.server(), data));
        });
    }

    private void handlePrepareRequest(net.minecraft.server.MinecraftServer server, byte[] payload) {
        final BridgeCodec.PrepareRequest req;
        try {
            req = BridgeCodec.decodePrepareRequest(payload);
        } catch (Exception e) {
            CoreError.of(
                            CoreErrorCode.ISLAND_PREPARE_WORKER_INVALID_MESSAGE,
                            CoreErrorSeverity.WARN,
                            "Failed to decode PREPARE_REQUEST."
                    )
                    .withCause(e)
                    .withContextEntry("nodeId", nodeId)
                    .log();
            return;
        }

        final UUID requestId = req.requestId();
        final UUID ownerId = req.ownerId();
        final String replyToNodeId = req.replyToNodeId();

        LOG.info("Received PREPARE_REQUEST requestId='{}' ownerId='{}' targetNodeId='{}' replyToNodeId='{}'",
                requestId, ownerId, req.targetNodeId(), replyToNodeId
        );

        final PrivateIslandWorldManager manager = PrivateIslandWorldManager.get(server);

        // Island vorbereiten (Manager macht intern die Thread-Wechsel korrekt)
        manager.getOrCreateIslandWorld(ownerId).whenComplete((world, throwable) -> {
            final boolean ok = (throwable == null && world != null);
            final String err = ok ? null : safeErr(throwable, world);

            final byte[] responseBytes;
            try {
                responseBytes = BridgeCodec.encodePrepareResponse(
                        requestId,
                        ok,
                        err,
                        replyToNodeId
                );
            } catch (Exception e) {
                CoreError.of(
                                CoreErrorCode.ISLAND_PREPARE_WORKER_PUBLISH_FAILED,
                                CoreErrorSeverity.ERROR,
                                "Failed to encode PREPARE_RESPONSE."
                        )
                        .withCause(e)
                        .withContextEntry("nodeId", nodeId)
                        .withContextEntry("requestId", requestId.toString())
                        .withContextEntry("ownerId", ownerId.toString())
                        .withContextEntry("replyToNodeId", replyToNodeId)
                        .log();
                return;
            }

            // Send muss sauber im Server-Thread laufen (Carrier-Player + networking)
            server.execute(() -> {
                boolean sent = bridge.sendToProxy(responseBytes);
                if (!sent) {
                    CoreError.of(
                                    CoreErrorCode.ISLAND_PREPARE_WORKER_PUBLISH_FAILED,
                                    CoreErrorSeverity.WARN,
                                    "Failed to send PREPARE_RESPONSE to proxy (no carrier / send failed)."
                            )
                            .withContextEntry("nodeId", nodeId)
                            .withContextEntry("requestId", requestId.toString())
                            .withContextEntry("ownerId", ownerId.toString())
                            .withContextEntry("replyToNodeId", replyToNodeId)
                            .log();
                    return;
                }

                if (ok) {
                    LOG.info("Island prepared successfully for owner '{}' (world='{}') requestId='{}'.",
                            ownerId,
                            world.getRegistryKey().getValue(),
                            requestId
                    );
                } else {
                    CoreError.of(
                                    CoreErrorCode.ISLAND_PREPARE_WORKER_WORLD_FAILED,
                                    CoreErrorSeverity.WARN,
                                    "Island prepare failed while creating/loading world."
                            )
                            .withContextEntry("nodeId", nodeId)
                            .withContextEntry("ownerId", ownerId.toString())
                            .withContextEntry("requestId", requestId.toString())
                            .withContextEntry("replyToNodeId", replyToNodeId)
                            .withContextEntry("error", String.valueOf(err))
                            .withCause(throwable)
                            .log();
                }
            });
        });
    }

    private static String safeErr(Throwable throwable, Object world) {
        if (throwable != null) {
            String msg = throwable.getMessage();
            return (msg == null || msg.isBlank())
                    ? ("PREPARE_FAILED:" + throwable.getClass().getSimpleName())
                    : msg;
        }
        if (world == null) return "PREPARE_FAILED:world-null";
        return "PREPARE_FAILED:unknown";
    }
}
