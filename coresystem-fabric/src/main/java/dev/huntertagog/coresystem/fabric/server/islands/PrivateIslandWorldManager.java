package dev.huntertagog.coresystem.fabric.server.islands;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import dev.huntertagog.coresystem.common.error.CoreError;
import dev.huntertagog.coresystem.common.error.CoreErrorCode;
import dev.huntertagog.coresystem.common.error.CoreErrorSeverity;
import dev.huntertagog.coresystem.common.islands.PrivateIslandWorldEnv;
import dev.huntertagog.coresystem.common.islands.PrivateIslandWorldNodeSelector;
import dev.huntertagog.coresystem.common.islands.PrivateIslandWorldOwnerRepositoryRedis;
import dev.huntertagog.coresystem.common.log.Logger;
import dev.huntertagog.coresystem.common.log.LoggerFactory;
import dev.huntertagog.coresystem.common.provider.ServiceProvider;
import dev.huntertagog.coresystem.common.ratelimit.RateLimitRules;
import dev.huntertagog.coresystem.common.text.CoreMessage;
import dev.huntertagog.coresystem.common.world.island.IslandGenerationSelector;
import dev.huntertagog.coresystem.fabric.CoresystemCommon;
import dev.huntertagog.coresystem.fabric.common.ratelimit.RateLimitUtil;
import dev.huntertagog.coresystem.fabric.common.teleport.TeleportManagerService;
import dev.huntertagog.coresystem.fabric.common.text.Messages;
import dev.huntertagog.coresystem.fabric.mixin.MinecraftServerAccess;
import dev.huntertagog.coresystem.fabric.server.bridge.PrivateIslandPrepareResponseBus;
import dev.huntertagog.coresystem.fabric.server.bridge.VelocityBridgeClient;
import dev.huntertagog.coresystem.fabric.server.world.gen.OceanIslandChunkGenerator;
import dev.huntertagog.coresystem.fabric.server.world.island.*;
import dev.huntertagog.coresystem.fabric.server.world.runtime.RuntimeWorldConfig;
import dev.huntertagog.coresystem.fabric.server.world.runtime.RuntimeWorldHandle;
import dev.huntertagog.coresystem.platform.islands.PrivateIslandNodeRepository;
import dev.huntertagog.coresystem.platform.islands.PrivateIslandWorldNodeStatus;
import dev.huntertagog.coresystem.platform.task.TaskScheduler;
import lombok.Getter;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.Difficulty;
import net.minecraft.world.GameRules;
import net.minecraft.world.World;
import net.minecraft.world.dimension.DimensionTypes;
import net.minecraft.world.gen.chunk.ChunkGenerator;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.time.Duration;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;

import static dev.huntertagog.coresystem.fabric.server.islands.PrivateIslandWorldNodeConfig.IS_ISLAND_SERVER;


public final class PrivateIslandWorldManager {

    private static final Logger LOG = LoggerFactory.get("PrivateIslands");

    private static volatile PrivateIslandWorldManager instance;

    private final MinecraftServer server;
    @Getter
    private final Config config;

    private final Cache<@NotNull UUID, RegistryKey<World>> islandKeyCache;
    private final Cache<@NotNull UUID, ServerWorld> islandWorldCache;

    private final AtomicInteger islandWorldCount = new AtomicInteger(0);

    // ------------------------------------------------------------------------
    // Konstruktion / Lifecycle
    // ------------------------------------------------------------------------

    private PrivateIslandWorldManager(MinecraftServer server, Config config) {
        this.server = Objects.requireNonNull(server, "server");
        this.config = Objects.requireNonNull(config, "config");

        this.islandKeyCache = Caffeine.newBuilder()
                .maximumSize(10_000)
                .expireAfterAccess(Duration.ofMinutes(30))
                .build();

        this.islandWorldCache = Caffeine.newBuilder()
                .maximumSize(10_000)
                .expireAfterAccess(Duration.ofMinutes(15))
                .build();
    }

