package dev.huntertagog.coresystem.common.player;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import dev.huntertagog.coresystem.common.error.CoreError;
import dev.huntertagog.coresystem.common.error.CoreErrorCode;
import dev.huntertagog.coresystem.common.error.CoreErrorSeverity;
import dev.huntertagog.coresystem.common.log.Logger;
import dev.huntertagog.coresystem.common.log.LoggerFactory;
import dev.huntertagog.coresystem.common.player.event.PlayerJoinedNetworkEvent;
import dev.huntertagog.coresystem.common.player.event.PlayerQuitNetworkEvent;
import dev.huntertagog.coresystem.common.provider.ServiceProvider;
import dev.huntertagog.coresystem.common.redis.RedisClient;
import dev.huntertagog.coresystem.platform.event.DomainEventBus;
import dev.huntertagog.coresystem.platform.player.PlayerProfile;
import dev.huntertagog.coresystem.platform.player.PlayerProfileService;
import redis.clients.jedis.Jedis;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

public final class RedisPlayerProfileService implements PlayerProfileService {

    private static final Logger LOG = LoggerFactory.get("PlayerProfileService");
    private static final String KEY_PREFIX = "cs:playerprofile:";

    private final Cache<UUID, PlayerProfile> cache;
    private final Gson gson = new Gson();
    private final DomainEventBus eventBus;

    public RedisPlayerProfileService() {
        this.cache = Caffeine.newBuilder()
                .expireAfterAccess(Duration.ofMinutes(10))
                .maximumSize(50_000)
                .build();

        this.eventBus = ServiceProvider.getService(DomainEventBus.class);
    }

    @Override
    public PlayerProfile getOrCreate(UUID uniqueId, String fallbackName) {
        PlayerProfile cached = cache.getIfPresent(uniqueId);
        if (cached != null) return cached;

        Optional<PlayerProfile> fromRedis = loadFromRedis(uniqueId);
        if (fromRedis.isPresent()) {
            PlayerProfile profile = fromRedis.get();
            cache.put(uniqueId, profile);
            return profile;
        }

        long now = System.currentTimeMillis();
        String name = (fallbackName != null && !fallbackName.isBlank()) ? fallbackName : uniqueId.toString();

        PlayerProfile created = new PlayerProfile(
                uniqueId,
                name,
                now,
                now,
                0,
                "unknown",
                null
        );

        saveToRedis(created);
        cache.put(uniqueId, created);
        return created;
    }

    @Override
    public Optional<PlayerProfile> find(UUID uniqueId) {
        PlayerProfile cached = cache.getIfPresent(uniqueId);
        if (cached != null) return Optional.of(cached);

        Optional<PlayerProfile> fromRedis = loadFromRedis(uniqueId);
        fromRedis.ifPresent(profile -> cache.put(uniqueId, profile));
        return fromRedis;
    }

    @Override
    public PlayerProfile updateOnJoin(
            UUID uniqueId,
            String playerName,
            String currentServerName,
            String nodeId
    ) {
        long now = System.currentTimeMillis();

        PlayerProfile current = getOrCreate(uniqueId, playerName);
        PlayerProfile updated = current.withUpdatedOnJoin(
                playerName,
                currentServerName,
                nodeId,
                now
        );

        cache.put(uniqueId, updated);
        saveToRedis(updated);

        LOG.debug("Updated PlayerProfile on join for {} ({})", playerName, uniqueId);
        publishPlayerJoined(updated);

        return updated;
    }

    @Override
    public Optional<PlayerProfile> updateOnQuit(
            UUID uniqueId,
            String currentServerName,
            String nodeId
    ) {
        long now = System.currentTimeMillis();

        // Name ist beim Quit optional → wir nutzen UUID als fallback
        PlayerProfile current = getOrCreate(uniqueId, uniqueId.toString());
        PlayerProfile updated = current.withUpdatedLastSeen(now, currentServerName, nodeId);

        cache.put(uniqueId, updated);
        saveToRedis(updated);

        LOG.debug("Updated PlayerProfile on quit for {} ({})", updated.getName(), uniqueId);
        publishPlayerQuit(updated);

        return Optional.of(updated);
    }

    private void publishPlayerJoined(PlayerProfile profile) {
        if (eventBus == null) return;
        eventBus.publishAsync(new PlayerJoinedNetworkEvent(profile));
    }

    private void publishPlayerQuit(PlayerProfile profile) {
        if (eventBus == null) return;
        eventBus.publishAsync(new PlayerQuitNetworkEvent(profile));
    }

    private Optional<PlayerProfile> loadFromRedis(UUID uuid) {
        String key = KEY_PREFIX + uuid;

        try (Jedis jedis = RedisClient.get().getResource()) {
            String json = jedis.get(key);
            if (json == null || json.isEmpty()) return Optional.empty();

            JsonObject obj = gson.fromJson(json, JsonObject.class);

            UUID id = UUID.fromString(obj.get("uniqueId").getAsString());
            String name = obj.get("name").getAsString();
            long firstSeen = obj.get("firstSeenAt").getAsLong();
            long lastSeen = obj.get("lastSeenAt").getAsLong();
            int totalJoins = obj.get("totalJoins").getAsInt();

            String lastServer = obj.has("lastServer") && !obj.get("lastServer").isJsonNull()
                    ? obj.get("lastServer").getAsString()
                    : "unknown";

            String lastNodeId = obj.has("lastNodeId") && !obj.get("lastNodeId").isJsonNull()
                    ? obj.get("lastNodeId").getAsString()
                    : null;

            return Optional.of(new PlayerProfile(
                    id, name, firstSeen, lastSeen, totalJoins, lastServer, lastNodeId
            ));
        } catch (Exception e) {
            CoreError error = CoreError.of(
                            CoreErrorCode.PLAYERPROFILE_REDIS_LOAD_FAILED,
                            CoreErrorSeverity.WARN,
                            "Failed to load PlayerProfile from Redis"
                    )
                    .withCause(e)
                    .withContextEntry("redisKey", key)
                    .withContextEntry("playerUuid", uuid.toString());

            LOG.warn(error.toLogString(), e);
            return Optional.empty();
        }
    }

    private void saveToRedis(PlayerProfile profile) {
        String key = KEY_PREFIX + profile.getUniqueId();

        try (Jedis jedis = RedisClient.get().getResource()) {
            JsonObject obj = new JsonObject();
            obj.addProperty("uniqueId", profile.getUniqueId().toString());
            obj.addProperty("name", profile.getName());
            obj.addProperty("firstSeenAt", profile.getFirstSeenAt());
            obj.addProperty("lastSeenAt", profile.getLastSeenAt());
            obj.addProperty("totalJoins", profile.getTotalJoins());
            obj.addProperty("lastServer", profile.getLastServer());
            obj.addProperty("lastNodeId", profile.getLastNodeId());

            jedis.set(key, gson.toJson(obj));
        } catch (Exception e) {
            CoreError error = CoreError.of(
                            CoreErrorCode.PLAYERPROFILE_REDIS_SAVE_FAILED,
                            CoreErrorSeverity.WARN,
                            "Failed to save PlayerProfile to Redis"
                    )
                    .withCause(e)
                    .withContextEntry("redisKey", key)
                    .withContextEntry("playerUuid", profile.getUniqueId().toString())
                    .withContextEntry("playerName", profile.getName());

            LOG.warn(error.toLogString(), e);
        }
    }
}
