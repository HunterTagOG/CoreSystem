package dev.huntertagog.coresystem.fabric.server.command.impl;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import dev.huntertagog.coresystem.common.command.BaseCommand;
import dev.huntertagog.coresystem.common.error.CoreError;
import dev.huntertagog.coresystem.common.error.CoreErrorCode;
import dev.huntertagog.coresystem.common.error.CoreErrorSeverity;
import dev.huntertagog.coresystem.common.health.HealthMonitorService;
import dev.huntertagog.coresystem.common.islands.RedisPrivateIslandNodeRepository;
import dev.huntertagog.coresystem.common.model.ServerTargets;
import dev.huntertagog.coresystem.common.permission.CorePermission;
import dev.huntertagog.coresystem.common.permission.PermissionKeys;
import dev.huntertagog.coresystem.common.provider.ServiceProvider;
import dev.huntertagog.coresystem.common.ratelimit.RateLimitRules;
import dev.huntertagog.coresystem.common.text.CoreMessage;
import dev.huntertagog.coresystem.fabric.common.audit.FabricAuditContext;
import dev.huntertagog.coresystem.fabric.common.error.CoreErrorUtil;
import dev.huntertagog.coresystem.fabric.common.permission.PermissionService;
import dev.huntertagog.coresystem.fabric.common.ratelimit.RateLimitUtil;
import dev.huntertagog.coresystem.fabric.common.text.Messages;
import dev.huntertagog.coresystem.fabric.server.command.CommandSuggestions;
import dev.huntertagog.coresystem.fabric.server.command.CommandUtil;
import dev.huntertagog.coresystem.fabric.server.command.CommandsProvider;
import dev.huntertagog.coresystem.fabric.server.command.CoreCommand;
import dev.huntertagog.coresystem.fabric.server.islands.PrivateIslandWorldNodeConfig;
import dev.huntertagog.coresystem.fabric.server.permission.PermissionCache;
import dev.huntertagog.coresystem.fabric.server.world.structures.StructureSpawnService;
import dev.huntertagog.coresystem.platform.audit.AuditLogService;
import dev.huntertagog.coresystem.platform.command.CommandMeta;
import dev.huntertagog.coresystem.platform.dev.DevToolsService;
import dev.huntertagog.coresystem.platform.feature.FeatureToggleKey;
import dev.huntertagog.coresystem.platform.feature.FeatureToggleService;
import dev.huntertagog.coresystem.platform.health.HealthCheckResult;
import dev.huntertagog.coresystem.platform.health.HealthStatus;
import dev.huntertagog.coresystem.platform.islands.PrivateIslandWorldNodeStatus;
import dev.huntertagog.coresystem.platform.metrics.MetricsService;
import dev.huntertagog.coresystem.platform.module.CoreModule;
import dev.huntertagog.coresystem.platform.module.ModuleRegistryService;
import dev.huntertagog.coresystem.platform.player.PlayerProfile;
import dev.huntertagog.coresystem.platform.player.PlayerProfileService;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;
import org.jetbrains.annotations.NotNull;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;

import static net.minecraft.server.command.CommandManager.literal;

@CommandMeta(value = "coresystem_reload_structures", permission = PermissionKeys.HOME_USE, enabled = true)
public final class CoreSystemCommand extends BaseCommand implements CoreCommand {

    public CoreSystemCommand() {
    }

    private final PermissionService perms = ServiceProvider.getService(PermissionService.class);

    private static final DateTimeFormatter DEBUG_TS =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
                    .withZone(ZoneId.systemDefault());

