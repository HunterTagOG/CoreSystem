package dev.huntertagog.coresystem.common.islands;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import dev.huntertagog.coresystem.common.error.CoreError;
import dev.huntertagog.coresystem.common.error.CoreErrorCode;
import dev.huntertagog.coresystem.common.error.CoreErrorSeverity;
import dev.huntertagog.coresystem.common.log.Logger;
import dev.huntertagog.coresystem.common.log.LoggerFactory;
import dev.huntertagog.coresystem.common.redis.RedisClient;
import dev.huntertagog.coresystem.platform.islands.PrivateIslandNodeRepository;
import dev.huntertagog.coresystem.platform.islands.PrivateIslandWorldNodeStatus;
import dev.huntertagog.coresystem.platform.provider.Service;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.Pipeline;
import redis.clients.jedis.Response;
import redis.clients.jedis.params.ScanParams;
import redis.clients.jedis.resps.ScanResult;

import java.time.Duration;
import java.util.*;

public final class RedisPrivateIslandNodeRepository implements PrivateIslandNodeRepository, Service {

    private static final Logger LOG = LoggerFactory.get("IslandNodeRepo");

    // Redis Keys
    private static final String NODE_PREFIX = "coresystem:islands:node:";
    private static final String NODES_SET = "coresystem:islands:nodes";

    // TTL: Node gilt nach X Sekunden ohne Heartbeat als stale
    private static final int STATUS_TTL_SECONDS = 15;

    // lokale Read-Caches (kurz, weil Redis ohnehin Source of Truth ist)
    private final Cache<String, PrivateIslandWorldNodeStatus> nodeCache;
    private final Cache<String, Collection<PrivateIslandWorldNodeStatus>> allNodesCache;

    public RedisPrivateIslandNodeRepository() {
        this.nodeCache = Caffeine.newBuilder()
                .maximumSize(512)
                .expireAfterWrite(Duration.ofSeconds(2))
                .build();

        this.allNodesCache = Caffeine.newBuilder()
                .maximumSize(1)
                .expireAfterWrite(Duration.ofSeconds(2))
                .build();
    }

    private String nodeKey(String nodeId) {
        return NODE_PREFIX + nodeId;
    }

    @Override
    public void saveStatus(PrivateIslandWorldNodeStatus status) {
        if (status == null || status.nodeId() == null || status.nodeId().isBlank()) {
            return;
        }

        String key = nodeKey(status.nodeId());

        Map<String, String> fields = new HashMap<>(16);
        fields.put("nodeId", status.nodeId());
        fields.put("serverName", safe(status.serverName(), status.nodeId()));
        fields.put("islandServer", Boolean.toString(status.islandServer()));
        fields.put("currentIslands", Integer.toString(status.currentIslands()));
        fields.put("maxIslands", Integer.toString(status.maxIslands()));
        fields.put("currentPlayers", Integer.toString(status.currentPlayers()));
        fields.put("maxPlayers", Integer.toString(status.maxPlayers()));
        fields.put("lastHeartbeatMillis", Long.toString(status.lastHeartbeatMillis()));

        try (Jedis jedis = RedisClient.get().getResource()) {
            Pipeline p = jedis.pipelined();
            p.hset(key, fields);
            p.expire(key, STATUS_TTL_SECONDS);
            p.sadd(NODES_SET, status.nodeId());
            p.sync();
        } catch (Exception e) {
            CoreError.of(
                            CoreErrorCode.ISLAND_NODE_SAVE_FAILED,
                            CoreErrorSeverity.WARN,
                            "Failed to save IslandNodeStatus to Redis."
                    )
                    .withCause(e)
                    .withContextEntry("nodeId", status.nodeId())
                    .withContextEntry("serverName", status.serverName())
                    .log();
        }

        // Cache: write-through + allNodes invalidieren
        nodeCache.put(status.nodeId(), status);
        allNodesCache.invalidateAll();
    }

    @Override
    public PrivateIslandWorldNodeStatus getStatus(String nodeId) {
        if (nodeId == null || nodeId.isBlank()) return null;

        PrivateIslandWorldNodeStatus cached = nodeCache.getIfPresent(nodeId);
        if (cached != null) return cached;

        try (Jedis jedis = RedisClient.get().getResource()) {
            Map<String, String> map = jedis.hgetAll(nodeKey(nodeId));
            if (map == null || map.isEmpty()) return null;

            PrivateIslandWorldNodeStatus status = mapToStatus(map);
            nodeCache.put(nodeId, status);
            return status;
        } catch (Exception e) {
            CoreError.of(
                            CoreErrorCode.ISLAND_NODE_LOAD_FAILED,
                            CoreErrorSeverity.WARN,
                            "Failed to load IslandNodeStatus from Redis."
                    )
                    .withCause(e)
                    .withContextEntry("nodeId", nodeId)
                    .log();
            return null;
        }
    }

