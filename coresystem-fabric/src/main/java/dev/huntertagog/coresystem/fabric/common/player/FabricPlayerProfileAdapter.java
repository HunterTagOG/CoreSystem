package dev.huntertagog.coresystem.fabric.common.player;

import dev.huntertagog.coresystem.platform.player.PlayerProfileService;
import net.minecraft.server.network.ServerPlayerEntity;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class FabricPlayerProfileAdapter {

    private final PlayerProfileService delegate;

    public FabricPlayerProfileAdapter(PlayerProfileService delegate) {
        this.delegate = delegate;
    }

    public void onJoin(
            @NotNull ServerPlayerEntity player,
            @NotNull String serverName,
            @Nullable String nodeId
    ) {
        delegate.updateOnJoin(
                player.getUuid(),
                player.getGameProfile().getName(),
                serverName,
                nodeId
        );
    }

    public void onQuit(
            @NotNull ServerPlayerEntity player,
            @NotNull String serverName,
            @Nullable String nodeId
    ) {
        delegate.updateOnQuit(
                player.getUuid(),
                serverName,
                nodeId
        );
    }

    public void ensureProfile(@NotNull ServerPlayerEntity player) {
        delegate.getOrCreate(
                player.getUuid(),
                player.getGameProfile().getName()
        );
    }
}