    @Override
    public void register(CommandDispatcher<ServerCommandSource> dispatcher,
                         CommandRegistryAccess registryAccess,
                         CommandManager.RegistrationEnvironment environment) {

        dispatcher.register(
                literal("cs")
                        // /cs structures reload
                        .then(
                                CommandUtil.literalWithPermission(this, "structures", CorePermission.ADMIN_SRUCTURES)
                                        .then(literal("reload")
                                                .executes(ctx -> {
                                                    trackCommandUsage("cs_structures_reload", ctx.getSource());

                                                    ServerCommandSource src = ctx.getSource();
                                                    StructureSpawnService service = ServiceProvider.getService(StructureSpawnService.class);

                                                    if (service == null) {
                                                        CoreError error = CoreError.of(
                                                                        CoreErrorCode.SERVICE_MISSING,
                                                                        CoreErrorSeverity.CRITICAL,
                                                                        "StructureSpawnService not available for /cs structures reload"
                                                                )
                                                                .withContextEntry("service", "StructureSpawnService")
                                                                .withContextEntry("command", "cs structures reload");
                                                        CoreErrorUtil.notify(src, error);
                                                        return 0;
                                                    }

                                                    try {
                                                        service.reload(src.getServer());
                                                    } catch (Exception ex) {
                                                        CoreError error = new CoreError(
                                                                CoreErrorCode.STRUCTURE_RELOAD_FAILED,
                                                                CoreErrorSeverity.ERROR,
                                                                "Failed to reload structure spawn config",
                                                                ex,
                                                                Map.of(
                                                                        "command", "cs structures reload",
                                                                        "source", src.getName()
                                                                )
                                                        );
                                                        CoreErrorUtil.notify(src, error);
                                                        return 0;
                                                    }

                                                    src.sendFeedback(
                                                            () -> Messages.t(CoreMessage.STRUCTURE_RELOAD_SUCCESS),
                                                            true
                                                    );
                                                    return 1;
                                                }))
                        )
                        // /cs whereami
                        .then(
                                CommandUtil.literalWithPermission(this, "whereami", CorePermission.ISLAND_ADMIN)
                                        .executes(ctx -> {
                                            trackCommandUsage("cs_whereami", ctx.getSource());

                                            ServerCommandSource src = ctx.getSource();
                                            ServerPlayerEntity player = src.getPlayer();
                                            if (player == null) {
                                                Messages.send(src, CoreMessage.ONLY_PLAYERS);
                                                return 0;
                                            }

                                            World world = player.getWorld();
                                            Identifier worldId = world.getRegistryKey().getValue();

                                            boolean isIsland = PrivateIslandWorldNodeConfig.isIslandWorldKey(world.getRegistryKey());

                                            src.sendMessage(Messages.t(CoreMessage.WHEREAMI_HEADER));
                                            src.sendMessage(Messages.t(CoreMessage.WHEREAMI_WORLD_ID, worldId.toString()));
                                            src.sendMessage(Messages.t(CoreMessage.WHEREAMI_DIM_TYPE, world.getDimension().toString()));
                                            src.sendMessage(Messages.t(CoreMessage.WHEREAMI_IS_ISLAND, isIsland));

                                            if (isIsland) {
                                                String path = worldId.getPath(); // island/<uuid>
                                                String[] parts = path.split("/");
                                                if (parts.length == 2) {
                                                    String uuid = parts[1];
                                                    src.sendMessage(Messages.t(CoreMessage.WHEREAMI_ISLAND_OWNER, uuid));
                                                }

                                                var key = world.getRegistryKey();
                                                var resolved = PrivateIslandWorldNodeConfig
                                                        .resolveIslandPath(PrivateIslandWorldNodeConfig.getIslandsBasePath(), key);

                                                src.sendMessage(Messages.t(CoreMessage.WHEREAMI_ISLAND_FILESYSTEM, resolved.toString()));
                                            }

                                            src.sendMessage(Messages.t(CoreMessage.WHEREAMI_FOOTER));

                                            return 1;
                                        })
                        )
                        // /cs dev ...
                        .then(
                                CommandUtil.literalWithPermission(this, "dev", CorePermission.STAFF_DEBUG)
                                        .then(literal("env")
                                                .executes(this::executeDevEnv)
                                        )
                                        .then(literal("simulate")
                                                .then(literal("join")
                                                        .then(CommandManager.argument("name", StringArgumentType.word())
                                                                .executes(this::executeDevSimulateJoin)
                                                        )
                                                )
                                                .then(literal("quit")
                                                        .then(CommandManager.argument("name", StringArgumentType.word())
                                                                .executes(this::executeDevSimulateQuit)
                                                        )
                                                )
                                                // /cs dev simulate burst <count>
                                                .then(literal("burst")
                                                        .then(CommandManager.argument("count", IntegerArgumentType.integer(1, 5000))
                                                                .executes(this::executeDevSimulateBurst)
                                                        )
                                                )
                                        )
                                        // /cs dev economy smoke
                                        .then(literal("economy")
                                                .then(literal("smoke")
                                                        .executes(this::executeDevEconomySmoke)
                                                )
                                        )
                                        // /cs dev redis ping
                                        .then(literal("redis")
                                                .then(literal("ping")
                                                        .executes(this::executeDevRedisPing)
                                                )
                                        )
                        )
                        // /cs health
                        .then(
                                CommandUtil.literalWithPermission(this, "health", CorePermission.STAFF_DEBUG)
                                        .executes(this::executeHealth)
                        )
                        // /cs debug ...
                        .then(
                                CommandUtil.literalWithPermission(this, "debug", CorePermission.STAFF_DEBUG)
                                        .then(literal("perms")
                                                // /cs debug perms → Profilblock + alle CorePermissions
                                                .executes(this::executeListAllPermissions)
                                                // /cs debug perms used
                                                .then(literal("used")
                                                        .executes(this::executeListUsedPermissions))
                                                // /cs debug perms commands
                                                .then(literal("commands")
                                                        .executes(this::executeListPermissionCommandMapping))
                                                // /cs debug perms <permission>
                                                .then(CommandManager.argument("permission", StringArgumentType.word())
                                                        .suggests(CommandSuggestions.corePermissions())
                                                        .executes(this::executePermissionDetails))
                                        )
                                        // /cs debug islandnodes
                                        .then(literal("islandnodes")
                                                .executes(ctx -> {
                                                    trackCommandUsage("cs_debug_islandnodes", ctx.getSource());

                                                    ServerCommandSource source = ctx.getSource();

                                                    Collection<PrivateIslandWorldNodeStatus> nodes;
                                                    try {
                                                        RedisPrivateIslandNodeRepository redisRepo = ServiceProvider.getService(RedisPrivateIslandNodeRepository.class);
                                                        nodes = redisRepo.getAllStatuses();
                                                    } catch (Exception ex) {
                                                        CoreError error = new CoreError(
                                                                CoreErrorCode.REDIS_FAILURE,
                                                                CoreErrorSeverity.ERROR,
                                                                "Failed to load island node statuses",
                                                                ex,
                                                                Map.of(
                                                                        "command", "cs debug islandnodes",
                                                                        "serverName", PrivateIslandWorldNodeConfig.SERVER_NAME
                                                                )
                                                        );
                                                        CoreErrorUtil.notify(source, error);
                                                        return 0;
                                                    }

                                                    if (nodes == null || nodes.isEmpty()) {
                                                        source.sendFeedback(
                                                                () -> Messages.t(CoreMessage.ISLAND_NODES_NONE),
                                                                false
                                                        );
                                                        return 1;
                                                    }

                                                    long now = System.currentTimeMillis();

                                                    source.sendFeedback(
                                                            () -> Messages.t(CoreMessage.ISLAND_NODES_HEADER, nodes.size()),
                                                            false
                                                    );

                                                    for (PrivateIslandWorldNodeStatus status : nodes) {
                                                        buildLine(status, now);
                                                        source.sendFeedback(
                                                                () -> Messages.t(
                                                                        CoreMessage.ISLAND_NODES_LINE,
                                                                        status.nodeId(),
                                                                        status.serverName(),
                                                                        getOnlineColor(status, now),
                                                                        getOnlineText(status, now),
                                                                        status.currentIslands(),
                                                                        status.maxIslands(),
                                                                        calcIslandLoad(status),
                                                                        status.currentPlayers(),
                                                                        status.maxPlayers(),
                                                                        calcPlayerLoad(status),
                                                                        getAgeSec(status, now)
                                                                ),
                                                                false
                                                        );
                                                    }

                                                    return 1;
                                                }))
                        )
        );
    }

