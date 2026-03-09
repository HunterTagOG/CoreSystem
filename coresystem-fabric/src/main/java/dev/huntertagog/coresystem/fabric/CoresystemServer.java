package dev.huntertagog.coresystem.fabric;

import dev.huntertagog.coresystem.common.cache.IslandOwnerSuggestionCache;
import dev.huntertagog.coresystem.common.chat.RedisChatFilterService;
import dev.huntertagog.coresystem.common.clans.RedisClanService;
import dev.huntertagog.coresystem.common.config.*;
import dev.huntertagog.coresystem.common.dev.DefaultDevToolsService;
import dev.huntertagog.coresystem.common.economy.RedisEconomyService;
import dev.huntertagog.coresystem.common.event.SimpleDomainEventBus;
import dev.huntertagog.coresystem.common.feature.RedisFeatureToggleService;
import dev.huntertagog.coresystem.common.friends.RedisFriendService;
import dev.huntertagog.coresystem.common.health.*;
import dev.huntertagog.coresystem.common.log.Logger;
import dev.huntertagog.coresystem.common.log.LoggerFactory;
import dev.huntertagog.coresystem.common.metrics.MetricsEventListener;
import dev.huntertagog.coresystem.common.metrics.RedisMetricsService;
import dev.huntertagog.coresystem.common.module.DefaultModuleRegistryService;
import dev.huntertagog.coresystem.common.player.RedisPlayerProfileService;
import dev.huntertagog.coresystem.common.player.playerdata.RedisPlayerDataSyncService;
import dev.huntertagog.coresystem.common.provider.ServiceProvider;
import dev.huntertagog.coresystem.common.ratelimit.InMemoryRateLimitService;
import dev.huntertagog.coresystem.common.redis.RedisClient;
import dev.huntertagog.coresystem.common.settings.RedisPlayerSettingsService;
import dev.huntertagog.coresystem.fabric.common.chat.DefaultNamePrefixResolver;
import dev.huntertagog.coresystem.fabric.common.chat.NamePrefixResolver;
import dev.huntertagog.coresystem.fabric.common.friends.gui.FriendGuiServerNet;
import dev.huntertagog.coresystem.fabric.common.message.FabricPlayerMessageService;
import dev.huntertagog.coresystem.fabric.common.net.CoresystemNetworking;
import dev.huntertagog.coresystem.fabric.common.permission.PermissionService;
import dev.huntertagog.coresystem.fabric.common.player.lifecycle.PlayerLifecycleListener;
import dev.huntertagog.coresystem.fabric.common.region.RegionService;
import dev.huntertagog.coresystem.fabric.common.region.visual.RegionOutlineService;
import dev.huntertagog.coresystem.fabric.common.teleport.TeleportManagerService;
import dev.huntertagog.coresystem.fabric.server.bridge.PrivateIslandDeleteResponseBus;
import dev.huntertagog.coresystem.fabric.server.bridge.PrivateIslandPrepareResponseBus;
import dev.huntertagog.coresystem.fabric.server.bridge.VelocityBridgeClient;
import dev.huntertagog.coresystem.fabric.server.command.CommandsProvider;
import dev.huntertagog.coresystem.fabric.server.islands.*;
import dev.huntertagog.coresystem.fabric.server.net.RegionImageNetServer;
import dev.huntertagog.coresystem.fabric.server.net.ServerSwitcherNetworking;
import dev.huntertagog.coresystem.fabric.server.permission.LuckPermsPermissionService;
import dev.huntertagog.coresystem.fabric.server.player.FabricPlayerProfileLifecycle;
import dev.huntertagog.coresystem.fabric.server.player.playerdata.FabricPlayerDataCodec;
import dev.huntertagog.coresystem.fabric.server.protection.WorldProtectionBootstrap;
import dev.huntertagog.coresystem.fabric.server.protection.WorldProtectionService;
import dev.huntertagog.coresystem.fabric.server.region.RegionGuardHooks;
import dev.huntertagog.coresystem.fabric.server.region.RegionPlayerTracker;
import dev.huntertagog.coresystem.fabric.server.region.WorldPersistentRegionService;
import dev.huntertagog.coresystem.fabric.server.region.visual.NetworkRegionOutlineService;
import dev.huntertagog.coresystem.fabric.server.task.CoreTaskScheduler;
import dev.huntertagog.coresystem.fabric.server.teleport.ProxyTeleportManagerService;
import dev.huntertagog.coresystem.fabric.server.teleport.TeleportArrivalListener;
import dev.huntertagog.coresystem.fabric.server.world.structures.StructureSpawnService;
import dev.huntertagog.coresystem.platform.chat.ChatFilterService;
import dev.huntertagog.coresystem.platform.clans.ClanService;
import dev.huntertagog.coresystem.platform.config.ConfigService;
import dev.huntertagog.coresystem.platform.dev.DevToolsService;
import dev.huntertagog.coresystem.platform.economy.DefaultPlayerWalletService;
import dev.huntertagog.coresystem.platform.economy.EconomyService;
import dev.huntertagog.coresystem.platform.economy.PlayerWalletService;
import dev.huntertagog.coresystem.platform.event.DomainEventBus;
import dev.huntertagog.coresystem.platform.feature.FeatureToggleService;
import dev.huntertagog.coresystem.platform.friends.FriendService;
import dev.huntertagog.coresystem.platform.message.PlayerMessageService;
import dev.huntertagog.coresystem.platform.metrics.MetricsService;
import dev.huntertagog.coresystem.platform.module.CoreModule;
import dev.huntertagog.coresystem.platform.module.ModuleRegistryService;
import dev.huntertagog.coresystem.platform.player.PlayerProfileService;
import dev.huntertagog.coresystem.platform.player.playerdata.PlayerDataCodec;
import dev.huntertagog.coresystem.platform.player.playerdata.PlayerDataSyncService;
import dev.huntertagog.coresystem.platform.ratelimit.RateLimitService;
import dev.huntertagog.coresystem.platform.settings.PlayerSettingsService;
import dev.huntertagog.coresystem.platform.task.TaskScheduler;
import net.fabricmc.api.DedicatedServerModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerWorldEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.network.ServerPlayerEntity;

