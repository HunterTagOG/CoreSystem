package dev.huntertagog.coresystem.velocity.node;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Configuration holder for NodeRouter.
 * <p>
 * nodeId -> velocity registered server name
 * <p>
 * Example (conceptual):
 * island-1 -> islands-node-1
 * island-2 -> islands-node-2
 * <p>
 * Loading (HOCON/YAML/JSON) happens in your Velocity plugin bootstrap.
 */
public final class NodeRouterConfig {

    private final Map<String, String> mappings;

    public NodeRouterConfig(Map<String, String> mappings) {
        this.mappings = Collections.unmodifiableMap(new LinkedHashMap<>(mappings));
    }

    public Map<String, String> mappings() {
        return mappings;
    }

    /**
     * Optional ENV override:
     * CORESYSTEM_NODE_ROUTER="nodeA=serverA,nodeB=serverB"
     */
    public static NodeRouterConfig fromEnvOrEmpty(String envVar) {
        String raw = System.getenv(envVar.toUpperCase());
        if (raw == null || raw.isBlank()) {
            return new NodeRouterConfig(Map.of());
        }

        Map<String, String> map = new LinkedHashMap<>();
        String[] pairs = raw.split(",");
        for (String p : pairs) {
            String pair = p.trim();
            if (pair.isEmpty()) continue;
            int eq = pair.indexOf('=');
            if (eq <= 0 || eq >= pair.length() - 1) continue;

            String nodeId = pair.substring(0, eq).trim();
            String serverName = pair.substring(eq + 1).trim();
            if (!nodeId.isEmpty() && !serverName.isEmpty()) {
                map.put(nodeId, serverName);
            }
        }

        return new NodeRouterConfig(map);
    }
}