    private PrivateIslandNodeRepository nodeRepo() {
        PrivateIslandNodeRepository repo = ServiceProvider.getService(PrivateIslandNodeRepository.class);
        if (repo == null) {
            throw new IllegalStateException("PrivateIslandNodeRepository missing (ServiceProvider)");
        }
        return repo;
    }

    public static void init(MinecraftServer server, Config config) {
        PrivateIslandWorldManager manager = new PrivateIslandWorldManager(server, config);
        manager.initializeIslandWorldCount();
        instance = manager;
        LOG.info("PrivateIslandWorldManager initialisiert mit maxIslandsPerNode={} maxPlayersPerIsland={}",
                config.maxIslandsPerNode(), config.maxPlayersPerIsland());
    }

    public static PrivateIslandWorldManager get(MinecraftServer server) {
        if (instance == null) {
            throw new IllegalStateException("PrivateIslandWorldManager not initialized. Call init(server, config) first.");
        }
        if (instance.server != server) {
            throw new IllegalStateException("PrivateIslandWorldManager accessed with different MinecraftServer instance.");
        }
        return instance;
    }

    @Nullable
    public static PrivateIslandWorldManager getInstance() {
        return instance;
    }

    public Object getDeletionQueue() {
        return PrivateIslandWorldService.get(this.server).getDeletionQueue();
    }

    public void teleportPlayerToIslandWorld(ServerPlayerEntity player, ServerWorld world) {
        BlockPos pos = world.getSpawnPos();
        player.teleport(world, pos.getX(), pos.getY(), pos.getZ(), player.getYaw(), player.getPitch());
    }

    public record Config(
            int maxIslandsPerNode,
            int maxPlayersPerIsland
    ) {
    }

    // ------------------------------------------------------------------------
    // Live-Counter-API
    // ------------------------------------------------------------------------

    void incrementIslandWorldCount() {
        int value = islandWorldCount.incrementAndGet();
        LOG.debug("Island world count incremented: {}", value);
    }

    void decrementIslandWorldCount() {
        int value = islandWorldCount.decrementAndGet();
        if (value < 0) {
            islandWorldCount.set(0);
            LOG.warn("Island world count went below 0, resetting to 0.");
        } else {
            LOG.debug("Island world count decremented: {}", value);
        }
    }

    public int countIslandWorlds() {
        return islandWorldCount.get();
    }

    private void initializeIslandWorldCount() {
        int count = 0;
        for (ServerWorld world : server.getWorlds()) {
            if (isIslandWorld(world)) {
                count++;
            }
        }
        islandWorldCount.set(count);
        LOG.info("Initialized island world count to {} on startup.", count);
    }

    // ------------------------------------------------------------------------
    // World-Resolution / Lifecycle
    // ------------------------------------------------------------------------

