package dev.huntertagog.coresystem.common.friends;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import dev.huntertagog.coresystem.common.error.CoreError;
import dev.huntertagog.coresystem.common.error.CoreErrorCode;
import dev.huntertagog.coresystem.common.error.CoreErrorSeverity;
import dev.huntertagog.coresystem.common.log.Logger;
import dev.huntertagog.coresystem.common.log.LoggerFactory;
import dev.huntertagog.coresystem.common.redis.RedisClient;
import dev.huntertagog.coresystem.platform.friends.FriendRelation;
import dev.huntertagog.coresystem.platform.friends.FriendRelationStatus;
import dev.huntertagog.coresystem.platform.friends.FriendService;
import dev.huntertagog.coresystem.platform.friends.OfflineFriendMessage;
import redis.clients.jedis.Jedis;

import java.util.*;

public final class RedisFriendService implements FriendService {

    private static final Logger LOG = LoggerFactory.get("Friends");
    private static final Gson GSON = new Gson();

    private static String friendsKey(UUID u) {
        return "cs:friends:" + u + ":friends";
    }

    private static String incomingKey(UUID u) {
        return "cs:friends:" + u + ":incoming";
    }

    private static String outgoingKey(UUID u) {
        return "cs:friends:" + u + ":outgoing";
    }

    private static String messagesKey(UUID u) {
        return "cs:friends:" + u + ":messages";
    }

    @Override
    public FriendRelation getRelation(UUID self, UUID other) {
        if (self.equals(other)) {
            return new FriendRelation(self, other, FriendRelationStatus.NONE);
        }

        try (Jedis jedis = RedisClient.get().getResource()) {
            String selfFriendsKey = friendsKey(self);
            String selfIncomingKey = incomingKey(self);
            String selfOutgoingKey = outgoingKey(self);

            String otherStr = other.toString();

            if (jedis.sismember(selfFriendsKey, otherStr)) {
                return new FriendRelation(self, other, FriendRelationStatus.FRIENDS);
            }
            if (jedis.sismember(selfIncomingKey, otherStr)) {
                return new FriendRelation(self, other, FriendRelationStatus.PENDING_IN);
            }
            if (jedis.sismember(selfOutgoingKey, otherStr)) {
                return new FriendRelation(self, other, FriendRelationStatus.PENDING_OUT);
            }
            return new FriendRelation(self, other, FriendRelationStatus.NONE);
        } catch (Exception e) {
            CoreError.of(
                            CoreErrorCode.FRIENDS_REDIS_FAILURE,
                            CoreErrorSeverity.WARN,
                            "Failed to determine friend relation"
                    )
                    .withCause(e)
                    .withContextEntry("self", self.toString())
                    .withContextEntry("other", other.toString())
                    .log();
            return new FriendRelation(self, other, FriendRelationStatus.NONE);
        }
    }

    @Override
    public List<UUID> getFriends(UUID self) {
        try (Jedis jedis = RedisClient.get().getResource()) {
            Set<String> raw = jedis.smembers(friendsKey(self));
            if (raw == null || raw.isEmpty()) return List.of();
            return raw.stream()
                    .map(this::parseUuidSafe)
                    .filter(Objects::nonNull)
                    .toList();
        } catch (Exception e) {
            CoreError.of(
                            CoreErrorCode.FRIENDS_REDIS_FAILURE,
                            CoreErrorSeverity.WARN,
                            "Failed to load friends"
                    )
                    .withCause(e)
                    .withContextEntry("self", self.toString())
                    .log();
            return List.of();
        }
    }

    @Override
    public List<UUID> getIncomingRequests(UUID self) {
        return loadUuidSet(incomingKey(self));
    }

    @Override
    public List<UUID> getOutgoingRequests(UUID self) {
        return loadUuidSet(outgoingKey(self));
    }

    @Override
    public boolean areFriends(UUID a, UUID b) {
        return getRelation(a, b).status() == FriendRelationStatus.FRIENDS;
    }

    @Override
    public boolean sendRequest(UUID from, UUID to) {
        if (from.equals(to)) return false;

        try (Jedis jedis = RedisClient.get().getResource()) {
            String fromStr = from.toString();
            String toStr = to.toString();

            // Bereits Freunde?
            if (jedis.sismember(friendsKey(from), toStr)) {
                return false;
            }

            // Wenn der andere bereits eine Anfrage an mich geschickt hat → direkt Freundschaft
            if (jedis.sismember(incomingKey(from), toStr)) {
                return acceptRequest(from, to);
            }

            jedis.sadd(outgoingKey(from), toStr);
            jedis.sadd(incomingKey(to), fromStr);
            return true;
        } catch (Exception e) {
            CoreError.of(
                            CoreErrorCode.FRIENDS_REDIS_FAILURE,
                            CoreErrorSeverity.WARN,
                            "Failed to send friend request"
                    )
                    .withCause(e)
                    .withContextEntry("from", from.toString())
                    .withContextEntry("to", to.toString())
                    .log();
            return false;
        }
    }