    // ------------------------------------------------------------------------
    // /cs debug perms <permission> → Detail-View zu einer einzelnen Permission
    // inkl. Profil-Block am Anfang
    // ------------------------------------------------------------------------

    private int executePermissionDetails(CommandContext<ServerCommandSource> ctx) {
        trackCommandUsage("cs_debug_perms_detail", ctx.getSource());

        ServerCommandSource source = ctx.getSource();
        String input = StringArgumentType.getString(ctx, "permission");

        Optional<CorePermission> match = Arrays.stream(CorePermission.values())
                .filter(p -> p.key().equalsIgnoreCase(input) || p.name().equalsIgnoreCase(input))
                .findFirst();

        if (match.isEmpty()) {
            CoreError error = CoreError.of(
                            CoreErrorCode.INVALID_ARGUMENT,
                            CoreErrorSeverity.WARN,
                            "Unknown permission: " + input
                    )
                    .withContextEntry("argument", input)
                    .withContextEntry("command", "cs debug perms <permission>");
            CoreErrorUtil.notify(source, error);
            return 0;
        }

        // Profil + Level vorweg
        sendProfileDebug(source);

        CorePermission perm = match.get();
        hasPerms(source, perms, perm);
        return 1;
    }

    // ------------------------------------------------------------------------
    // /cs debug perms  → alle CorePermissions mit "has / has not"
    // + PlayerProfile + ServerSwitch-Level
    // ------------------------------------------------------------------------

