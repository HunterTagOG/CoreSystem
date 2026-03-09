package dev.huntertagog.coresystem.common.islands;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import dev.huntertagog.coresystem.common.error.CoreError;
import dev.huntertagog.coresystem.common.error.CoreErrorCode;
import dev.huntertagog.coresystem.common.error.CoreErrorSeverity;
import dev.huntertagog.coresystem.common.log.Logger;
import dev.huntertagog.coresystem.common.log.LoggerFactory;
import dev.huntertagog.coresystem.common.redis.RedisClient;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.params.ScanParams;
import redis.clients.jedis.resps.ScanResult;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public final class PrivateIslandWorldOwnerRepositoryRedis {

    private static final Logger LOG = LoggerFactory.get("IslandOwnerRepo");
    private static final PrivateIslandWorldOwnerRepositoryRedis INSTANCE = new PrivateIslandWorldOwnerRepositoryRedis();

    private static final String OWNER_PREFIX = "coresystem:islands:owner:";
    private static final String OWNERS_SET = "coresystem:islands:owners";

    // lokaler Read-Cache: Owner -> NodeId
    private final Cache<UUID, String> ownerNodeCache = Caffeine.newBuilder()
            .maximumSize(50_000)
            .expireAfterAccess(Duration.ofHours(1))
            .build();

    private PrivateIslandWorldOwnerRepositoryRedis() {
    }

    public static PrivateIslandWorldOwnerRepositoryRedis get() {
        return INSTANCE;
    }

    private String ownerKey(UUID ownerId) {
        return OWNER_PREFIX + ownerId;
    }

    /**
     * Assign owner -> node:
     * - SET ownerKey -> nodeId
     * - SADD owners index
     */
    public void assignOwnerToNode(UUID ownerId, String nodeId) {
        try (Jedis jedis = RedisClient.get().getResource()) {
            jedis.set(ownerKey(ownerId), nodeId);
            jedis.sadd(OWNERS_SET, ownerId.toString());
        } catch (Exception e) {
            CoreError error = CoreError.of(
                            CoreErrorCode.ISLAND_OWNER_ASSIGN_REDIS_FAILED,
                            CoreErrorSeverity.WARN,
                            "Failed to assign island owner to node in Redis."
                    )
                    .withCause(e)
                    .withContextEntry("ownerId", ownerId.toString())
                    .withContextEntry("nodeId", nodeId);

            LOG.warn(error.toLogString(), e);
        }

        // Cache best-effort
        ownerNodeCache.put(ownerId, nodeId);
    }

    /**
     * Optional aber wichtig für Delete/Unassign:
     * - DEL ownerKey
     * - SREM owners index
     * - cache invalidate
     */
    public void unassignOwner(UUID ownerId) {
        try (Jedis jedis = RedisClient.get().getResource()) {
            jedis.del(ownerKey(ownerId));
            jedis.srem(OWNERS_SET, ownerId.toString());
        } catch (Exception e) {
            CoreError error = CoreError.of(
                            CoreErrorCode.ISLAND_OWNER_ASSIGN_REDIS_FAILED,
                            CoreErrorSeverity.WARN,
                            "Failed to unassign island owner from Redis."
                    )
                    .withCause(e)
                    .withContextEntry("ownerId", ownerId.toString());
            LOG.warn(error.toLogString(), e);
        } finally {
            ownerNodeCache.invalidate(ownerId);
        }
    }

    public String getAssignedNode(UUID ownerId) {
        String cached = ownerNodeCache.getIfPresent(ownerId);
        if (cached != null) return cached;

        try (Jedis jedis = RedisClient.get().getResource()) {
            String nodeId = jedis.get(ownerKey(ownerId));
            if (nodeId != null && !nodeId.isBlank()) {
                ownerNodeCache.put(ownerId, nodeId);
                return nodeId;
            }
            return null;
        } catch (Exception e) {
            CoreError error = CoreError.of(
                            CoreErrorCode.ISLAND_OWNER_LOAD_REDIS_FAILED,
                            CoreErrorSeverity.WARN,
                            "Failed to load assigned island node for owner from Redis."
                    )
                    .withCause(e)
                    .withContextEntry("ownerId", ownerId.toString());

            LOG.warn(error.toLogString(), e);
            return null;
        }
    }

    /**
     * Performant listing via SSCAN.
     * Für Tab-Suggestions: limit setzen (z.B. 200).
     */
    public Collection<UUID> getAllOwners(int limit) {
        if (limit <= 0) return Collections.emptyList();

        try (Jedis jedis = RedisClient.get().getResource()) {
            ArrayList<UUID> owners = new ArrayList<>(Math.min(limit, 512));

            // Randomized start cursor reduziert Hotspots bei vielen gleichzeitigen Tab-Requests
            String cursor = ThreadLocalRandom.current().nextBoolean() ? "0" : "0";
            ScanParams params = new ScanParams().count(Math.min(limit, 500));

            do {
                ScanResult<String> res = jedis.sscan(OWNERS_SET, cursor, params);
                cursor = res.getCursor();

                for (String s : res.getResult()) {
                    try {
                        owners.add(UUID.fromString(s));
                        if (owners.size() >= limit) return owners;
                    } catch (IllegalArgumentException ignored) {
                        // defekte Daten im Set ignorieren
                    }
                }
            } while (!"0".equals(cursor));

            return owners;
        } catch (Exception e) {
            CoreError error = CoreError.of(
                            CoreErrorCode.ISLAND_OWNER_LIST_REDIS_FAILED,
                            CoreErrorSeverity.WARN,
                            "Failed to list all island owners from Redis (SSCAN)."
                    )
                    .withCause(e);

            LOG.warn(error.toLogString(), e);
            return Collections.emptyList();
        }
    }

    /**
     * Backwards kompatibel: ohne limit (nicht empfohlen).
     * Ich würde das im Code NICHT mehr verwenden.
     */
    public Collection<UUID> getAllOwners() {
        return getAllOwners(500);
    }
}
