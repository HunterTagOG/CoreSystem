package dev.huntertagog.coresystem.fabric.server.chat;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import dev.huntertagog.coresystem.common.error.CoreError;
import dev.huntertagog.coresystem.common.error.CoreErrorCode;
import dev.huntertagog.coresystem.common.error.CoreErrorSeverity;
import dev.huntertagog.coresystem.common.log.Logger;
import dev.huntertagog.coresystem.common.log.LoggerFactory;
import dev.huntertagog.coresystem.fabric.common.chat.PlayerNicknameService;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.world.PersistentState;
import net.minecraft.world.PersistentStateManager;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public final class PersistentPlayerNicknameService implements PlayerNicknameService {

    private static final Logger LOG = LoggerFactory.get("PlayerNick");

    private final MinecraftServer server;
    private final Cache<UUID, Optional<String>> cache;

    // In-Memory views (backed by state)
    private final Map<UUID, String> byPlayer = new ConcurrentHashMap<>();
    private final Map<String, UUID> byNickLower = new ConcurrentHashMap<>();

    private NickState state;

    public PersistentPlayerNicknameService(MinecraftServer server) {
        this.server = Objects.requireNonNull(server, "server");
        this.cache = Caffeine.newBuilder()
                .expireAfterAccess(Duration.ofMinutes(10))
                .maximumSize(50_000)
                .build();

        this.state = loadState(server);
        // Bootstrap maps from persisted state
        this.byPlayer.putAll(this.state.byPlayer);
        rebuildIndex();
    }

    @Override
    public Optional<String> getNickname(UUID playerId) {
        Optional<String> cached = cache.getIfPresent(playerId);
        if (cached != null) return cached;

        String nick = byPlayer.get(playerId);
        Optional<String> opt = (nick == null || nick.isBlank())
                ? Optional.empty()
                : Optional.of(nick);

        cache.put(playerId, opt);
        return opt;
    }

    @Override
    public String getEffectiveName(ServerPlayerEntity player) {
        return getNickname(player.getUuid())
                .filter(n -> !n.isBlank())
                .orElse(player.getGameProfile().getName());
    }

    @Override
    public void setNickname(UUID playerId, @Nullable String nickname) {
        try {
            // remove old index entry (if any)
            String old = byPlayer.get(playerId);
            if (old != null && !old.isBlank()) {
                removeIndex(old, playerId);
            }

            if (nickname == null || nickname.isBlank()) {
                byPlayer.remove(playerId);
                cache.put(playerId, Optional.empty());
                markDirtyAndPersist();
                return;
            }

            String trimmed = nickname.trim();
            String lower = trimmed.toLowerCase(Locale.ROOT);

            // enforce uniqueness like your Redis-index did (optional but recommended)
            UUID existingOwner = byNickLower.get(lower);
            if (existingOwner != null && !existingOwner.equals(playerId)) {
                // nickname is already taken → you can also throw a CoreError if you prefer
                CoreError.of(
                                CoreErrorCode.PLAYER_NICK_REDIS_SAVE_FAILED,
                                CoreErrorSeverity.WARN,
                                "Nickname already in use (local index)"
                        )
                        .withContextEntry("nick", trimmed)
                        .withContextEntry("existingOwner", existingOwner.toString())
                        .withContextEntry("playerUuid", playerId.toString())
                        .log();
                return;
            }

            byPlayer.put(playerId, trimmed);
            byNickLower.put(lower, playerId);

            cache.put(playerId, Optional.of(trimmed));
            markDirtyAndPersist();
        } catch (Exception e) {
            CoreError.of(
                            CoreErrorCode.PLAYER_NICK_REDIS_SAVE_FAILED,
                            CoreErrorSeverity.WARN,
                            "Failed to set nickname (local persistent state)"
                    )
                    .withCause(e)
                    .withContextEntry("playerUuid", playerId.toString())
                    .withContextEntry("nick", nickname == null ? "<null>" : nickname)
                    .log();
        }
    }

    @Override
    public void clearNickname(UUID playerId) {
        setNickname(playerId, null);
    }

    @Override
    public Optional<UUID> findByNickname(String nickname) {
        if (nickname == null || nickname.isBlank()) return Optional.empty();
        String lower = nickname.toLowerCase(Locale.ROOT);
        return Optional.ofNullable(byNickLower.get(lower));
    }

    private void removeIndex(String oldNickname, UUID playerId) {
        String lower = oldNickname.toLowerCase(Locale.ROOT);
        UUID current = byNickLower.get(lower);
        if (playerId.equals(current)) {
            byNickLower.remove(lower);
        }
    }

    private void rebuildIndex() {
        byNickLower.clear();
        for (var entry : byPlayer.entrySet()) {
            String nick = entry.getValue();
            if (nick == null || nick.isBlank()) continue;
            byNickLower.put(nick.toLowerCase(Locale.ROOT), entry.getKey());
        }
    }

    private void markDirtyAndPersist() {
        // sync view → state
        state.byPlayer.clear();
        state.byPlayer.putAll(byPlayer);
        state.markDirty();
    }

    private static NickState loadState(MinecraftServer server) {
        PersistentStateManager psm = server.getOverworld().getPersistentStateManager();
        return psm.getOrCreate(NickState.TYPE, NickState.ID);
    }

    // ===== PersistentState =====

    private static final class NickState extends PersistentState {
        static final String ID = "coresystem_player_nicks";

        final Map<UUID, String> byPlayer = new HashMap<>();

        static final Type<NickState> TYPE = new Type<>(
                NickState::new,
                NickState::fromNbt,
                null
        );

        private NickState() {
        }

        @Override
        public NbtCompound writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registries) {
            NbtList list = new NbtList();
            for (var entry : byPlayer.entrySet()) {
                NbtCompound e = new NbtCompound();
                e.putString("uuid", entry.getKey().toString());
                e.putString("nick", entry.getValue());
                list.add(e);
            }
            nbt.put("entries", list);
            return nbt;
        }

        static NickState fromNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registries) {
            NickState s = new NickState();
            NbtList list = nbt.getList("entries", 10);
            for (int i = 0; i < list.size(); i++) {
                NbtCompound e = list.getCompound(i);
                String uuidStr = e.getString("uuid");
                String nick = e.getString("nick");
                try {
                    UUID uuid = UUID.fromString(uuidStr);
                    if (nick != null && !nick.isBlank()) {
                        s.byPlayer.put(uuid, nick);
                    }
                } catch (Exception ignored) {
                }
            }
            return s;
        }
    }
}