    @Override
    public Collection<PrivateIslandWorldNodeStatus> getAllStatuses() {
        return allNodesCache.get("all", __ -> loadAllFromRedisScan(500));
    }

    /**
     * SSCAN statt SMEMBERS: nicht-blockierend, limitierbar.
     */
    private Collection<PrivateIslandWorldNodeStatus> loadAllFromRedisScan(int limit) {
        if (limit <= 0) return List.of();

        List<String> nodeIds = new ArrayList<>(Math.min(limit, 512));

        try (Jedis jedis = RedisClient.get().getResource()) {
            String cursor = ScanParams.SCAN_POINTER_START;
            ScanParams params = new ScanParams().count(Math.min(limit, 200));

            do {
                ScanResult<String> res = jedis.sscan(NODES_SET, cursor, params);
                cursor = res.getCursor();

                for (String id : res.getResult()) {
                    if (id == null || id.isBlank()) continue;
                    nodeIds.add(id);
                    if (nodeIds.size() >= limit) break;
                }
            } while (!"0".equals(cursor) && nodeIds.size() < limit);

            if (nodeIds.isEmpty()) return List.of();

            // pipelined HGETALL
            Pipeline p = jedis.pipelined();
            List<Response<Map<String, String>>> responses = new ArrayList<>(nodeIds.size());
            for (String id : nodeIds) {
                responses.add(p.hgetAll(nodeKey(id)));
            }
            p.sync();

            List<PrivateIslandWorldNodeStatus> out = new ArrayList<>(nodeIds.size());

            for (int i = 0; i < nodeIds.size(); i++) {
                String id = nodeIds.get(i);
                Map<String, String> map = responses.get(i).get();

                if (map == null || map.isEmpty()) {
                    // stale -> Index cleanup best effort
                    try {
                        jedis.srem(NODES_SET, id);
                    } catch (Exception ignored) {
                    }
                    continue;
                }

                PrivateIslandWorldNodeStatus status = mapToStatus(map);
                nodeCache.put(id, status);
                out.add(status);
            }

            return out;
        } catch (Exception e) {
            CoreError.of(
                            CoreErrorCode.ISLAND_NODE_LOAD_ALL_FAILED,
                            CoreErrorSeverity.WARN,
                            "Failed to load all IslandNodeStatus entries from Redis (SSCAN+pipelined)."
                    )
                    .withCause(e)
                    .log();
            return List.of();
        }
    }

    private PrivateIslandWorldNodeStatus mapToStatus(Map<String, String> map) {
        try {
            String nodeId = map.getOrDefault("nodeId", "unknown");
            String serverName = map.getOrDefault("serverName", nodeId);

            boolean islandServer = Boolean.parseBoolean(map.getOrDefault("islandServer", "false"));
            int currentIslands = parseInt(map.get("currentIslands"), 0);
            int maxIslands = parseInt(map.get("maxIslands"), 0);
            int currentPlayers = parseInt(map.get("currentPlayers"), 0);
            int maxPlayers = parseInt(map.get("maxPlayers"), 0);
            long lastHeartbeat = parseLong(map.get("lastHeartbeatMillis"), 0L);

            return new PrivateIslandWorldNodeStatus(
                    nodeId,
                    serverName,
                    islandServer,
                    currentIslands,
                    maxIslands,
                    currentPlayers,
                    maxPlayers,
                    lastHeartbeat
            );
        } catch (Exception e) {
            CoreError.of(
                            CoreErrorCode.ISLAND_NODE_STATUS_PARSE_FAILED,
                            CoreErrorSeverity.WARN,
                            "Failed to map Redis hash to IslandNodeStatus."
                    )
                    .withCause(e)
                    .withContextEntry("rawMap", String.valueOf(map))
                    .log();

            String nodeId = map.getOrDefault("nodeId", "unknown");
            String serverName = map.getOrDefault("serverName", nodeId);

            return new PrivateIslandWorldNodeStatus(nodeId, serverName, false, 0, 0, 0, 0, 0L);
        }
    }

    private static String safe(String s, String fallback) {
        if (s == null || s.isBlank()) return fallback;
        return s;
    }

    private static int parseInt(String s, int def) {
        try {
            return s == null ? def : Integer.parseInt(s);
        } catch (Exception ignored) {
            return def;
        }
    }

    private static long parseLong(String s, long def) {
        try {
            return s == null ? def : Long.parseLong(s);
        } catch (Exception ignored) {
            return def;
        }
    }
}
