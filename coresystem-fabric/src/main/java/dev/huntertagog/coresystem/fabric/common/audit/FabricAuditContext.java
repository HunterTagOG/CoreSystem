package dev.huntertagog.coresystem.fabric.common.audit;

import dev.huntertagog.coresystem.platform.audit.AuditContext;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.Optional;

public final class FabricAuditContext implements AuditContext {

    private final ServerCommandSource source;
    private final String nodeId;

    public FabricAuditContext(ServerCommandSource source) {
        this.source = source;
        this.nodeId = Optional.ofNullable(System.getenv("SERVER_ID")).orElse("default");
    }

    private Optional<ServerPlayerEntity> player() {
        try {
            return Optional.ofNullable(source.getPlayer());
        } catch (Exception ignored) {
            return Optional.empty();
        }
    }

    @Override
    public Optional<String> actorId() {
        return player().map(p -> p.getUuid().toString());
    }

    @Override
    public String actorName() {
        return player()
                .map(p -> p.getGameProfile().getName())
                .orElse("CONSOLE");
    }

    @Override
    public String serverName() {
        // Velocity-Servername wäre idealerweise über config/ENV, aber das passt als Fallback:
        return source.getServer().getName();
    }

    @Override
    public String nodeId() {
        return nodeId;
    }

    @Override
    public AuditActorType actorType() {
        return player().isPresent()
                ? AuditActorType.PLAYER
                : AuditActorType.CONSOLE;
    }
}