    public CompletableFuture<ServerWorld> getOrCreateIslandWorld(@NotNull UUID ownerId) {
        if (!IS_ISLAND_SERVER) {
            CoreError error = CoreError.of(
                            CoreErrorCode.PRIVATE_ISLAND_CALL_ON_NON_ISLAND_NODE,
                            CoreErrorSeverity.ERROR,
                            "[PrivateIslands] getOrCreateIslandWorld() called on non-island server."
                    )
                    .withContextEntry("ownerId", ownerId)
                    .withContextEntry("serverId", server.getServerMotd());

            error.log();
            return CompletableFuture.failedFuture(
                    new IllegalStateException(error.toLogString())
            );
        }

        RegistryKey<World> worldKey = islandWorldKey(ownerId);

        ServerWorld cached = islandWorldCache.getIfPresent(ownerId);
        if (cached != null && cached.getServer() == server) {
            LOG.debug("[PrivateIslands] World already exists for owner {} (cache-hit).", ownerId);
            return CompletableFuture.completedFuture(cached);
        }

        ServerWorld existing = server.getWorld(worldKey);
        if (existing != null) {
            islandWorldCache.put(ownerId, existing);
            LOG.debug("[PrivateIslands] World already exists for owner {} (server world list).", ownerId);
            return CompletableFuture.completedFuture(existing);
        }

        if (countIslandWorlds() >= config.maxIslandsPerNode()) {
            CoreError error = CoreError.of(
                            CoreErrorCode.PRIVATE_ISLAND_MAX_WORLDS_REACHED,
                            CoreErrorSeverity.WARN,
                            "[PrivateIslands] Max worlds on this node reached."
                    )
                    .withContextEntry("maxIslandsPerNode", config.maxIslandsPerNode())
                    .withContextEntry("currentIslands", countIslandWorlds());

            error.log();
            return CompletableFuture.failedFuture(
                    new IllegalStateException(error.toLogString())
            );
        }
        boolean existsOnDisk = worldDirExists(worldKey);

        return createIslandWorldAsync(worldKey, ownerId, existsOnDisk)
                .thenApply(world -> {
                    LOG.debug("[PrivateIslands] Created island world for owner {}.", ownerId);
                    islandWorldCache.put(ownerId, world);
                    return world;
                });
    }

    public boolean isIslandWorld(ServerWorld world) {
        Identifier id = world.getRegistryKey().getValue();
        return id.getNamespace().equals(CoresystemCommon.MOD_ID)
                && id.getPath().startsWith("island/");
    }

    public void deleteIslandWorld(ServerWorld world, UUID ownerId) {
        if (world != null) {
            world.getPlayers().forEach(player -> {
                sendToFallbackServer("lobby", player);
                player.sendMessage(
                        Messages.t(CoreMessage.ISLAND_DELETE_KICK),
                        true
                );
            });

            PrivateIslandWorldService.get(server).enqueueWorldDeletion(world);
            islandKeyCache.invalidate(ownerId);
            islandWorldCache.invalidate(ownerId);
            decrementIslandWorldCount();
            var ownerRepo = PrivateIslandWorldOwnerRepositoryRedis.get();
            ownerRepo.unassignOwner(ownerId);
            PrivateIslandInitState init = world.getPersistentStateManager()
                    .getOrCreate(PrivateIslandInitState.TYPE, PrivateIslandInitState.KEY);
            init.setUninitialized();
            LOG.info("Enqueued deletion of island world {} for owner {}.",
                    world.getRegistryKey().getValue(), ownerId);
        }
    }

    public static void sendToFallbackServer(String targetServerName, ServerPlayerEntity player) {
        TeleportManagerService tp = ServiceProvider.getService(TeleportManagerService.class);
        tp.teleportPlayer(player, targetServerName, "network-global", "island-delete-fallback");
    }

    public RegistryKey<World> islandWorldKey(UUID ownerId) {
        RegistryKey<World> cached = islandKeyCache.getIfPresent(ownerId);
        if (cached != null) {
            return cached;
        }

        Identifier id = Identifier.of(
                CoresystemCommon.MOD_ID,
                "island/" + ownerId.toString().toLowerCase()
        );
        RegistryKey<World> key = RegistryKey.of(RegistryKeys.WORLD, id);
        islandKeyCache.put(ownerId, key);
        return key;
    }

    // ------------------------------------------------------------------------
    // Welt-Erstellung (async)
    // ------------------------------------------------------------------------