    @Override
    public boolean acceptRequest(UUID target, UUID from) {
        try (Jedis jedis = RedisClient.get().getResource()) {
            String targetStr = target.toString();
            String fromStr = from.toString();

            // incoming/outgoing bereinigen
            jedis.srem(incomingKey(target), fromStr);
            jedis.srem(outgoingKey(from), targetStr);

            // Freundschaft auf beiden Seiten eintragen
            jedis.sadd(friendsKey(target), fromStr);
            jedis.sadd(friendsKey(from), targetStr);
            return true;
        } catch (Exception e) {
            CoreError.of(
                            CoreErrorCode.FRIENDS_REDIS_FAILURE,
                            CoreErrorSeverity.ERROR,
                            "Failed to accept friend request"
                    )
                    .withCause(e)
                    .withContextEntry("target", target.toString())
                    .withContextEntry("from", from.toString())
                    .log();
            return false;
        }
    }

    @Override
    public boolean denyRequest(UUID target, UUID from) {
        try (Jedis jedis = RedisClient.get().getResource()) {
            String targetStr = target.toString();
            String fromStr = from.toString();

            jedis.srem(incomingKey(target), fromStr);
            jedis.srem(outgoingKey(from), targetStr);
            return true;
        } catch (Exception e) {
            CoreError.of(
                            CoreErrorCode.FRIENDS_REDIS_FAILURE,
                            CoreErrorSeverity.WARN,
                            "Failed to deny friend request"
                    )
                    .withCause(e)
                    .withContextEntry("target", target.toString())
                    .withContextEntry("from", from.toString())
                    .log();
            return false;
        }
    }

    @Override
    public boolean removeFriend(UUID a, UUID b) {
        try (Jedis jedis = RedisClient.get().getResource()) {
            String aStr = a.toString();
            String bStr = b.toString();

            jedis.srem(friendsKey(a), bStr);
            jedis.srem(friendsKey(b), aStr);
            return true;
        } catch (Exception e) {
            CoreError.of(
                            CoreErrorCode.FRIENDS_REDIS_FAILURE,
                            CoreErrorSeverity.WARN,
                            "Failed to remove friend"
                    )
                    .withCause(e)
                    .withContextEntry("a", a.toString())
                    .withContextEntry("b", b.toString())
                    .log();
            return false;
        }
    }

    @Override
    public void sendOfflineMessage(OfflineFriendMessage message) {
        try (Jedis jedis = RedisClient.get().getResource()) {
            String key = messagesKey(message.to());
            String json = GSON.toJson(message);
            jedis.lpush(key, json);
            // Optional: TTL setzen, z.B. 30 Tage
            jedis.expire(key, 60 * 60 * 24 * 30);
        } catch (Exception e) {
            CoreError.of(
                            CoreErrorCode.FRIENDS_REDIS_FAILURE,
                            CoreErrorSeverity.WARN,
                            "Failed to store offline friend message"
                    )
                    .withCause(e)
                    .withContextEntry("to", message.to().toString())
                    .log();
        }
    }

    @Override
    public List<OfflineFriendMessage> pollOfflineMessages(UUID target) {
        List<OfflineFriendMessage> result = new ArrayList<>();
        String key = messagesKey(target);

        try (Jedis jedis = RedisClient.get().getResource()) {
            while (true) {
                String json = jedis.rpop(key); // FIFO
                if (json == null) break;

                try {
                    OfflineFriendMessage msg = GSON.fromJson(json, OfflineFriendMessage.class);
                    if (msg != null) result.add(msg);
                } catch (JsonSyntaxException ex) {
                    CoreError.of(
                                    CoreErrorCode.FRIENDS_MESSAGE_PARSE_FAILED,
                                    CoreErrorSeverity.WARN,
                                    "Failed to parse offline friend message JSON"
                            )
                            .withCause(ex)
                            .withContextEntry("payload", json)
                            .log();
                }
            }
        } catch (Exception e) {
            CoreError.of(
                            CoreErrorCode.FRIENDS_REDIS_FAILURE,
                            CoreErrorSeverity.ERROR,
                            "Failed to poll offline friend messages"
                    )
                    .withCause(e)
                    .withContextEntry("target", target.toString())
                    .log();
        }
        return result;
    }

    // -----------------------------------------------------

    private UUID parseUuidSafe(String s) {
        try {
            return UUID.fromString(s);
        } catch (IllegalArgumentException e) {
            LOG.warn("Invalid UUID in friend set: {}", s);
            return null;
        }
    }

    private List<UUID> loadUuidSet(String key) {
        try (Jedis jedis = RedisClient.get().getResource()) {
            Set<String> raw = jedis.smembers(key);
            if (raw == null || raw.isEmpty()) return List.of();
            return raw.stream()
                    .map(this::parseUuidSafe)
                    .filter(Objects::nonNull)
                    .toList();
        } catch (Exception e) {
            CoreError.of(
                            CoreErrorCode.FRIENDS_REDIS_FAILURE,
                            CoreErrorSeverity.WARN,
                            "Failed to load UUID set"
                    )
                    .withCause(e)
                    .withContextEntry("key", key)
                    .log();
            return List.of();
        }
    }
}
