package dev.huntertagog.coresystem.fabric.common.player;

import com.mojang.authlib.GameProfile;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.Optional;
import java.util.UUID;

public final class PlayerLookup {

    private PlayerLookup() {
    }

    public static Optional<UUID> resolveUuid(MinecraftServer server, String name) {
        if (name == null || name.isBlank()) return Optional.empty();

        // 1) online
        ServerPlayerEntity online = server.getPlayerManager().getPlayer(name);
        if (online != null) return Optional.of(online.getUuid());

        // 2) cached profiles
        if (server.getUserCache() != null) {
            return server.getUserCache().findByName(name).map(GameProfile::getId);
        }

        return Optional.empty();
    }

    public static Optional<String> resolveName(MinecraftServer server, UUID uuid) {
        if (uuid == null) return Optional.empty();

        ServerPlayerEntity online = server.getPlayerManager().getPlayer(uuid);
        if (online != null) return Optional.of(online.getGameProfile().getName());

        if (server.getUserCache() != null) {
            return server.getUserCache().getByUuid(uuid).map(GameProfile::getName);
        }

        return Optional.empty();
    }
}