    private CompletableFuture<ServerWorld> createIslandWorldAsync(RegistryKey<World> worldKey, UUID ownerId, boolean existsOnDisk) {
        ServerWorld overworld = server.getOverworld();


        if (overworld == null) {
            CoreError error = CoreError.of(
                            CoreErrorCode.PRIVATE_ISLAND_OVERWORLD_MISSING,
                            CoreErrorSeverity.ERROR,
                            "[PrivateIslands] Overworld not available – cannot create island world."
                    )
                    .withContextEntry("ownerId", ownerId)
                    .withContextEntry("worldKey", worldKey.getValue().toString());

            error.log();
            return CompletableFuture.failedFuture(
                    new IllegalStateException(error.toLogString())
            );
        }

        Identifier persistentId = worldKey.getValue();
        long islandSeed = ThreadLocalRandom.current().nextLong();

        int islandRadiusChunks = PrivateIslandWorldEnv.ISLAND_RADIUS;
        int oceanRadiusChunks = PrivateIslandWorldEnv.OCEAN_RADIUS;
        int islandRadiusBlocks = islandRadiusChunks * 16;

        int seaLevel = overworld.getSeaLevel();
        int baseHeight = seaLevel - 2;

        LOG.info("Creating island world '{}' for owner '{}' (island={} chunks, ocean={} chunks, seed={})",
                persistentId,
                ownerId,
                islandRadiusChunks,
                oceanRadiusChunks,
                islandSeed
        );

        var preset = IslandGenerationSelector.selectPreset(islandSeed, ownerId);

        final IslandHeightmap heightmap;

        switch (preset) {
            case REALISTIC_SINGLE -> heightmap = IslandHeightmap.createRealisticSingle(
                    islandRadiusBlocks,
                    baseHeight,
                    seaLevel,
                    islandSeed ^ ownerId.getMostSignificantBits()
            );
            case REALISTIC_MULTI_PEAKS -> heightmap = IslandHeightmap.createRealisticMultiPeak(
                    islandRadiusBlocks,
                    baseHeight,
                    seaLevel,
                    islandSeed ^ ownerId.getLeastSignificantBits()
            );
            case CLUSTER -> {
                int clusterCount = 3 + Math.floorMod(islandSeed, 3);
                heightmap = IslandHeightmap.createCluster(
                        islandRadiusBlocks,
                        baseHeight,
                        seaLevel,
                        islandSeed ^ 0x1337L,
                        clusterCount
                );
            }
            case CLUSTER_LAGOONS -> {
                int clusterCount = 3 + Math.floorMod(islandSeed >>> 1, 3);
                heightmap = IslandHeightmap.createClusterWithLagoons(
                        islandRadiusBlocks,
                        baseHeight,
                        seaLevel,
                        islandSeed ^ 0xBADC0DEL,
                        clusterCount
                );
            }
            default -> throw new IllegalStateException("Unexpected preset: " + preset);
        }

        ChunkGenerator vanilla = overworld.getChunkManager().getChunkGenerator();

        ChunkGenerator generator = new OceanIslandChunkGenerator(
                vanilla,
                overworld,
                heightmap
        );

        RuntimeWorldConfig cfg = new RuntimeWorldConfig()
                .setDimensionType(DimensionTypes.OVERWORLD)
                .setGenerator(generator)
                .setDifficulty(Difficulty.NORMAL)
                .setGameRule(GameRules.PLAYERS_SLEEPING_PERCENTAGE, 20)
                .setSeed(islandSeed);

        return loadPersistentWorldAsync(server, persistentId, cfg)
                .thenCompose(world -> {
                    CompletableFuture<ServerWorld> future = new CompletableFuture<>();

                    server.execute(() -> {
                        try {
                            PrivateIslandInitState init = world.getPersistentStateManager()
                                    .getOrCreate(PrivateIslandInitState.TYPE, PrivateIslandInitState.KEY);

                            // Wenn Disk sagt “existiert”, aber init fehlt: NICHT dekorieren, um Builds zu schützen
                            if (existsOnDisk && init.isInitialized()) {
                                LOG.warn("Island {} exists on disk but init flag missing. Skipping decoration to avoid overwrites.", persistentId);
                                future.complete(world);
                                return;
                            }

                            // ---- FIRST CREATE ONLY ----
                            configureIslandWorldBorder(world, islandRadiusChunks, oceanRadiusChunks);

                            int rChunks = islandRadiusChunks + 2;
                            for (int cx = -rChunks; cx <= rChunks; cx++) {
                                for (int cz = -rChunks; cz <= rChunks; cz++) {
                                    world.getChunk(cx, cz);
                                }
                            }

                            IslandStyle style = IslandStyleSelector.selectFor(world, ownerId, islandSeed);
                            IslandHeightmap.applyGroundProfile(world, heightmap, style, seaLevel);

                            IslandDecorator.decorateOceanIsland(world, heightmap, islandRadiusBlocks, seaLevel, style, islandSeed);

                            TeleportSafeZoneUtil.createTeleportSafeZone(world, heightmap, style, seaLevel);

                            world.getChunk(0, 0);

                            PrivateIslandWorldSchematicLoader.placeRandomStarterIsland(world);

                            init.markInitialized();

                            LOG.info("Island {} created & initialized (decoration applied).", persistentId);
                            future.complete(world);

                        } catch (Exception e) {
                            CoreError error = CoreError.of(
                                            CoreErrorCode.PRIVATE_ISLAND_WORLD_CREATION_FAILED,
                                            CoreErrorSeverity.ERROR,
                                            "[PrivateIslands] Failed to create/decorate island world."
                                    )
                                    .withContextEntry("ownerId", ownerId)
                                    .withContextEntry("worldKey", worldKey.getValue().toString())
                                    .withCause(e);

                            error.log();
                            future.completeExceptionally(
                                    new IllegalStateException(error.toLogString(), e)
                            );
                        }
                    });

                    return future;
                });
    }

