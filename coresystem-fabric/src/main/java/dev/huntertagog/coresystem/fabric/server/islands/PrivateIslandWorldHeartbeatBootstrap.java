package dev.huntertagog.coresystem.fabric.server.islands;

import dev.huntertagog.coresystem.common.error.CoreError;
import dev.huntertagog.coresystem.common.error.CoreErrorCode;
import dev.huntertagog.coresystem.common.error.CoreErrorSeverity;
import dev.huntertagog.coresystem.common.islands.RedisPrivateIslandNodeRepository;
import dev.huntertagog.coresystem.common.log.Logger;
import dev.huntertagog.coresystem.common.log.LoggerFactory;
import dev.huntertagog.coresystem.common.provider.ServiceProvider;
import dev.huntertagog.coresystem.platform.islands.PrivateIslandWorldNodeStatus;
import dev.huntertagog.coresystem.platform.task.TaskScheduler;
import net.minecraft.server.MinecraftServer;

public final class PrivateIslandWorldHeartbeatBootstrap {

    private static final Logger LOG = LoggerFactory.get("IslandNodeHeartbeat");
    private static final int INTERVAL_TICKS = 20 * 5; // 5 Sekunden bei 20 TPS

    // Optional: Handle, falls wir den Heartbeat später gezielt stoppen wollen
    private static TaskScheduler.ScheduledTask HEARTBEAT_TASK;

    private PrivateIslandWorldHeartbeatBootstrap() {
    }

    public static void register(MinecraftServer server,
                                String nodeId,
                                String serverName,
                                int maxIslands,
                                int maxPlayers) {

        if (!PrivateIslandWorldNodeConfig.IS_ISLAND_SERVER) {
            LOG.info("Skipping IslandNode heartbeat – this node is not an island server ({}).",
                    PrivateIslandWorldNodeConfig.SERVER_ID);
            return;
        }

        TaskScheduler scheduler = ServiceProvider.getService(TaskScheduler.class);
        if (scheduler == null) {
            CoreError error = CoreError.of(
                            CoreErrorCode.ISLAND_NODE_HEARTBEAT_INIT_FAILED,
                            CoreErrorSeverity.ERROR,
                            "Cannot register IslandNode heartbeat: TaskScheduler not available."
                    )
                    .withContextEntry("nodeId", nodeId)
                    .withContextEntry("serverName", serverName);

            LOG.error(error.toLogString());
            return;
        }

        if (HEARTBEAT_TASK != null && !HEARTBEAT_TASK.isCancelled()) {
            CoreError error = CoreError.of(
                            CoreErrorCode.ISLAND_NODE_HEARTBEAT_ALREADY_REGISTERED,
                            CoreErrorSeverity.WARN,
                            "IslandNode heartbeat is already registered, skipping duplicate registration."
                    )
                    .withContextEntry("nodeId", nodeId)
                    .withContextEntry("serverName", serverName);

            LOG.warn(error.toLogString());
            return;
        }

        LOG.info("Registering IslandNode heartbeat for node '{}' (serverName='{}').", nodeId, serverName);

        HEARTBEAT_TASK = scheduler.runSyncRepeating(
                () -> {
                    try {
                        PrivateIslandWorldManager manager = PrivateIslandWorldManager.get(server);

                        // 🔹 NUR lokale, billige Reads im Mainthread
                        int currentIslands = manager.countIslandWorlds();
                        int currentPlayers = server.getPlayerManager().getPlayerList().size();
                        long now = System.currentTimeMillis();

                        // 🔹 Async Redis-Write
                        scheduler.runAsync(() -> {
                            try {
                                PrivateIslandWorldNodeStatus status = new PrivateIslandWorldNodeStatus(
                                        nodeId,
                                        serverName,
                                        true,
                                        currentIslands,
                                        maxIslands,
                                        currentPlayers,
                                        maxPlayers,
                                        now
                                );

                                RedisPrivateIslandNodeRepository redisRepo = ServiceProvider.getService(RedisPrivateIslandNodeRepository.class);
                                redisRepo.saveStatus(status);

                            } catch (Exception e) {
                                CoreError.of(
                                                CoreErrorCode.ISLAND_NODE_HEARTBEAT_SEND_FAILED,
                                                CoreErrorSeverity.ERROR,
                                                "Async error while sending IslandNode heartbeat."
                                        )
                                        .withCause(e)
                                        .withContextEntry("nodeId", nodeId)
                                        .log();
                            }
                        });

                    } catch (Exception e) {
                        CoreError.of(
                                        CoreErrorCode.ISLAND_NODE_HEARTBEAT_SEND_FAILED,
                                        CoreErrorSeverity.ERROR,
                                        "Error while preparing IslandNode heartbeat."
                                )
                                .withCause(e)
                                .withContextEntry("nodeId", nodeId)
                                .log();
                    }
                },
                INTERVAL_TICKS,
                INTERVAL_TICKS
        );
    }

    /**
     * Optionaler Shutdown-Hook, falls du den Heartbeat unabhängig vom globalen Scheduler stoppen willst.
     */
    public static void shutdown() {
        if (HEARTBEAT_TASK != null && !HEARTBEAT_TASK.isCancelled()) {
            LOG.info("Cancelling IslandNode heartbeat task.");
            HEARTBEAT_TASK.cancel();
        }
        HEARTBEAT_TASK = null;
    }
}
