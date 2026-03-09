package dev.huntertagog.coresystem.fabric.server.player.playerdata;

import dev.huntertagog.coresystem.fabric.common.player.playerdata.PlayerInventoryCodec;
import dev.huntertagog.coresystem.platform.player.playerdata.PlayerDataCodec;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;

import java.io.IOException;
import java.util.Objects;
import java.util.UUID;

public final class FabricPlayerDataCodec implements PlayerDataCodec {

    private final MinecraftServer server;

    public FabricPlayerDataCodec(MinecraftServer server) {
        this.server = Objects.requireNonNull(server, "server");
    }

    @Override
    public String encodeInventoryToBase64(UUID playerId) throws IOException {
        ServerPlayerEntity player = server.getPlayerManager().getPlayer(playerId);
        if (player == null) {
            throw new IllegalStateException("Player not online: " + playerId);
        }
        return PlayerInventoryCodec.encodeInventoryToBase64(server, player);
    }

    @Override
    public void applyInventoryFromBase64(UUID playerId, String payloadBase64) throws IOException {
        ServerPlayerEntity player = server.getPlayerManager().getPlayer(playerId);
        if (player == null) {
            throw new IllegalStateException("Player not online: " + playerId);
        }
        PlayerInventoryCodec.applyInventoryFromBase64(server, player, payloadBase64);
    }
}