    private void configureIslandWorldBorder(ServerWorld world,
                                            int islandRadiusChunks,
                                            int oceanRadiusChunks) {
        int borderRadiusBlocks = PrivateIslandWorldEnv.BORDER_RADIUS * 16;

        LOG.info("Configuring island world border radius {} blocks (island={} chunks, ocean={} chunks) for world '{}'",
                borderRadiusBlocks,
                islandRadiusChunks,
                oceanRadiusChunks,
                world.getRegistryKey().getValue());

        try {
            server.getCommandManager().executeWithPrefix(
                    server.getCommandSource().withLevel(4),
                    "chunky border add " + world.getRegistryKey().getValue() + " circle 0.5 0.5 " + borderRadiusBlocks * 2
            );

            LOG.info("Configured border for '{}' -> radius {} blocks (island={} chunks, ocean={} chunks)",
                    world.getRegistryKey().getValue(),
                    borderRadiusBlocks,
                    islandRadiusChunks,
                    oceanRadiusChunks);
        } catch (Exception e) {
            CoreError error = CoreError.of(
                            CoreErrorCode.PRIVATE_ISLAND_BORDER_CONFIG_FAILED,
                            CoreErrorSeverity.WARN,
                            "Failed to configure island world border via Chunky command."
                    )
                    .withContextEntry("worldKey", world.getRegistryKey().getValue().toString())
                    .withContextEntry("borderRadiusBlocks", borderRadiusBlocks)
                    .withContextEntry("islandRadiusChunks", islandRadiusChunks)
                    .withContextEntry("oceanRadiusChunks", oceanRadiusChunks)
                    .withCause(e);

            error.log();
        }
    }

    private CompletableFuture<ServerWorld> loadPersistentWorldAsync(MinecraftServer server,
                                                                    Identifier id,
                                                                    RuntimeWorldConfig config) {
        PrivateIslandWorldService piws = PrivateIslandWorldService.get(server);
        return piws.getOrOpenPersistentWorld(id, config)
                .thenApply(RuntimeWorldHandle::asWorld);
    }

    private boolean worldDirExists(RegistryKey<World> worldKey) {
        var session = ((MinecraftServerAccess) server).getSession();
        File dir = session.getWorldDirectory(worldKey).toFile();
        return dir.exists() && dir.isDirectory();
    }

