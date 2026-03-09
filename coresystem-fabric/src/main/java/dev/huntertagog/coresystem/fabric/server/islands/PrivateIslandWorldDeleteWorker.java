package dev.huntertagog.coresystem.fabric.server.islands;

import dev.huntertagog.coresystem.fabric.common.net.payload.BridgePayload;
import dev.huntertagog.coresystem.fabric.server.bridge.VelocityBridgeClient;
import dev.huntertagog.coresystem.platform.bridge.BridgeMessageType;
import dev.huntertagog.coresystem.platform.bridge.codec.BridgeCodec;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

import static dev.huntertagog.coresystem.fabric.server.command.impl.PrivateIslandCommand.deleteIslandForOwner;

public final class PrivateIslandWorldDeleteWorker {

    private final VelocityBridgeClient bridge;

    public PrivateIslandWorldDeleteWorker(VelocityBridgeClient bridge) {
        this.bridge = bridge;
    }

    public void initInbound() {
        ServerPlayNetworking.registerGlobalReceiver(BridgePayload.ID, (payload, context) -> {

            byte[] data = payload.bytes();
            if (data == null || data.length == 0) return;

            BridgeMessageType t;
            try {
                t = BridgeMessageType.fromId(data[0]);
            } catch (Exception ignored) {
                return;
            }

            if (t != BridgeMessageType.ISLAND_DELETE_REQUEST) return;

            // decode
            BridgeCodec.IslandDeleteRequest req = BridgeCodec.decodeIslandDeleteRequest(data);

            // Work im Server-Thread
            context.server().execute(() -> {
                boolean ok = true;
                String err = null;

                try {
                    deleteIslandForOwner(context.server(), req.ownerId(), context.server().getCommandSource());
                } catch (Exception ex) {
                    ok = false;
                    err = "DELETE_FAILED:" + ex.getClass().getSimpleName();
                }

                // Response wird an replyToNodeId adressiert (Velocity routet)
                byte[] resp = BridgeCodec.encodeIslandDeleteResponse(
                        req.requestId(),
                        ok,
                        err,
                        req.replyToNodeId()
                );

                bridge.sendToProxy(resp);
            });
        });
    }
}
