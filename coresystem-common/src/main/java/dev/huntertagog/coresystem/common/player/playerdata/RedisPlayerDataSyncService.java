package dev.huntertagog.coresystem.common.player.playerdata;

import dev.huntertagog.coresystem.common.error.CoreError;
import dev.huntertagog.coresystem.common.error.CoreErrorCode;
import dev.huntertagog.coresystem.common.error.CoreErrorSeverity;
import dev.huntertagog.coresystem.common.log.Logger;
import dev.huntertagog.coresystem.common.log.LoggerFactory;
import dev.huntertagog.coresystem.common.provider.ServiceProvider;
import dev.huntertagog.coresystem.common.redis.RedisClient;
import dev.huntertagog.coresystem.platform.player.playerdata.PlayerDataCodec;
import dev.huntertagog.coresystem.platform.player.playerdata.PlayerDataSyncService;
import dev.huntertagog.coresystem.platform.player.playerdata.PlayerInventorySnapshot;
import redis.clients.jedis.Jedis;

import java.io.IOException;
import java.util.Optional;
import java.util.UUID;

public final class RedisPlayerDataSyncService implements PlayerDataSyncService {

    private static final Logger LOG = LoggerFactory.get("PlayerDataSyncService");

    private static final String KEY_PREFIX = "cs:playerdata:inv:";
    private static final int TTL_SECONDS = 7 * 24 * 60 * 60; // 7 Tage

    private PlayerDataCodec codec() {
        PlayerDataCodec c = ServiceProvider.getService(PlayerDataCodec.class);
        if (c == null) {
            throw new IllegalStateException("PlayerDataCodec not registered (platform adapter missing)");
        }
        return c;
    }

    @Override
    public void saveInventorySnapshot(UUID playerId,
                                      String context,
                                      String reason) {

        String key = buildKey(playerId, context);

        final String payload;
        try {
            payload = codec().encodeInventoryToBase64(playerId);
        } catch (IOException e) {
            CoreError.of(
                            CoreErrorCode.PLAYERDATA_NBT_ENCODE_FAILED,
                            CoreErrorSeverity.WARN,
                            "Failed to encode inventory snapshot"
                    )
                    .withCause(e)
                    .withContextEntry("playerUuid", playerId.toString())
                    .withContextEntry("context", context)
                    .withContextEntry("reason", String.valueOf(reason))
                    .log();
            return;
        } catch (Exception e) {
            CoreError.of(
                            CoreErrorCode.PLAYERDATA_NBT_ENCODE_FAILED,
                            CoreErrorSeverity.ERROR,
                            "Unexpected error while encoding inventory snapshot"
                    )
                    .withCause(e)
                    .withContextEntry("playerUuid", playerId.toString())
                    .withContextEntry("context", context)
                    .log();
            return;
        }

        long now = System.currentTimeMillis();
        PlayerInventorySnapshot snapshot = new PlayerInventorySnapshot(
                playerId,
                context,
                now,
                payload,
                "v1"
        );

        try (Jedis jedis = RedisClient.get().getResource()) {
            // aktuell: nur payload speichern (wie vorher). Wenn du später erweitern willst: Gson + komplettes Snapshot-JSON.
            jedis.setex(key, TTL_SECONDS, snapshot.payloadBase64());
        } catch (Exception e) {
            CoreError.of(
                            CoreErrorCode.PLAYERDATA_REDIS_SAVE_FAILED,
                            CoreErrorSeverity.WARN,
                            "Failed to save inventory snapshot to Redis"
                    )
                    .withCause(e)
                    .withContextEntry("redisKey", key)
                    .withContextEntry("playerUuid", playerId.toString())
                    .withContextEntry("context", context)
                    .log();
            return;
        }

        LOG.debug("Saved inventory snapshot for {} context={}", playerId, context);
    }

    @Override
    public Optional<PlayerInventorySnapshot> findInventorySnapshot(UUID playerId,
                                                                   String context) {
        String key = buildKey(playerId, context);

        final String payload;
        try (Jedis jedis = RedisClient.get().getResource()) {
            payload = jedis.get(key);
        } catch (Exception e) {
            CoreError.of(
                            CoreErrorCode.PLAYERDATA_REDIS_LOAD_FAILED,
                            CoreErrorSeverity.WARN,
                            "Failed to load inventory snapshot from Redis"
                    )
                    .withCause(e)
                    .withContextEntry("redisKey", key)
                    .withContextEntry("playerUuid", playerId.toString())
                    .withContextEntry("context", context)
                    .log();
            return Optional.empty();
        }

        if (payload == null || payload.isEmpty()) {
            return Optional.empty();
        }

        long now = System.currentTimeMillis();
        return Optional.of(new PlayerInventorySnapshot(
                playerId,
                context,
                now,      // createdAt unbekannt (weil nur payload gespeichert)
                payload,
                "v1"
        ));
    }

    @Override
    public boolean applyInventorySnapshot(UUID playerId,
                                          String context,
                                          boolean clearAfter) {

        Optional<PlayerInventorySnapshot> opt = findInventorySnapshot(playerId, context);
        if (opt.isEmpty()) return false;

        PlayerInventorySnapshot snapshot = opt.get();

        try {
            codec().applyInventoryFromBase64(playerId, snapshot.payloadBase64());
        } catch (IOException e) {
            CoreError.of(
                            CoreErrorCode.PLAYERDATA_NBT_DECODE_FAILED,
                            CoreErrorSeverity.ERROR,
                            "Failed to apply inventory snapshot (NBT decode failed)"
                    )
                    .withCause(e)
                    .withContextEntry("playerUuid", playerId.toString())
                    .withContextEntry("context", context)
                    .log();
            return false;
        } catch (Exception e) {
            CoreError.of(
                            CoreErrorCode.PLAYERDATA_NBT_DECODE_FAILED,
                            CoreErrorSeverity.ERROR,
                            "Unexpected error while applying inventory snapshot"
                    )
                    .withCause(e)
                    .withContextEntry("playerUuid", playerId.toString())
                    .withContextEntry("context", context)
                    .log();
            return false;
        }

        if (clearAfter) {
            deleteInventorySnapshot(playerId, context);
        }

        LOG.debug("Applied inventory snapshot for {} context={}", playerId, context);
        return true;
    }

    @Override
    public void deleteInventorySnapshot(UUID playerId, String context) {
        String key = buildKey(playerId, context);
        try (Jedis jedis = RedisClient.get().getResource()) {
            jedis.del(key);
        } catch (Exception e) {
            CoreError.of(
                            CoreErrorCode.PLAYERDATA_REDIS_DELETE_FAILED,
                            CoreErrorSeverity.WARN,
                            "Failed to delete inventory snapshot from Redis"
                    )
                    .withCause(e)
                    .withContextEntry("redisKey", key)
                    .withContextEntry("playerUuid", playerId.toString())
                    .withContextEntry("context", context)
                    .log();
        }
    }

    private static String buildKey(UUID uuid, String context) {
        String normalizedContext = context.toLowerCase().replace(' ', '_');
        return KEY_PREFIX + normalizedContext + ":" + uuid;
    }
}