    // ------------------------------------------------------------------------
    // Command-Entry / Node-Routing
    // ------------------------------------------------------------------------

    public void handleIslandCommand(ServerPlayerEntity player) {
        var src = player.getCommandSource();
        if (!RateLimitUtil.checkPlayerRateLimit(src, player, RateLimitRules.ISLAND_TELEPORT, "island:command")) return;

        TaskScheduler scheduler = ServiceProvider.getService(TaskScheduler.class);
        if (scheduler == null) {
            player.sendMessage(Messages.t(CoreMessage.ISLAND_NODE_INTERNAL_ERROR), false);
            return;
        }

        UUID ownerId = player.getUuid();
        player.sendMessage(Messages.t(CoreMessage.ISLAND_PREPARE_START), false);

        scheduler.runAsync(() -> {
            try {
                // 1) Node wählen
                String nodeId = selectBestIslandNodeId(ownerId);
                if (nodeId == null) {
                    scheduler.runSync(() -> player.sendMessage(Messages.t(CoreMessage.ISLAND_NO_NODE_CAPACITY), false));
                    return;
                }

                // 2) Owner->Node Assignment (Redis)
                PrivateIslandWorldOwnerRepositoryRedis.get().assignOwnerToNode(ownerId, nodeId);

                // 3) Ziel-Servername aus NodeRepo
                var status = nodeRepo().getStatus(nodeId);
                if (status == null) {
                    scheduler.runSync(() -> player.sendMessage(Messages.t(CoreMessage.ISLAND_NODE_INTERNAL_ERROR), false));
                    return;
                }

                String targetServerName = status.serverName();

                // 4) Prepare-Request via Velocity Bridge (Request/Response-Flow)
                var bridge = ServiceProvider.getService(VelocityBridgeClient.class);
                var bus = ServiceProvider.getService(PrivateIslandPrepareResponseBus.class);

                if (bridge == null || bus == null) {
                    scheduler.runSync(() -> player.sendMessage(Messages.t(CoreMessage.ISLAND_NODE_INTERNAL_ERROR), false));
                    return;
                }

                var prepareClient = new PrivateIslandWorldPrepareClient(bridge, bus);

                var fut = prepareClient.requestPrepare(ownerId, nodeId);

                // 5) Response auswerten (wieder auf Main Thread)
                fut.whenComplete((resp, err) -> scheduler.runSync(() -> {
                    if (!player.networkHandler.isConnectionOpen()) return;

                    if (err != null) {
                        player.sendMessage(Messages.t(CoreMessage.ISLAND_NODE_INTERNAL_ERROR), false);
                        return;
                    }

                    if (resp == null || !resp.ok()) {
                        // resp.error() kann null sein -> defensive
                        player.sendMessage(Messages.t(CoreMessage.ISLAND_NODE_INTERNAL_ERROR), false);
                        return;
                    }

                    // 6) Teleport / Switch auf Zielserver (Proxy)
                    TeleportManagerService tp = ServiceProvider.getService(TeleportManagerService.class);
                    tp.teleportPlayer(player, targetServerName, "island-world-join", "PrivateIslandWorldManager");
                }));

            } catch (Exception e) {
                scheduler.runSync(() -> player.sendMessage(Messages.t(CoreMessage.ISLAND_NODE_INTERNAL_ERROR), false));
            }
        });
    }

    @Nullable
    private String selectBestIslandNodeId(UUID ownerId) {
        var all = nodeRepo().getAllStatuses();
        long heartbeatIntervalMs = 5_000; // oder aus Config ableiten
        return PrivateIslandWorldNodeSelector
                .selectBestIslandNode(all, heartbeatIntervalMs, ownerId)
                .map(PrivateIslandWorldNodeStatus::nodeId)
                .orElse(null);
    }
}
