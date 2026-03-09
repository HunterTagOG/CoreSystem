package dev.huntertagog.coresystem.fabric.server.islands;

import dev.huntertagog.coresystem.fabric.server.bridge.PrivateIslandPrepareResponseBus;
import dev.huntertagog.coresystem.fabric.server.bridge.VelocityBridgeClient;
import dev.huntertagog.coresystem.platform.bridge.codec.BridgeCodec;

import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public final class PrivateIslandWorldPrepareClient {

    private final VelocityBridgeClient bridge;
    private final PrivateIslandPrepareResponseBus bus;

    public PrivateIslandWorldPrepareClient(
            VelocityBridgeClient bridge,
            PrivateIslandPrepareResponseBus bus
    ) {
        this.bridge = bridge;
        this.bus = bus;
    }

    /**
     * Low-Level: Request mit expliziter requestId
     */
    public CompletableFuture<BridgeCodec.PrepareResponse> sendPrepare(
            UUID ownerId,
            String targetNodeId,
            UUID requestId
    ) {
        String replyToNodeId = System.getenv("SERVER_ID");

        var future = bus.register(requestId, Duration.ofSeconds(30));

        byte[] payload = BridgeCodec.encodePrepareRequest(
                requestId,
                ownerId,
                targetNodeId,
                replyToNodeId
        );

        boolean sent = bridge.sendToProxy(payload);
        if (!sent) {
            future.complete(
                    new BridgeCodec.PrepareResponse(
                            requestId,
                            false,
                            "NO_CARRIER_PLAYER",
                            replyToNodeId
                    )
            );
        }

        return future;
    }

    /**
     * High-Level Convenience API
     */
    public CompletableFuture<BridgeCodec.PrepareResponse> requestPrepare(
            UUID ownerId,
            String targetNodeId
    ) {
        UUID requestId = UUID.randomUUID();
        String replyToNodeId = System.getenv("SERVER_ID");

        var future = bus.register(requestId, Duration.ofSeconds(20));

        byte[] payload = BridgeCodec.encodePrepareRequest(
                requestId,
                ownerId,
                targetNodeId,
                replyToNodeId
        );

        boolean sent = bridge.sendToProxy(payload);
        if (!sent) {
            future.completeExceptionally(
                    new IllegalStateException("No carrier player / could not send to proxy")
            );
        }

        return future;
    }
}
