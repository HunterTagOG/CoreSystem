package dev.huntertagog.coresystem.fabric.common.player;

import dev.huntertagog.coresystem.common.error.CoreError;
import dev.huntertagog.coresystem.common.error.CoreErrorCode;
import dev.huntertagog.coresystem.common.error.CoreErrorSeverity;
import dev.huntertagog.coresystem.common.player.PlayerProfileLookup;
import dev.huntertagog.coresystem.common.provider.ServiceProvider;
import dev.huntertagog.coresystem.platform.player.PlayerProfile;
import dev.huntertagog.coresystem.platform.player.PlayerProfileService;
import dev.huntertagog.coresystem.platform.settings.PlayerSettingKey;
import dev.huntertagog.coresystem.platform.settings.PlayerSettingsService;
import lombok.Getter;
import net.minecraft.server.network.ServerPlayerEntity;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.UUID;

public final class PlayerContext {

    private final UUID uniqueId;
    private final String name;
    private final PlayerProfile profile;
    @Getter
    private final boolean firstJoin;
    private final PlayerSettingsService settingsService;

    private PlayerContext(PlayerProfile profile, boolean firstJoin) {
        this.uniqueId = profile.getUniqueId();
        this.name = profile.getName();
        this.profile = profile;
        this.firstJoin = firstJoin;
        this.settingsService = ServiceProvider.getService(PlayerSettingsService.class);
    }

    // ----------------------------------------------------
    // Static Factory – "neutrales" Laden (kein Join-Update)
    // ----------------------------------------------------
    public static PlayerContext of(ServerPlayerEntity player) {
        PlayerProfileService service = resolveService();
        UUID uuid = player.getUuid();
        String fallbackName = player.getGameProfile().getName();

        PlayerProfile profile = safeGetOrCreate(service, uuid, fallbackName);
        boolean firstJoin = profile.getTotalJoins() <= 1; // heuristisch

        return new PlayerContext(profile, firstJoin);
    }

    public static PlayerContext of(UUID uniqueId, @Nullable String fallbackName) {
        PlayerProfileService service = resolveService();
        PlayerProfile profile = safeGetOrCreate(service, uniqueId, fallbackName);
        boolean firstJoin = profile.getTotalJoins() <= 1;
        return new PlayerContext(profile, firstJoin);
    }

    // ----------------------------------------------------
    // Factory für Join-Flow – kapselt updateOnJoin
    // ----------------------------------------------------
    public static PlayerContext onJoin(ServerPlayerEntity player,
                                       String currentServerName,
                                       @Nullable String nodeId) {
        PlayerProfileService service = resolveService();
        UUID uuid = player.getUuid();
        String name = player.getGameProfile().getName();

        // Pre-State (für firstJoin-Berechnung)
        Optional<PlayerProfile> existingOpt = safeFind(service, uuid);

        // Erweiterungsvorschlag: updateOnJoin gibt PlayerProfile zurück
        PlayerProfile updated = safeUpdateOnJoin(service, player, currentServerName, nodeId);

        boolean firstJoin = existingOpt.isEmpty() || updated.getTotalJoins() <= 1;
        return new PlayerContext(updated, firstJoin);
    }

    // ----------------------------------------------------
    // Factory für Quit-Flow – kapselt updateOnQuit
    // ----------------------------------------------------
    public static PlayerContext onQuit(ServerPlayerEntity player,
                                       String currentServerName,
                                       @Nullable String nodeId) {
        PlayerProfileService service = resolveService();
        UUID uuid = player.getUuid();

        Optional<PlayerProfile> existingOpt = safeFind(service, uuid);

        PlayerProfile updated = safeUpdateOnQuit(service, player, currentServerName, nodeId)
                .orElseGet(() -> existingOpt.orElseGet(
                        () -> fallbackProfile(uuid, player.getGameProfile().getName())
                ));

        boolean firstJoin = updated.getTotalJoins() <= 1;
        return new PlayerContext(updated, firstJoin);
    }

    // ----------------------------------------------------
    // Öffentliche API
    // ----------------------------------------------------

    public PlayerProfile profile() {
        return this.profile;
    }

    public UUID uniqueId() {
        return this.uniqueId;
    }

    public String name() {
        return this.name;
    }

    public long getFirstSeenAt() {
        return profile.getFirstSeenAt();
    }

    public long getLastSeenAt() {
        return profile.getLastSeenAt();
    }

    public int getTotalJoins() {
        return profile.getTotalJoins();
    }

    @Nullable
    public String getLastServer() {
        return profile.getLastServer();
    }

    @Nullable
    public String getLastNodeId() {
        return profile.getLastNodeId();
    }

    // ---------------- Settings-Access ----------------

