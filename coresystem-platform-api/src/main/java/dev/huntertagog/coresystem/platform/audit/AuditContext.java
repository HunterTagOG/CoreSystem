package dev.huntertagog.coresystem.platform.audit;

import java.util.Map;
import java.util.Optional;

public interface AuditContext {

    // ===== ACTOR =====
    AuditActorType actorType();

    /**
     * Actor identifier (UUID, account-id, system-id) als String
     */
    Optional<String> actorId();

    /**
     * Anzeigename (Playername, "CONSOLE", "SYSTEM")
     */
    String actorName();
    

    // ===== ENVIRONMENT =====
    String serverName();

    String nodeId();

    // ===== TRACE =====

    /**
     * Optional: Request / Trace / Correlation ID
     */
    default Optional<String> traceId() {
        return Optional.empty();
    }

    // ===== METADATA =====

    /**
     * Zusätzlicher Kontext (z.B. command, source, ip, module)
     */
    default Map<String, String> metadata() {
        return Map.of();
    }

    enum AuditActorType {
        PLAYER,
        CONSOLE,
        SYSTEM
    }
}
