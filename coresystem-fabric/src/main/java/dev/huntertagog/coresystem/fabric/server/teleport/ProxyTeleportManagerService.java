package dev.huntertagog.coresystem.fabric.server.teleport;

import dev.huntertagog.coresystem.common.provider.ServiceProvider;
import dev.huntertagog.coresystem.fabric.common.teleport.TeleportManagerService;
import dev.huntertagog.coresystem.fabric.server.bridge.VelocityBridgeClient;
import dev.huntertagog.coresystem.platform.bridge.codec.BridgeCodec;
import dev.huntertagog.coresystem.platform.player.playerdata.PlayerDataSyncService;
import net.minecraft.server.network.ServerPlayerEntity;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public final class ProxyTeleportManagerService implements TeleportManagerService {

    private final VelocityBridgeClient bridge;
    private final String replyToNodeId;

    public ProxyTeleportManagerService(VelocityBridgeClient bridge, String replyToNodeId) {
        this.bridge = bridge;
        this.replyToNodeId = replyToNodeId;
    }

    @Override
    public boolean teleportPlayer(@NotNull ServerPlayerEntity player,
                                  @NotNull String targetServerName,
                                  String inventoryContext,
                                  String reason
    ) {

        UUID requestId = UUID.randomUUID();
        UUID playerId = player.getUuid();

        byte[] payload = BridgeCodec.encodeTeleportRequest(
                requestId,
                playerId,
                targetServerName,
                reason,
                inventoryContext,
                replyToNodeId
        );

        PlayerDataSyncService service = ServiceProvider.getService(PlayerDataSyncService.class);
        service.saveInventorySnapshot(player.getUuid(), inventoryContext, "proxy-teleport-" + requestId);

        // fire-and-forget zum Proxy (carrier player wird intern gewählt)
        return bridge.sendToProxy(payload);
    }

    @Override
    public boolean handleJoinOnThisServer(@NotNull ServerPlayerEntity player) {
        return false;
    }
}