    private int executeListAllPermissions(CommandContext<ServerCommandSource> ctx) {
        trackCommandUsage("cs_debug_perms_all", ctx.getSource());

        ServerCommandSource source = ctx.getSource();

        // Erst Spielerprofil / Level
        sendProfileDebug(source);

        source.sendFeedback(
                () -> Messages.t(CoreMessage.PERMDEBUG_ALL_HEADER),
                false
        );

        for (CorePermission perm : CorePermission.values()) {
            hasPerms(source, perms, perm);
        }

        return 1;
    }

    /**
     * Debug-Block für aktuelles Player-Profil + ServerSwitch-Level.
     * Verwendet das Message-System und formatiert Timestamps sauber.
     */
    private void sendProfileDebug(ServerCommandSource source) {
        ServerPlayerEntity player = source.getPlayer();
        if (player == null) {
            return;
        }

        PlayerProfileService profileService = ServiceProvider.getService(PlayerProfileService.class);
        if (profileService == null) {
            CoreError error = CoreError.of(
                            CoreErrorCode.SERVICE_MISSING,
                            CoreErrorSeverity.ERROR,
                            "PlayerProfileService not available for permission debug"
                    )
                    .withContextEntry("command", "cs debug perms")
                    .withContextEntry("player", player.getGameProfile().getName());
            CoreErrorUtil.notify(source, error);
            return;
        }

        PlayerProfile profile = profileService.getOrCreate(player.getUuid(), player.getGameProfile().getName());
        ServerTargets.Level level = PermissionCache.getLevel(player);

        // long -> formatted
        String firstSeen = formatTimestamp(profile.getFirstSeenAt());
        String lastSeen = formatTimestamp(profile.getLastSeenAt());

        String totalJoins = String.valueOf(profile.getTotalJoins());
        String lastServer = profile.getLastServer() != null ? profile.getLastServer() : "-";
        String lastNode = profile.getLastNodeId() != null ? profile.getLastNodeId() : "-";

        source.sendFeedback(
                () -> Messages.t(CoreMessage.PERMDEBUG_PROFILE_HEADER).copy().formatted(Formatting.DARK_GRAY),
                false
        );

        source.sendFeedback(
                () -> Messages.t(CoreMessage.PERMDEBUG_PROFILE_NAME, profile.getName()).copy().formatted(Formatting.GRAY),
                false
        );
        source.sendFeedback(
                () -> Messages.t(CoreMessage.PERMDEBUG_PROFILE_UUID, profile.getUniqueId().toString()).copy().formatted(Formatting.GRAY),
                false
        );
        source.sendFeedback(
                () -> Messages.t(CoreMessage.PERMDEBUG_PROFILE_FIRST_SEEN, firstSeen).copy().formatted(Formatting.GRAY),
                false
        );
        source.sendFeedback(
                () -> Messages.t(CoreMessage.PERMDEBUG_PROFILE_LAST_SEEN, lastSeen).copy().formatted(Formatting.GRAY),
                false
        );
        source.sendFeedback(
                () -> Messages.t(CoreMessage.PERMDEBUG_PROFILE_TOTAL_JOINS, totalJoins).copy().formatted(Formatting.GRAY),
                false
        );
        source.sendFeedback(
                () -> Messages.t(CoreMessage.PERMDEBUG_PROFILE_LAST_SERVER, lastServer).copy().formatted(Formatting.GRAY),
                false
        );
        source.sendFeedback(
                () -> Messages.t(CoreMessage.PERMDEBUG_PROFILE_LAST_NODE, lastNode).copy().formatted(Formatting.GRAY),
                false
        );
        source.sendFeedback(
                () -> Messages.t(CoreMessage.PERMDEBUG_PROFILE_SWITCH_LEVEL, level.name()).copy().formatted(Formatting.GOLD),
                false
        );
    }

    /**
     * Hilfsmethode: formatiert einen Instant defensiv.
     */
    private String formatTimestamp(long epochMillis) {
        if (epochMillis <= 0) {
            return "-";
        }
        return DEBUG_TS.format(Instant.ofEpochMilli(epochMillis));
    }

    private void hasPerms(ServerCommandSource source, PermissionService perms, CorePermission perm) {
        boolean has = perms.has(source, perm);
        Formatting color = has ? Formatting.GREEN : Formatting.RED;
        String status = has ? "✔" : "✖";

        source.sendFeedback(
                () -> Messages.t(
                        CoreMessage.PERMDEBUG_LINE,
                        status,
                        perm.key(),
                        perm.description(),
                        perm.defaultGroup()
                ).copy().formatted(color),
                false
        );
    }

    // ------------------------------------------------------------------------
    // /cs debug perms used  → nur Permissions, die tatsächlich von Commands genutzt werden
    // ------------------------------------------------------------------------

