package dev.huntertagog.coresystem.velocity.net;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.PluginMessageEvent;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import dev.huntertagog.coresystem.common.log.Logger;
import dev.huntertagog.coresystem.common.log.LoggerFactory;
import dev.huntertagog.coresystem.platform.bridge.BridgeMessageType;
import dev.huntertagog.coresystem.platform.bridge.codec.BridgeCodec;
import dev.huntertagog.coresystem.velocity.node.NodeRouter;

import java.util.Optional;

public final class VelocityBridge {

    private final ResponseDispatcher responseDispatcher;
    private final ProxyServer proxy;

    private static final Logger log = LoggerFactory.get("ResponseDispatcher");

    public VelocityBridge(ProxyServer proxy, NodeRouter nodeRouter) {
        this.responseDispatcher = new ResponseDispatcher(proxy, nodeRouter);
        this.proxy = proxy;
    }

    @Subscribe
    public void onPluginMessage(PluginMessageEvent e) {
        if (!e.getIdentifier().equals(BridgeChannel.IDENTIFIER)) return;

        // wir handeln das selbst (kein Forward)
        e.setResult(PluginMessageEvent.ForwardResult.handled());

        byte[] data = e.getData();
        if (data.length == 0) return;

        BridgeMessageType type;
        try {
            type = BridgeMessageType.fromId(data[0]);
        } catch (Exception ignored) {
            return;
        }

        if (type == BridgeMessageType.TELEPORT_REQUEST) {
            handleTeleportRequest(data);
        }

        // Response-Flow: Router entscheidet, wohin das Paket muss
        boolean handled = responseDispatcher.dispatchResponse(data);

        // Optional: wenn nicht handled, später Request-Handling / Logging
        // if (!handled) { ... }
    }

    private void handleTeleportRequest(byte[] payload) {
        BridgeCodec.TeleportRequest req;
        try {
            req = BridgeCodec.decodeTeleportRequest(payload);
        } catch (Exception ex) {
            log.warn("[CoreSystemVelocity] TELEPORT_REQUEST decode failed: {}", ex.getMessage());
            return;
        }

        var playerOpt = proxy.getPlayer(req.playerId());
        if (playerOpt.isEmpty()) {
            log.debug("[CoreSystemVelocity] TELEPORT_REQUEST: player not online {}", req.playerId());
            return;
        }

        Optional<RegisteredServer> target = proxy.getServer(req.targetServerName());
        if (target.isEmpty()) {
            log.warn("[CoreSystemVelocity] TELEPORT_REQUEST: target server '{}' not found", req.targetServerName());
            return;
        }

        // Business-Log (optional, aber hilfreich im Betrieb)
        log.info("[CoreSystemVelocity] Teleport player={} -> server={} reason={} invCtx={}",
                playerOpt.get().getUsername(),
                req.targetServerName(),
                req.reason(),
                req.inventoryContext()
        );

        // Switch ausführen
        playerOpt.get()
                .createConnectionRequest(target.get())
                .fireAndForget();
    }
}
