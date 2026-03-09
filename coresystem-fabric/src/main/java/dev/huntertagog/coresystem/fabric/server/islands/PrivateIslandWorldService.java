package dev.huntertagog.coresystem.fabric.server.islands;

import com.google.common.collect.ImmutableList;
import dev.huntertagog.coresystem.common.error.CoreError;
import dev.huntertagog.coresystem.common.error.CoreErrorCode;
import dev.huntertagog.coresystem.common.error.CoreErrorSeverity;
import dev.huntertagog.coresystem.common.log.Logger;
import dev.huntertagog.coresystem.common.log.LoggerFactory;
import dev.huntertagog.coresystem.common.provider.ServiceProvider;
import dev.huntertagog.coresystem.fabric.common.teleport.TeleportManagerService;
import dev.huntertagog.coresystem.fabric.mixin.MinecraftServerAccess;
import dev.huntertagog.coresystem.fabric.server.world.gen.VoidWorldProgressListener;
import dev.huntertagog.coresystem.fabric.server.world.runtime.RuntimeWorldConfig;
import dev.huntertagog.coresystem.fabric.server.world.runtime.RuntimeWorldHandle;
import dev.huntertagog.coresystem.fabric.server.world.runtime.RuntimeWorldProperties;
import dev.huntertagog.coresystem.platform.task.TaskScheduler;
import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerWorldEvents;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;
import net.minecraft.util.math.random.RandomSequencesState;
import net.minecraft.world.World;
import net.minecraft.world.biome.source.BiomeAccess;
import net.minecraft.world.dimension.DimensionOptions;
import net.minecraft.world.level.storage.LevelStorage;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

public final class PrivateIslandWorldService {

    private static final Logger LOG = LoggerFactory.get("PrivateIslandWorldService");

    public static final String ID = "piws";

    private static PrivateIslandWorldService instance;

    private final Set<ServerWorld> deletionQueue = new ReferenceOpenHashSet<>();
    private final MinecraftServer server;
    private final MinecraftServerAccess serverAccess;

    // optionaler Scheduler-Task für das regelmäßige Abarbeiten der Delete-Queue
    private TaskScheduler.ScheduledTask deletionTask;

    private PrivateIslandWorldService(MinecraftServer server) {
        this.server = server;
        this.serverAccess = (MinecraftServerAccess) server;

        // Scheduler-basiertes Ticking statt globalem ServerTickEvents
        TaskScheduler scheduler = ServiceProvider.getService(TaskScheduler.class);
        if (scheduler != null) {
            // alle Sekunde prüfen (20 Ticks)
            this.deletionTask = scheduler.runSyncRepeating(
                    this::tick,
                    20,
                    20
            );
            LOG.info("Registered deletion queue tick task for PrivateIslandWorldService.");
        } else {
            CoreError error = CoreError.of(
                            CoreErrorCode.SCHEDULER_MISSING,
                            CoreErrorSeverity.WARN,
                            "TaskScheduler not available – PrivateIslandWorldService will not tick deletion queue automatically."
                    )
                    .withContextEntry("service", "PrivateIslandWorldService");

            LOG.warn(error.toLogString());
        }
    }

    public static PrivateIslandWorldService get(MinecraftServer server) {
        if (instance == null || instance.server != server) {
            instance = new PrivateIslandWorldService(server);
        }
        return instance;
    }

    // --------------------------------------------------------
    // Lifecycle / Ticking
    // --------------------------------------------------------

    private void tick() {
        if (this.deletionQueue.isEmpty()) {
            return;
        }

        try {
            // nur Welten entfernen, die vollständig „leer“ sind
            this.deletionQueue.removeIf(this::tickDeleteWorld);
        } catch (Exception e) {
            CoreError error = CoreError.of(
                            CoreErrorCode.ISLAND_WORLD_DELETE_TICK_FAILED,
                            CoreErrorSeverity.ERROR,
                            "Unexpected error while ticking island deletion queue."
                    )
                    .withCause(e)
                    .withContextEntry("queueSize", this.deletionQueue.size());

            LOG.error(error.toLogString(), e);
        }
    }

    public void shutdown() {
        if (deletionTask != null && !deletionTask.isCancelled()) {
            deletionTask.cancel();
            deletionTask = null;
        }
        deletionQueue.clear();
        LOG.info("PrivateIslandWorldService shutdown – deletion task cancelled and queue cleared.");
    }

    // --------------------------------------------------------
    // Public API
    // --------------------------------------------------------

    public CompletableFuture<RuntimeWorldHandle> getOrOpenPersistentWorld(Identifier key, RuntimeWorldConfig config) {
        // supplyAsync mit server als Executor = auf MC-Thread ausführen
        return CompletableFuture.supplyAsync(() -> {
                    try {
                        RegistryKey<World> worldKey = RegistryKey.of(RegistryKeys.WORLD, key);
                        ServerWorld world = this.server.getWorld(worldKey);
                        if (world != null) {
                            this.deletionQueue.remove(world);
                            return world;
                        }

                        return this.openPersistentWorld(key, config);
                    } catch (Exception e) {
                        CoreError error = CoreError.of(
                                        CoreErrorCode.RUNTIME_WORLD_OPEN_FAILED,
                                        CoreErrorSeverity.ERROR,
                                        "Failed to open or create persistent island world."
                                )
                                .withCause(e)
                                .withContextEntry("worldId", key.toString());

                        LOG.error(error.toLogString(), e);
                        throw e;
                    }
                }, this.server)
                .thenApply(world -> new RuntimeWorldHandle(this, world));
    }