    private int executeListUsedPermissions(CommandContext<ServerCommandSource> ctx) {
        trackCommandUsage("cs_debug_perms_used", ctx.getSource());

        ServerCommandSource source = ctx.getSource();
        CommandsProvider commandsProvider = ServiceProvider.getService(CommandsProvider.class);

        if (commandsProvider == null) {
            CoreError error = CoreError.of(
                            CoreErrorCode.SERVICE_MISSING,
                            CoreErrorSeverity.ERROR,
                            "CommandsProvider not available for /cs debug perms used"
                    )
                    .withContextEntry("command", "cs debug perms used");
            CoreErrorUtil.notify(source, error);
            return 0;
        }

        Set<CorePermission> used = commandsProvider.getUsedPermissions();

        source.sendFeedback(
                () -> Messages.t(CoreMessage.PERMDEBUG_USED_HEADER),
                false
        );

        if (used.isEmpty()) {
            source.sendFeedback(
                    () -> Messages.t(CoreMessage.PERMDEBUG_USED_NONE),
                    false
            );
            return 1;
        }

        for (CorePermission perm : used) {
            hasPerms(source, perms, perm);
        }

        return 1;
    }

    // ------------------------------------------------------------------------
    // /cs debug perms commands  → Permission → Command-Klassen, die sie nutzen
    // ------------------------------------------------------------------------

    private int executeListPermissionCommandMapping(CommandContext<ServerCommandSource> ctx) {
        trackCommandUsage("cs_debug_perms_commands", ctx.getSource());

        ServerCommandSource source = ctx.getSource();
        CommandsProvider commandsProvider = ServiceProvider.getService(CommandsProvider.class);

        if (commandsProvider == null) {
            CoreError error = CoreError.of(
                            CoreErrorCode.SERVICE_MISSING,
                            CoreErrorSeverity.ERROR,
                            "CommandsProvider not available for /cs debug perms commands"
                    )
                    .withContextEntry("command", "cs debug perms commands");
            CoreErrorUtil.notify(source, error);
            return 0;
        }

        Map<CorePermission, Set<Class<? extends CoreCommand>>> index = commandsProvider.getPermissionIndex();

        source.sendFeedback(
                () -> Messages.t(CoreMessage.PERMDEBUG_MAPPING_HEADER),
                false
        );

        if (index.isEmpty()) {
            source.sendFeedback(
                    () -> Messages.t(CoreMessage.PERMDEBUG_MAPPING_EMPTY),
                    false
            );
            return 1;
        }

        for (Map.Entry<CorePermission, Set<Class<? extends CoreCommand>>> entry : index.entrySet()) {
            CorePermission perm = entry.getKey();
            Set<Class<? extends CoreCommand>> commands = entry.getValue();

            source.sendFeedback(
                    () -> Messages.t(
                            CoreMessage.PERMDEBUG_MAPPING_PERM_HEADER,
                            perm.key(),
                            perm.description(),
                            commands.size()
                    ).copy().formatted(Formatting.AQUA),
                    false
            );

            for (Class<? extends CoreCommand> cmdClass : commands) {
                source.sendFeedback(
                        () -> Messages.t(
                                CoreMessage.PERMDEBUG_MAPPING_COMMAND_LINE,
                                cmdClass.getName()
                        ).copy().formatted(Formatting.GRAY),
                        false
                );
            }
        }

        return 1;
    }

    // ------------------------------------------------------------------------
    // Hilfsmethoden für IslandNodes-Statusanzeige
    // ------------------------------------------------------------------------

    private static @NotNull String buildLine(PrivateIslandWorldNodeStatus status, long now) {
        long ageMs = now - status.lastHeartbeatMillis();
        long ageSec = ageMs / 1000L;

        boolean online = ageMs <= 15_000L;
        String onlineColor = online ? "§a" : "§c";
        String onlineText = online ? "ONLINE" : "OFFLINE";

        double islandLoad = calcIslandLoad(status);
        double playerLoad = calcPlayerLoad(status);

        return String.format(
                "[IslandNodes] Node=%s (Proxy=%s) %s%s Islands=%d/%d (%.1f%%) Players=%d/%d (%.1f%%) LastHB=%ds",
                status.nodeId(),
                status.serverName(),
                onlineColor,
                onlineText,
                status.currentIslands(),
                status.maxIslands(),
                islandLoad,
                status.currentPlayers(),
                status.maxPlayers(),
                playerLoad,
                ageSec
        );
    }

    private static long getAgeSec(PrivateIslandWorldNodeStatus status, long now) {
        long ageMs = now - status.lastHeartbeatMillis();
        return ageMs / 1000L;
    }