    public <T> T getSetting(PlayerSettingKey<T> key) {
        if (settingsService == null) {
            return key.defaultValue();
        }
        return settingsService.getOrDefault(profile.getUniqueId(), key);
    }

    public <T> void setSetting(PlayerSettingKey<T> key, T value) {
        if (settingsService == null) {
            return;
        }
        settingsService.set(profile.getUniqueId(), key, value);
    }

    public <T> void clearSetting(PlayerSettingKey<T> key) {
        if (settingsService == null) {
            return;
        }
        settingsService.clear(profile.getUniqueId(), key);
    }

    // ----------------------------------------------------
    // Internal Helpers – inkl. CoreError-Integration
    // ----------------------------------------------------

    private static PlayerProfileService resolveService() {
        PlayerProfileService service = ServiceProvider.getService(PlayerProfileService.class);
        if (service == null) {
            CoreError.of(CoreErrorCode.PLAYERPROFILE_SERVICE_MISSING,
                            CoreErrorSeverity.CRITICAL,
                            "PlayerProfileService not registered in ServiceProvider")
                    .log();
            throw new IllegalStateException("PlayerProfileService not available");
        }
        return service;
    }

    private static PlayerProfile safeGetOrCreate(PlayerProfileService service,
                                                 UUID uniqueId,
                                                 @Nullable String fallbackName) {
        try {
            return service.getOrCreate(uniqueId, fallbackName);
        } catch (Exception e) {
            CoreError.of(CoreErrorCode.PLAYERPROFILE_LOAD_FAILED,
                            CoreErrorSeverity.ERROR,
                            "Failed to getOrCreate PlayerProfile. Falling back to in-memory profile.")
                    .withContextEntry("uuid", uniqueId.toString())
                    .withContextEntry("fallbackName", String.valueOf(fallbackName))
                    .log();

            return fallbackProfile(uniqueId, fallbackName);
        }
    }

    private static Optional<PlayerProfile> safeFind(PlayerProfileService service,
                                                    UUID uniqueId) {
        try {
            return service.find(uniqueId);
        } catch (Exception e) {
            CoreError.of(CoreErrorCode.PLAYERPROFILE_FIND_FAILED,
                            CoreErrorSeverity.WARN,
                            "Failed to find PlayerProfile from backend.")
                    .withContextEntry("uuid", uniqueId.toString())
                    .log();
            return Optional.empty();
        }
    }

    private static PlayerProfile safeUpdateOnJoin(PlayerProfileService service,
                                                  ServerPlayerEntity player,
                                                  String currentServerName,
                                                  @Nullable String nodeId) {
        try {
            // Erweiterungsvorschlag: updateOnJoin gibt PlayerProfile zurück
            return service.updateOnJoin(player.getUuid(), player.getGameProfile().getName(), currentServerName, nodeId);
        } catch (Exception e) {
            CoreError.of(CoreErrorCode.PLAYERPROFILE_UPDATE_ON_JOIN_FAILED,
                            CoreErrorSeverity.ERROR,
                            "Failed to update PlayerProfile on join. Continuing with fallback profile.")
                    .withContextEntry("uuid", player.getUuidAsString())
                    .withContextEntry("server", currentServerName)
                    .withContextEntry("nodeId", String.valueOf(nodeId))
                    .log();

            return fallbackProfile(player.getUuid(), player.getGameProfile().getName());
        }
    }

    private static Optional<PlayerProfile> safeUpdateOnQuit(PlayerProfileService service,
                                                            ServerPlayerEntity player,
                                                            String currentServerName,
                                                            @Nullable String nodeId) {
        try {
            // Erweiterungsvorschlag: updateOnQuit gibt Optional<PlayerProfile> zurück
            return service.updateOnQuit(player.getUuid(), currentServerName, nodeId);
        } catch (Exception e) {
            CoreError.of(CoreErrorCode.PLAYERPROFILE_UPDATE_ON_QUIT_FAILED,
                            CoreErrorSeverity.WARN,
                            "Failed to update PlayerProfile on quit. Ignoring and continuing.")
                    .withContextEntry("uuid", player.getUuidAsString())
                    .withContextEntry("server", currentServerName)
                    .withContextEntry("nodeId", String.valueOf(nodeId))
                    .log();
            return Optional.empty();
        }
    }

    private static PlayerProfile fallbackProfile(UUID uniqueId, @Nullable String name) {
        long now = System.currentTimeMillis();
        String safeName = (name != null && !name.isBlank()) ? name : uniqueId.toString();

        // Annahme: statische Factory im Value Object
        return PlayerProfile.newEphemeral(uniqueId, safeName, now);
    }

    public Optional<PlayerProfileLookup.LastLocation> resolveLastLocation(UUID other) {
        return PlayerProfileLookup.lastLocation(other);
    }
}