    public void enqueueWorldDeletion(ServerWorld world) {
        // wir laufen meistens ohnehin auf dem Server-Thread, aber zur Sicherheit:
        server.execute(() -> this.deletionQueue.add(world));
    }

    public Object getDeletionQueue() {
        return this.deletionQueue;
    }

    // --------------------------------------------------------
    // Intern: Welt-Erstellung / -Löschung
    // --------------------------------------------------------

    ServerWorld openPersistentWorld(Identifier key, RuntimeWorldConfig config) {
        try {
            RegistryKey<World> worldKey = RegistryKey.of(RegistryKeys.WORLD, key);

            DimensionOptions options = new DimensionOptions(
                    config.getDimensionType(this.server),
                    config.getGenerator()
            );

            RuntimeWorldProperties properties = new RuntimeWorldProperties(this.server.getSaveProperties(), config);

            // Wichtig: RandomSequencesState wie Vanilla handhaben (Overworld teilen ist ok)
            ServerWorld overworld = this.server.getOverworld();
            RandomSequencesState randomSequences =
                    overworld != null ? overworld.getRandomSequences() : RandomSequencesState.fromNbt(config.getSeed(), new NbtCompound());

            ServerWorld world = new ServerWorld(
                    this.server,
                    Util.getMainWorkerExecutor(),
                    ((MinecraftServerAccess) this.server).getSession(),
                    properties,
                    worldKey,
                    options,
                    VoidWorldProgressListener.INSTANCE,
                    false,
                    BiomeAccess.hashSeed(config.getSeed()),
                    ImmutableList.of(),
                    false,
                    randomSequences
            );

            return this.addWorld(world);
        } catch (Exception e) {
            CoreError error = CoreError.of(
                            CoreErrorCode.RUNTIME_WORLD_CONSTRUCTION_FAILED,
                            CoreErrorSeverity.ERROR,
                            "Failed to construct island ServerWorld instance."
                    )
                    .withCause(e)
                    .withContextEntry("worldId", key.toString());
            LOG.error(error.toLogString(), e);
            throw e;
        }
    }

    private <T extends ServerWorld> T addWorld(T world) {
        this.serverAccess.getWorlds().put(world.getRegistryKey(), world);
        ServerWorldEvents.LOAD.invoker().onWorldLoad(this.server, world);
        return world;
    }

    private boolean tickDeleteWorld(ServerWorld world) {
        if (this.isWorldUnloaded(world)) {
            this.deleteWorld(world);
            return true;
        } else {
            this.kickPlayers(world);
            return false;
        }
    }

    private void kickPlayers(ServerWorld world) {
        if (world.getPlayers().isEmpty()) {
            return;
        }

        List<ServerPlayerEntity> players = new ArrayList<>(world.getPlayers());
        for (ServerPlayerEntity player : players) {
            TeleportManagerService tp = ServiceProvider.getService(TeleportManagerService.class);
            tp.teleportPlayer(player, "lobby", "delete-kick", "Player kicked from island world for deletion.");
        }
    }

    private boolean isWorldUnloaded(ServerWorld world) {
        return world.getPlayers().isEmpty()
                && world.getChunkManager().getLoadedChunkCount() <= 0;
    }

    private void deleteWorld(ServerWorld world) {
        RegistryKey<World> worldKey = world.getRegistryKey();
        Identifier id = worldKey.getValue();

        if (this.serverAccess.getWorlds().remove(worldKey, world)) {
            ServerWorldEvents.UNLOAD.invoker().onWorldUnload(this.server, world);

            LevelStorage.Session session = this.serverAccess.getSession();
            File worldDirectory = session.getWorldDirectory(worldKey).toFile();
            if (worldDirectory.exists()) {
                try {
                    FileUtils.deleteDirectory(worldDirectory);
                } catch (IOException e) {
                    CoreError error = CoreError.of(
                                    CoreErrorCode.WORLD_DIRECTORY_DELETE_FAILED,
                                    CoreErrorSeverity.WARN,
                                    "Failed to delete world directory, scheduling deleteOnExit."
                            )
                            .withCause(e)
                            .withContextEntry("worldDir", worldDirectory.getAbsolutePath())
                            .withContextEntry("dimension", id.toString());

                    LOG.warn(error.toLogString(), e);

                    try {
                        FileUtils.forceDeleteOnExit(worldDirectory);
                    } catch (IOException ignored) {
                        // hier bewusst nur still, wir haben oben bereits den Error geloggt
                    }
                }
            }

            LOG.info("Deleted island world '{}'.", id);
        }
    }
}