    private static boolean isOnline(PrivateIslandWorldNodeStatus status, long now) {
        long ageMs = now - status.lastHeartbeatMillis();
        return ageMs <= 15_000L; // 15 Sekunden Window
    }

    private static String getOnlineColor(PrivateIslandWorldNodeStatus status, long now) {
        return isOnline(status, now) ? "§a" : "§c";
    }

    private static String getOnlineText(PrivateIslandWorldNodeStatus status, long now) {
        return isOnline(status, now) ? "ONLINE" : "OFFLINE";
    }

    private static double calcIslandLoad(PrivateIslandWorldNodeStatus status) {
        return status.maxIslands() > 0
                ? (status.currentIslands() * 100.0 / status.maxIslands())
                : 0.0;
    }

    private static double calcPlayerLoad(PrivateIslandWorldNodeStatus status) {
        return status.maxPlayers() > 0
                ? (status.currentPlayers() * 100.0 / status.maxPlayers())
                : 0.0;
    }

    // ------------------------------------------------------------------------
    // /cs health  → Self-Diagnose des Core-Systems (Redis, Economy, Profile, Services, ...)
    // ------------------------------------------------------------------------

    private int executeHealth(CommandContext<ServerCommandSource> ctx) {
        trackCommandUsage("cs_health", ctx.getSource());

        ServerCommandSource source = ctx.getSource();
        ServerPlayerEntity player = source.getPlayer();

        // Optional: Rate-Limit nur für Spieler, nicht Konsole
        if (player != null) {
            if (!RateLimitUtil.checkPlayerRateLimit(
                    source,
                    player,
                    RateLimitRules.CORE_HEALTH_CHECK,
                    "cs:health"
            )) {
                return 0;
            }
        }

        // FeatureGate
        FeatureToggleService features = ServiceProvider.getService(FeatureToggleService.class);
        if (features != null && !features.isEnabled(FeatureToggleKey.HEALTH_COMMAND_ENABLED)) {
            Messages.send(source, CoreMessage.FEATURE_DISABLED); // oder spezifisch "Health-Command deaktiviert"
            return 0;
        }

        HealthMonitorService monitor = ServiceProvider.getService(HealthMonitorService.class);
        if (monitor == null) {
            CoreError error = CoreError.of(
                            CoreErrorCode.SERVICE_MISSING,
                            CoreErrorSeverity.CRITICAL,
                            "HealthMonitorService not available for /cs health"
                    )
                    .withContextEntry("command", "cs health")
                    .withContextEntry("service", "HealthMonitorService");
            CoreErrorUtil.notify(source, error);
            return 0;
        }

        long start = System.nanoTime();
        List<HealthCheckResult> results = monitor.runAll();
        long durationMs = (System.nanoTime() - start) / 1_000_000L;
        HealthStatus overall = monitor.aggregateStatus(results);

        // --- Metrics ---
        MetricsService metrics = ServiceProvider.getService(MetricsService.class);
        if (metrics != null) {
            metrics.recordTimer("core.healthcheck.duration", durationMs, null);
            metrics.incrementCounter("core.healthcheck.runs", 1L, Map.of(
                    "status", overall.name()
            ));
        }

        // Header
        source.sendFeedback(
                () -> Messages.t(CoreMessage.HEALTH_HEADER, overall.name())
                        .copy()
                        .formatted(mapOverallStatusColor(overall)),
                false
        );

        if (results.isEmpty()) {
            source.sendFeedback(
                    () -> Messages.t(CoreMessage.HEALTH_NONE),
                    false
            );
            return 1;
        }

        // pro Check eine Zeile
        for (HealthCheckResult r : results) {
            Formatting color = mapStatusColor(r.status());

            // Details komprimiert "key=value, key=value"
            String details = r.details().isEmpty()
                    ? "-"
                    : r.details().entrySet().stream()
                    .map(e -> e.getKey() + "=" + e.getValue())
                    .collect(java.util.stream.Collectors.joining(", "));

            source.sendFeedback(
                    () -> Messages.t(
                                    CoreMessage.HEALTH_LINE,
                                    r.name(),
                                    r.status().name(),
                                    r.message(),
                                    details
                            )
                            .copy()
                            .formatted(color),
                    false
            );
        }

        // Footer optional
        source.sendFeedback(
                () -> Messages.t(CoreMessage.HEALTH_FOOTER)
                        .copy()
                        .formatted(Formatting.DARK_GRAY),
                false
        );

        return 1;
    }

    private static Formatting mapStatusColor(HealthStatus status) {
        return switch (status) {
            case UP -> Formatting.GREEN;
            case DEGRADED -> Formatting.GOLD;
            case DOWN -> Formatting.RED;
        };
    }