import java.io.File;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Dedicated Server Bootstrap.
 * Redis/LuckPerms/Lifecycle/Workers dürfen hier rein.
 */
public final class CoresystemServer implements DedicatedServerModInitializer {

    private static final Logger LOG = LoggerFactory.get("CoreSystem");

    public static volatile PrivateIslandWorldManager ISLAND_MANAGER;

    private static volatile TaskScheduler TASK_SCHEDULER;
    private static volatile PrivateIslandPrepareResponseBus PREPARE_BUS;
    private static volatile PrivateIslandDeleteResponseBus DELETE_BUS;
    private static volatile VelocityBridgeClient BRIDGE;

    @Override
    public void onInitializeServer() {

        // ---------- Static / early wiring ----------
        File configDir = new File(FabricLoader.getInstance().getConfigDir().toFile(), "coresystem");

        // Player lifecycle (Fabric events)
        FabricPlayerProfileLifecycle.register();
        PlayerLifecycleListener.register();

        // Permissions
        ServiceProvider.registerService(PermissionService.class, new LuckPermsPermissionService());

        // Commands (dispatcher wiring)
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            CommandsProvider provider = ServiceProvider.getService(CommandsProvider.class);
            if (provider == null) {
                LOG.error("CommandsProvider not available during command registration.");
                return;
            }
            provider.registerAll(dispatcher, registryAccess, environment);
        });

        // ---------- Server STARTED ----------
        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            LOG.info("CoreSystem SERVER_STARTED initialisation...");

            if (!configDir.exists()) configDir.mkdirs();
            File mainConfig = new File(configDir, "config.json");

            // Config
            ConfigService configService = new CompositeConfigService(List.of(
                    new FileConfigSource(mainConfig),
                    new SystemPropertyConfigSource(),
                    new EnvConfigSource("")
            ));
            ServiceProvider.registerService(ConfigService.class, configService);

            // Scheduler + EventBus (zuerst!)
            TASK_SCHEDULER = new CoreTaskScheduler(server);
            ServiceProvider.registerService(TaskScheduler.class, TASK_SCHEDULER);
            ServiceProvider.registerService(DomainEventBus.class, new SimpleDomainEventBus(TASK_SCHEDULER));

            // Payloads / Net registrations
            CoresystemNetworking.registerCommonPayloads();
            ServerSwitcherNetworking.init();
            FriendGuiServerNet.register();
            RegionImageNetServer.register();

            // Buses (einmalig)
            PREPARE_BUS = new PrivateIslandPrepareResponseBus();
            DELETE_BUS = new PrivateIslandDeleteResponseBus();
            ServiceProvider.registerService(PrivateIslandPrepareResponseBus.class, PREPARE_BUS);
            ServiceProvider.registerService(PrivateIslandDeleteResponseBus.class, DELETE_BUS);

            String nodeId = System.getenv().getOrDefault("SERVER_ID", "unknown-node");

            // BridgeClient: IMMER (auf allen Servern)
            BRIDGE = new VelocityBridgeClient(server, PREPARE_BUS /* + ggf. DELETE_BUS, wenn du Delete darüber routest */);
            BRIDGE.initInbound();
            ServiceProvider.registerService(VelocityBridgeClient.class, BRIDGE);

            ServiceProvider.registerService(
                    TeleportManagerService.class,
                    new ProxyTeleportManagerService(BRIDGE, nodeId)
            );

            // Worker: NUR auf Island-Servern
            if (PrivateIslandWorldNodeConfig.IS_ISLAND_SERVER) {
                new PrivateIslandWorldPrepareWorker(BRIDGE).initInbound();
                new PrivateIslandWorldDeleteWorker(BRIDGE).initInbound();
            }

            // Metrics
            MetricsService metricsService = new RedisMetricsService();
            ServiceProvider.registerService(MetricsService.class, metricsService);

            // Messaging (platform.message.PlayerMessageService)
            // -> Fabric-Implementierung, die UUID->ServerPlayerEntity mapped
            ServiceProvider.registerService(PlayerMessageService.class, new FabricPlayerMessageService(server));

            // Economy
            EconomyService economyService = new RedisEconomyService();
            ServiceProvider.registerService(EconomyService.class, economyService);
            ServiceProvider.registerService(PlayerWalletService.class, new DefaultPlayerWalletService(economyService));

            // Cache warmup
            IslandOwnerSuggestionCache.refreshAsync(TASK_SCHEDULER);

            // Module/Health/Metrics wiring
            bootstrapModules();
            bootstrapHealth();
            bootstrapMetrics();

            // Structures / FS
            ServiceProvider.registerService(dev.huntertagog.coresystem.fabric.server.world.structures.StructureSpawnService.class,
                    new StructureSpawnService(configDir));

            // PlayerProfile + PlayerDataSync
            ServiceProvider.registerService(PlayerProfileService.class, new RedisPlayerProfileService());
            ServiceProvider.registerService(PlayerDataCodec.class,
                    new FabricPlayerDataCodec(server));

            ServiceProvider.registerService(PlayerDataSyncService.class,
                    new RedisPlayerDataSyncService());

            // DevTools
            ServiceProvider.registerService(DevToolsService.class, new DefaultDevToolsService());

            // RateLimiter
            ServiceProvider.registerService(RateLimitService.class, new InMemoryRateLimitService());

            // Settings
            ServiceProvider.registerService(PlayerSettingsService.class, new RedisPlayerSettingsService());

            // Prefix/Filter
            ServiceProvider.registerService(NamePrefixResolver.class, new DefaultNamePrefixResolver());
            ServiceProvider.registerService(ChatFilterService.class, new RedisChatFilterService());
            ((RedisChatFilterService) Objects.requireNonNull(ServiceProvider.getService(ChatFilterService.class)))
                    .bootstrapDefaultBadWords();

            // Protection + Teleport Listener
            ServiceProvider.registerService(dev.huntertagog.coresystem.fabric.server.protection.WorldProtectionService.class,
                    new WorldProtectionService(server));
            WorldProtectionBootstrap.register(server);
            TeleportArrivalListener.register();

            // Regions
            ServiceProvider.registerService(RegionService.class, new WorldPersistentRegionService(server));

            ServerWorldEvents.LOAD.register((srv, world) -> {
                RegionService rs = ServiceProvider.getService(RegionService.class);
                if (rs instanceof WorldPersistentRegionService wprs) {
                    wprs.loadForWorld(world);
                }
            });

            RegionGuardHooks.register();
            RegionPlayerTracker.register(server);
            ServiceProvider.registerService(RegionOutlineService.class, new NetworkRegionOutlineService());

            // Social
            ServiceProvider.registerService(FriendService.class, new RedisFriendService());
            ServiceProvider.registerService(ClanService.class, new RedisClanService());

            // Islands config + manager
            int maxIslandsPerNode = configService.get(CoreConfigKeys.ISLANDS_MAX_PER_NODE);
            int maxPlayersPerIsland = configService.get(CoreConfigKeys.ISLANDS_MAX_PLAYERS_PER_ISLAND);

            PrivateIslandWorldService.get(server);

            PrivateIslandWorldManager.Config cfg = new PrivateIslandWorldManager.Config(
                    maxIslandsPerNode,
                    maxPlayersPerIsland
            );
            PrivateIslandWorldManager.init(server, cfg);
            ISLAND_MANAGER = PrivateIslandWorldManager.get(server);

            PrivateIslandWorldHeartbeatBootstrap.register(
                    server,
                    PrivateIslandWorldNodeConfig.SERVER_ID,
                    PrivateIslandWorldNodeConfig.SERVER_NAME,
                    cfg.maxIslandsPerNode(),
                    cfg.maxPlayersPerIsland()
            );
            PrivateIslandWorldCounterBootstrap.register();

            if (PrivateIslandWorldNodeConfig.IS_ISLAND_SERVER) {
                LOG.info("Island node online: nodeId='{}' maxIslands={} maxPlayersPerIsland={}",
                        PrivateIslandWorldNodeConfig.SERVER_ID,
                        cfg.maxIslandsPerNode(),
                        cfg.maxPlayersPerIsland()
                );
            } else {
                LOG.info("Non-island node online; island workers not started.");
            }
        });

        // ---------- Server STOPPING ----------
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
            LOG.info("CoreSystem SERVER_STOPPING – shutting down scheduler/redis/buses...");

            try {
                if (PREPARE_BUS != null) PREPARE_BUS.shutdown();
            } catch (Exception ignored) {
            }

            try {
                if (DELETE_BUS != null) DELETE_BUS.shutdown();
            } catch (Exception ignored) {
            }

            try {
                TaskScheduler scheduler = ServiceProvider.getService(TaskScheduler.class);
                if (scheduler != null) scheduler.shutdown();
            } catch (Exception ignored) {
            }

            try {
                RedisClient.get().shutdown();
            } catch (Exception ignored) {
            }
        });

        // ---------- JOIN: auf Island-Servern Welt laden + teleport ----------
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            if (!PrivateIslandWorldNodeConfig.IS_ISLAND_SERVER) return;

            PlayerDataSyncService service = ServiceProvider.getService(PlayerDataSyncService.class);
            service.applyInventorySnapshot(handler.getPlayer().getUuid(), "island_join", true);

            ServerPlayerEntity player = handler.getPlayer();
            UUID ownerId = player.getUuid();

            PrivateIslandWorldManager.get(server)
                    .getOrCreateIslandWorld(ownerId)
                    .thenAccept(world ->
                            server.execute(() ->
                                    PrivateIslandWorldManager.get(server)
                                            .teleportPlayerToIslandWorld(player, world)
                            )
                    );
        });
    }

    // ------------------------------------------------------------------------
    // Health Monitor Bootstrap (server)
    // ------------------------------------------------------------------------
    private void bootstrapHealth() {
        HealthMonitorService monitor = new HealthMonitorService();
        ServiceProvider.registerService(HealthMonitorService.class, monitor);

        monitor.register(new RedisHealthCheck());
        monitor.register(new PlayerProfileHealthCheck());
        monitor.register(new EconomyHealthCheck());
        monitor.register(new ServiceRegistryHealthCheck());

        var results = monitor.runAll();
        var overall = monitor.aggregateStatus(results);

        LOG.info("Core health on startup: {}", overall);
        for (var r : results) {
            LOG.info("Health [{}]: status={} message={} details={}",
                    r.name(), r.status(), r.message(), r.details());
        }
    }

    // ------------------------------------------------------------------------
    // Metrics Event Listener Bootstrap (server)
    // ------------------------------------------------------------------------
    private void bootstrapMetrics() {
        MetricsService metrics = ServiceProvider.getService(MetricsService.class);
        if (metrics == null) {
            LOG.warn("MetricsService not available, skipping MetricsEventListener wiring.");
            return;
        }

        MetricsEventListener listener = new MetricsEventListener(metrics);
        listener.registerOnBus();
    }

    // ------------------------------------------------------------------------
    // Module Registry & Feature-Toggles (server)
    // ------------------------------------------------------------------------
    private void bootstrapModules() {
        DefaultModuleRegistryService moduleRegistry = new DefaultModuleRegistryService();
        ServiceProvider.registerService(ModuleRegistryService.class, moduleRegistry);

        moduleRegistry.registerModule(CoreModule.PLAYER_PROFILE, true, null);
        moduleRegistry.registerModule(CoreModule.ECONOMY, true, null);
        moduleRegistry.registerModule(CoreModule.ISLANDS, true, null);
        moduleRegistry.registerModule(CoreModule.MESSAGING, true, null);
        moduleRegistry.registerModule(CoreModule.METRICS, true, null);
        moduleRegistry.registerModule(CoreModule.AUDIT, true, null);
        moduleRegistry.registerModule(CoreModule.HEALTH, true, null);
        moduleRegistry.registerModule(CoreModule.DEVTOOLS, false, "Disabled by default");

        FeatureToggleService featureService = new RedisFeatureToggleService();
        ServiceProvider.registerService(FeatureToggleService.class, featureService);
    }
}
