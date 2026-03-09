package dev.huntertagog.coresystem.velocity.net;

import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import dev.huntertagog.coresystem.platform.bridge.BridgeMessageType;
import dev.huntertagog.coresystem.platform.bridge.codec.BridgeCodec;
import dev.huntertagog.coresystem.velocity.node.NodeRouter;

import java.util.Optional;

public final class ResponseDispatcher {

    private final ProxyServer proxy;
    private final NodeRouter router;

    public ResponseDispatcher(ProxyServer proxy, NodeRouter router) {
        this.proxy = proxy;
        this.router = router;
    }

    public boolean dispatchResponse(byte[] payload) {
        if (payload == null || payload.length == 0) return false;

        BridgeMessageType type;
        try {
            type = BridgeMessageType.fromId(payload[0]);
        } catch (Exception ignored) {
            return false;
        }

        return switch (type) {
            case PREPARE_RESPONSE -> {
                var resp = BridgeCodec.decodePrepareResponse(payload);
                yield sendToNode(resp.replyToNodeId(), payload);
            }
            case ISLAND_DELETE_RESPONSE -> {
                var resp = BridgeCodec.decodeIslandDeleteResponse(payload);
                yield sendToNode(resp.replyToNodeId(), payload);
            }
            default -> false;
        };
    }

    private boolean sendToNode(String replyToNodeId, byte[] payload) {
        Optional<String> targetServer = router.resolveServerName(replyToNodeId);
        if (targetServer.isEmpty()) return false;

        Optional<RegisteredServer> srv = proxy.getServer(targetServer.get());
        return srv.map(s -> s.sendPluginMessage(BridgeChannel.IDENTIFIER, payload)).orElse(false);
    }
}