    private static Formatting mapOverallStatusColor(HealthStatus status) {
        return mapStatusColor(status);
    }

    // ------------------------------------------------------------------------
    // Metrics-Helfer: Command-Usage tracken
    // ------------------------------------------------------------------------

    private void trackCommandUsage(String commandKey, ServerCommandSource source) {
        MetricsService metrics = ServiceProvider.getService(MetricsService.class);
        if (metrics == null) {
            return;
        }

        metrics.incrementCounter("commands.executed", 1L, Map.of(
                "command", commandKey
        ));

        AuditLogService audit = ServiceProvider.getService(AuditLogService.class);
        if (audit != null) {
            audit.log(
                    new FabricAuditContext(source),
                    "command_executed",
                    commandKey
            );
        }
    }

    // ------------------------------------------------------------------------
    // DevTools Hilfsmethoden
    // ------------------------------------------------------------------------

    private int executeDevEnv(CommandContext<ServerCommandSource> ctx) {
        ServerCommandSource source = ctx.getSource();
        trackCommandUsage("cs_dev_env", source);

        if (isDevToolsEnabled(source)) {
            return 0;
        }

        String env = System.getenv().getOrDefault("CORESYSTEM_ENV", "unknown");
        String serverName = System.getenv().getOrDefault("CORESYSTEM_SERVER_NAME", source.getServer().getServerMotd());
        String nodeId = System.getenv().getOrDefault("CORESYSTEM_NODE_ID", "unknown-node");

        source.sendFeedback(
                () -> Messages.t(CoreMessage.DEV_ENV_HEADER)
                        .copy().formatted(Formatting.DARK_GRAY),
                false
        );
        source.sendFeedback(
                () -> Messages.t(CoreMessage.DEV_ENV_LINE, "env", env)
                        .copy().formatted(Formatting.GRAY),
                false
        );
        source.sendFeedback(
                () -> Messages.t(CoreMessage.DEV_ENV_LINE, "serverName", serverName)
                        .copy().formatted(Formatting.GRAY),
                false
        );
        source.sendFeedback(
                () -> Messages.t(CoreMessage.DEV_ENV_LINE, "nodeId", nodeId)
                        .copy().formatted(Formatting.GRAY),
                false
        );

        return 1;
    }

    private int executeDevSimulateJoin(CommandContext<ServerCommandSource> ctx) {
        ServerCommandSource source = ctx.getSource();
        trackCommandUsage("cs_dev_simulate_join", source);

        if (isDevToolsEnabled(source)) {
            return 0;
        }

        String name = StringArgumentType.getString(ctx, "name");
        UUID uuid = java.util.UUID.nameUUIDFromBytes(("dev-" + name).getBytes());

        DevToolsService devTools = ServiceProvider.getService(DevToolsService.class);
        if (devTools == null) {
            CoreError error = CoreError.of(
                            CoreErrorCode.SERVICE_MISSING,
                            CoreErrorSeverity.ERROR,
                            "DevToolsService not available for /cs dev simulate join"
                    )
                    .withContextEntry("command", "cs dev simulate join")
                    .withContextEntry("name", name);
            CoreErrorUtil.notify(source, error);
            return 0;
        }

        devTools.simulatePlayerJoin(uuid, name);

        source.sendFeedback(
                () -> Messages.t(CoreMessage.DEV_SIM_JOIN_OK, name, uuid.toString())
                        .copy().formatted(Formatting.GREEN),
                false
        );

        return 1;
    }

    private int executeDevSimulateQuit(CommandContext<ServerCommandSource> ctx) {
        ServerCommandSource source = ctx.getSource();
        trackCommandUsage("cs_dev_simulate_quit", source);

        if (isDevToolsEnabled(source)) {
            return 0;
        }

        String name = StringArgumentType.getString(ctx, "name");
        UUID uuid = java.util.UUID.nameUUIDFromBytes(("dev-" + name).getBytes());

        DevToolsService devTools = ServiceProvider.getService(DevToolsService.class);
        if (devTools == null) {
            CoreError error = CoreError.of(
                            CoreErrorCode.SERVICE_MISSING,
                            CoreErrorSeverity.ERROR,
                            "DevToolsService not available for /cs dev simulate quit"
                    )
                    .withContextEntry("command", "cs dev simulate quit")
                    .withContextEntry("name", name);
            CoreErrorUtil.notify(source, error);
            return 0;
        }

        devTools.simulatePlayerQuit(uuid, name);

        source.sendFeedback(
                () -> Messages.t(CoreMessage.DEV_SIM_QUIT_OK, name, uuid.toString())
                        .copy().formatted(Formatting.GREEN),
                false
        );

        return 1;
    }

