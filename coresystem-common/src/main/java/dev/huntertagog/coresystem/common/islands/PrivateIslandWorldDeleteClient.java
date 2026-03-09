package dev.huntertagog.coresystem.common.islands;

import com.google.gson.JsonObject;
import dev.huntertagog.coresystem.common.error.CoreError;
import dev.huntertagog.coresystem.common.error.CoreErrorCode;
import dev.huntertagog.coresystem.common.error.CoreErrorSeverity;
import dev.huntertagog.coresystem.common.log.Logger;
import dev.huntertagog.coresystem.common.log.LoggerFactory;
import dev.huntertagog.coresystem.common.redis.RedisClient;
import redis.clients.jedis.Jedis;

import java.util.Map;
import java.util.UUID;

public final class PrivateIslandWorldDeleteClient {

    private static final Logger LOG = LoggerFactory.get("DeleteClient");

    private static final String DELETE_QUEUE_KEY = "pi:island:delete:queue";

    private PrivateIslandWorldDeleteClient() {
    }

    public static void enqueueDeleteJob(UUID ownerId, String targetNodeId) {

        if (ownerId == null || targetNodeId == null || targetNodeId.isBlank()) {
            CoreError error = new CoreError(
                    CoreErrorCode.ISLAND_DELETE_ENQUEUE_FAILED,
                    CoreErrorSeverity.ERROR,
                    "enqueueDeleteJob called with invalid arguments.",
                    null,
                    Map.of(
                            "ownerId", String.valueOf(ownerId),
                            "targetNodeId", targetNodeId
                    )
            );
            LOG.error(error.toLogString());
            return;
        }

        String queue = DELETE_QUEUE_KEY + ":" + targetNodeId;

        try (Jedis redis = RedisClient.get().getResource()) {

            JsonObject payload = new JsonObject();
            payload.addProperty("ownerId", ownerId.toString());
            payload.addProperty("nodeId", targetNodeId);
            payload.addProperty("ts", System.currentTimeMillis());

            redis.lpush(queue, payload.toString());

            LOG.debug("[Island-Delete] Enqueued delete job for owner {} on node {} → {}",
                    ownerId, targetNodeId, queue);

        } catch (Exception e) {

            CoreError error = new CoreError(
                    CoreErrorCode.ISLAND_DELETE_ENQUEUE_FAILED,
                    CoreErrorSeverity.CRITICAL, // Worker kann sonst nicht löschen
                    "Failed to enqueue island delete job in Redis.",
                    e,
                    Map.of(
                            "ownerId", ownerId.toString(),
                            "targetNodeId", targetNodeId,
                            "queue", queue
                    )
            );

            LOG.error(error.toLogString(), e);
        }
    }
}
