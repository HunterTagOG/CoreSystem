package dev.huntertagog.coresystem.velocity.node;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public final class NodeRouter {

    private final Map<String, String> nodeIdToServerName = new ConcurrentHashMap<>();

    /**
     * Registrierung beim Proxy-Startup
     * nodeId = SERVER_ID
     * serverName = Velocity RegisteredServer name
     */
    public void register(String nodeId, String serverName) {
        nodeIdToServerName.putIfAbsent(nodeId, serverName);
    }

    public void registerAll(Map<String, String> mappings) {
        nodeIdToServerName.putAll(mappings);
    }

    public Optional<String> resolveServerName(String nodeId) {
        return Optional.ofNullable(nodeIdToServerName.get(nodeId));
    }
}