    private boolean isDevToolsEnabled(ServerCommandSource source) {
        ModuleRegistryService modules = ServiceProvider.getService(ModuleRegistryService.class);
        if (modules != null && !modules.isEnabled(CoreModule.DEVTOOLS)) {
            Messages.send(source, CoreMessage.FEATURE_DEVTOOLS_DISABLED); // eigenen Key anlegen
            return true;
        }

        FeatureToggleService features = ServiceProvider.getService(FeatureToggleService.class);
        if (features != null && !features.isEnabled(FeatureToggleKey.DEVTOOLS_ENABLED)) {
            Messages.send(source, CoreMessage.FEATURE_DEVTOOLS_DISABLED);
            return true;
        }

        return false;
    }

    private int executeDevEconomySmoke(CommandContext<ServerCommandSource> ctx) {
        ServerCommandSource source = ctx.getSource();
        trackCommandUsage("cs_dev_economy_smoke", source);

        if (isDevToolsEnabled(source)) {
            return 0;
        }

        ServerPlayerEntity player = source.getPlayer();
        if (player == null) {
            Messages.send(source, CoreMessage.ONLY_PLAYERS);
            return 0;
        }

        DevToolsService devTools = ServiceProvider.getService(DevToolsService.class);
        if (devTools == null) {
            CoreError error = CoreError.of(
                            CoreErrorCode.SERVICE_MISSING,
                            CoreErrorSeverity.ERROR,
                            "DevToolsService not available for /cs dev economy smoke"
                    )
                    .withContextEntry("command", "cs dev economy smoke");
            CoreErrorUtil.notify(source, error);
            return 0;
        }

        String summary = devTools.runEconomySmokeTest(player.getUuid());

        source.sendFeedback(
                () -> Messages.t(CoreMessage.DEV_ECONOMY_SMOKE_HEADER, player.getGameProfile().getName())
                        .copy().formatted(Formatting.DARK_GRAY),
                false
        );

        for (String line : summary.split("\n")) {
            if (line.isEmpty()) continue;
            source.sendFeedback(
                    () -> Messages.literal("  " + line).copy().formatted(Formatting.GRAY),
                    false
            );
        }

        return 1;
    }

    private int executeDevRedisPing(CommandContext<ServerCommandSource> ctx) {
        ServerCommandSource source = ctx.getSource();
        trackCommandUsage("cs_dev_redis_ping", source);

        if (!isDevToolsEnabled(source)) {
            return 0;
        }

        DevToolsService devTools = ServiceProvider.getService(DevToolsService.class);
        if (devTools == null) {
            CoreError error = CoreError.of(
                            CoreErrorCode.SERVICE_MISSING,
                            CoreErrorSeverity.ERROR,
                            "DevToolsService not available for /cs dev redis ping"
                    )
                    .withContextEntry("command", "cs dev redis ping");
            CoreErrorUtil.notify(source, error);
            return 0;
        }

        int samples = 10;
        long avgMs = devTools.measureRedisLatencyMs(samples);

        if (avgMs < 0) {
            source.sendFeedback(
                    () -> Messages.t(CoreMessage.DEV_REDIS_PING_FAILED)
                            .copy().formatted(Formatting.RED),
                    false
            );
            return 0;
        }

        source.sendFeedback(
                () -> Messages.t(CoreMessage.DEV_REDIS_PING_RESULT, samples, avgMs)
                        .copy().formatted(Formatting.GREEN),
                false
        );

        return 1;
    }


    private int executeDevSimulateBurst(CommandContext<ServerCommandSource> ctx) {
        ServerCommandSource source = ctx.getSource();
        trackCommandUsage("cs_dev_simulate_burst", source);

        if (!isDevToolsEnabled(source)) {
            return 0;
        }

        int count = IntegerArgumentType.getInteger(ctx, "count");

        DevToolsService devTools = ServiceProvider.getService(DevToolsService.class);
        if (devTools == null) {
            CoreError error = CoreError.of(
                            CoreErrorCode.SERVICE_MISSING,
                            CoreErrorSeverity.ERROR,
                            "DevToolsService not available for /cs dev simulate burst"
                    )
                    .withContextEntry("command", "cs dev simulate burst")
                    .withContextEntry("count", String.valueOf(count));
            CoreErrorUtil.notify(source, error);
            return 0;
        }

        devTools.simulateJoinQuitBurst(count);

        source.sendFeedback(
                () -> Messages.t(CoreMessage.DEV_SIM_BURST_OK, count)
                        .copy().formatted(Formatting.GOLD),
                false
        );

        return 1;
    }


}
