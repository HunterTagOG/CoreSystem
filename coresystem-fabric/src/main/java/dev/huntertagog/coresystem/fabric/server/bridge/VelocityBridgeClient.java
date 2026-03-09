package dev.huntertagog.coresystem.fabric.server.bridge;

import dev.huntertagog.coresystem.fabric.common.net.payload.BridgePayload;
import dev.huntertagog.coresystem.platform.bridge.BridgeMessageType;
import dev.huntertagog.coresystem.platform.bridge.codec.BridgeCodec;
import dev.huntertagog.coresystem.platform.provider.Service;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.Optional;

public final class VelocityBridgeClient implements Service {

    private final MinecraftServer server;
    private final PrivateIslandPrepareResponseBus responseBus;

    public VelocityBridgeClient(
            MinecraftServer server,
            PrivateIslandPrepareResponseBus responseBus
    ) {
        this.server = server;
        this.responseBus = responseBus;
    }

    /**
     * Inbound: Nachrichten vom Velocity-Proxy
     * Erwartet rohe byte[] Payloads nach platform-Codec
     */
    public void initInbound() {
        ServerPlayNetworking.registerGlobalReceiver(BridgePayload.ID, (payload, context) -> {
            byte[] data = payload.bytes();

            // Minimal Peek auf Typ
            BridgeMessageType type = BridgeMessageType.fromId(data[0]);
            if (type != BridgeMessageType.PREPARE_RESPONSE) return;

            BridgeCodec.PrepareResponse resp =
                    BridgeCodec.decodePrepareResponse(data);

            // Completion immer im Server-Thread
            context.server().execute(() -> responseBus.complete(resp));
        });
    }

    /**
     * Outbound: PacketByteBuf -> byte[]
     */
    public boolean sendToProxy(PacketByteBuf buf) {
        byte[] data = new byte[buf.readableBytes()];
        buf.getBytes(0, data);
        return sendToProxy(data);
    }

    /**
     * Outbound: rohe platform-Payloads
     */
    public boolean sendToProxy(byte[] data) {
        Optional<ServerPlayerEntity> carrier = pickCarrier();
        if (carrier.isEmpty()) return false;

        ServerPlayNetworking.send(
                carrier.get(),
                new BridgePayload(data)
        );
        return true;
    }

    /**
     * Aktuell simplest möglicher Carrier:
     * irgendein verbundener Player
     * <p>
     * (Architektur erlaubt späteres Hardening)
     */
    private Optional<ServerPlayerEntity> pickCarrier() {
        return server.getPlayerManager()
                .getPlayerList()
                .stream()
                .findFirst();
    }
}
