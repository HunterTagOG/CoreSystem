package dev.huntertagog.coresystem.common.chat;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import dev.huntertagog.coresystem.common.error.CoreError;
import dev.huntertagog.coresystem.common.error.CoreErrorCode;
import dev.huntertagog.coresystem.common.error.CoreErrorSeverity;
import dev.huntertagog.coresystem.common.log.Logger;
import dev.huntertagog.coresystem.common.log.LoggerFactory;
import dev.huntertagog.coresystem.common.redis.RedisClient;
import dev.huntertagog.coresystem.platform.chat.ChatFilterService;
import redis.clients.jedis.Jedis;

import java.time.Duration;
import java.util.*;
import java.util.regex.Pattern;

public final class RedisChatFilterService implements ChatFilterService {

    private static final Logger LOG = LoggerFactory.get("ChatFilter");
    private static final String KEY_BADWORDS = "cs:chatfilter:badwords";

    // Basis-Badword-Set (lowercase)
    private static final Set<String> DEFAULT_BADWORDS = Set.of(
            // deutsch
            "fick", "ficken", "hurensohn", "hure", "arsch", "arschloch",
            "fotze", "schlampe", "spast", "spasti", "wichser",
            "verpiss", "verpiss dich",
            // englisch
            "fuck", "fucking", "bitch", "asshole", "bastard",
            "kys",
            // kritisch / hate
            "nigger", "negro", "hitler", "nazi"
    );

    // wir halten eine lowercase-Set-View im Cache
    private final Cache<String, Set<String>> badWordCache;

    public RedisChatFilterService() {
        this.badWordCache = Caffeine.newBuilder()
                .expireAfterWrite(Duration.ofSeconds(30))
                .maximumSize(1)
                .build();
    }

    /**
     * Einmalige / idempotente Initial-Befüllung mit Basis-Badwords.
     * <p>
     * Kann z. B. beim Systemstart aufgerufen werden:
     * ServiceProvider.getService(ChatFilterService.class).bootstrapDefaultBadWords();
     */
    public void bootstrapDefaultBadWords() {
        long added = 0L;

        try (Jedis jedis = RedisClient.get().getResource()) {
            for (String raw : DEFAULT_BADWORDS) {
                if (raw == null || raw.isBlank()) continue;
                String normalized = raw.trim().toLowerCase(Locale.ROOT);
                // SADD gibt 1 zurück, wenn neu hinzugefügt, 0 falls bereits vorhanden
                Long r = jedis.sadd(KEY_BADWORDS, normalized);
                if (r != null && r > 0) {
                    added += r;
                }
            }
        } catch (Exception e) {
            CoreError.of(
                            CoreErrorCode.CHATFILTER_REDIS_SAVE_FAILED,
                            CoreErrorSeverity.WARN,
                            "Failed to bootstrap default chat badwords to Redis"
                    )
                    .withCause(e)
                    .withContextEntry("redisKey", KEY_BADWORDS)
                    .log();
            return;
        }

        badWordCache.invalidateAll();

        LOG.info("ChatFilter bootstrap completed. Added {} default badword(s).", added);
    }

    private Set<String> loadBadWordsFromRedis() {
        try (Jedis jedis = RedisClient.get().getResource()) {
            Set<String> raw = jedis.smembers(KEY_BADWORDS);
            if (raw == null || raw.isEmpty()) {
                return Collections.emptySet();
            }
            Set<String> normalized = new HashSet<>();
            for (String s : raw) {
                if (s != null && !s.isBlank()) {
                    normalized.add(s.toLowerCase(Locale.ROOT));
                }
            }
            return normalized;
        } catch (Exception e) {
            CoreError.of(
                            CoreErrorCode.CHATFILTER_REDIS_LOAD_FAILED,
                            CoreErrorSeverity.WARN,
                            "Failed to load chat badwords from Redis"
                    )
                    .withCause(e)
                    .withContextEntry("redisKey", KEY_BADWORDS)
                    .log();
            return Collections.emptySet();
        }
    }

    private Set<String> getCachedBadWords() {
        return badWordCache.get("badwords", k -> loadBadWordsFromRedis());
    }

    @Override
    public String filter(String raw) {
        if (raw == null || raw.isEmpty()) {
            return raw;
        }

        Set<String> badWords = getCachedBadWords();
        if (badWords.isEmpty()) {
            return raw;
        }

        String filtered = raw;

        // simple, aber effektiv: für jedes Badword -> regex replace, case-insensitive
        for (String bad : badWords) {
            if (bad.isBlank()) {
                continue;
            }

            String patternString = "(?i)" + Pattern.quote(bad);
            String replacement = mask(bad.length());

            filtered = filtered.replaceAll(patternString, replacement);
        }

        return filtered;
    }

    @Override
    public void addBadWord(String word) {
        if (word == null || word.isBlank()) return;

        String normalized = word.trim().toLowerCase(Locale.ROOT);

        try (Jedis jedis = RedisClient.get().getResource()) {
            jedis.sadd(KEY_BADWORDS, normalized);
        } catch (Exception e) {
            CoreError.of(
                            CoreErrorCode.CHATFILTER_REDIS_SAVE_FAILED,
                            CoreErrorSeverity.WARN,
                            "Failed to add badword to Redis"
                    )
                    .withCause(e)
                    .withContextEntry("word", normalized)
                    .withContextEntry("redisKey", KEY_BADWORDS)
                    .log();
        }

        badWordCache.invalidateAll();
    }

    @Override
    public void removeBadWord(String word) {
        if (word == null || word.isBlank()) return;

        String normalized = word.trim().toLowerCase(Locale.ROOT);

        try (Jedis jedis = RedisClient.get().getResource()) {
            jedis.srem(KEY_BADWORDS, normalized);
        } catch (Exception e) {
            CoreError.of(
                            CoreErrorCode.CHATFILTER_REDIS_SAVE_FAILED,
                            CoreErrorSeverity.WARN,
                            "Failed to remove badword from Redis"
                    )
                    .withCause(e)
                    .withContextEntry("word", normalized)
                    .withContextEntry("redisKey", KEY_BADWORDS)
                    .log();
        }

        badWordCache.invalidateAll();
    }

    @Override
    public Set<String> getBadWords() {
        return Collections.unmodifiableSet(getCachedBadWords());
    }

    private static String mask(int length) {
        if (length <= 0) return "*";
        char[] arr = new char[length];
        Arrays.fill(arr, '*');
        return new String(arr);
    }
}
