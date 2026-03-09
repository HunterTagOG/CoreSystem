package dev.huntertagog.coresystem.fabric.server.command.impl;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import dev.huntertagog.coresystem.common.cache.IslandOwnerSuggestionCache;
import dev.huntertagog.coresystem.common.command.BaseCommand;
import dev.huntertagog.coresystem.common.error.CoreError;
import dev.huntertagog.coresystem.common.error.CoreErrorCode;
import dev.huntertagog.coresystem.common.error.CoreErrorSeverity;
import dev.huntertagog.coresystem.common.islands.PrivateIslandWorldDeleteClient;
import dev.huntertagog.coresystem.common.islands.PrivateIslandWorldOwnerRepositoryRedis;
import dev.huntertagog.coresystem.common.permission.CorePermission;
import dev.huntertagog.coresystem.common.permission.PermissionKeys;
import dev.huntertagog.coresystem.common.text.CoreMessage;
import dev.huntertagog.coresystem.fabric.common.error.CoreErrorUtil;
import dev.huntertagog.coresystem.fabric.common.text.Messages;
import dev.huntertagog.coresystem.fabric.mixin.MinecraftServerAccess;
import dev.huntertagog.coresystem.fabric.server.command.CommandSuggestions;
import dev.huntertagog.coresystem.fabric.server.command.CommandUtil;
import dev.huntertagog.coresystem.fabric.server.command.CoreCommand;
import dev.huntertagog.coresystem.fabric.server.islands.PrivateIslandWorldManager;
import dev.huntertagog.coresystem.fabric.server.islands.PrivateIslandWorldNodeConfig;
import dev.huntertagog.coresystem.platform.command.CommandMeta;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.util.Map;
import java.util.UUID;

import static net.minecraft.server.command.CommandManager.literal;

@CommandMeta(
        value = "coresystem_island",
        permission = PermissionKeys.ISLAND_CREATE, // „Default“-Recht fürs Command, rein für Doku/Index
        enabled = true
)
public final class PrivateIslandCommand extends BaseCommand implements CoreCommand {

    public PrivateIslandCommand() {
    }

    @Override
    public void register(CommandDispatcher<ServerCommandSource> dispatcher,
                         CommandRegistryAccess registryAccess,
                         CommandManager.RegistrationEnvironment environment) {

        dispatcher.register(
                // Root „island“ ohne eigenes requires → Zugriff wird über Subcommands geregelt
                literal("island")
                        .executes(ctx -> {
                            ServerCommandSource source = ctx.getSource();
                            MinecraftServer server = source.getServer();
                            ServerPlayerEntity player = source.getPlayer();

                            if (player == null) {
                                Messages.send(source, CoreMessage.ONLY_PLAYERS);
                                return 0;
                            }

                            logger("PrivateIslandCommand").info("Starting island world creation for identifier '{}'.",
                                    player.getName());

                            final PrivateIslandWorldManager manager;
                            try {
                                manager = PrivateIslandWorldManager.get(server);
                            } catch (Exception ex) {
                                CoreError error = new CoreError(
                                        CoreErrorCode.SERVICE_MISSING,
                                        CoreErrorSeverity.CRITICAL,
                                        "PrivateIslandWorldManager not available for /island create",
                                        ex,
                                        Map.of(
                                                "command", "island create",
                                                "player", player.getGameProfile().getName()
                                        )
                                );
                                CoreErrorUtil.notify(source, error);
                                return 0;
                            }

                            if (manager == null) {
                                CoreError error = CoreError.of(
                                                CoreErrorCode.SERVICE_MISSING,
                                                CoreErrorSeverity.CRITICAL,
                                                "PrivateIslandWorldManager returned null for /island create"
                                        )
                                        .withContextEntry("command", "island create")
                                        .withContextEntry("player", player.getGameProfile().getName());
                                CoreErrorUtil.notify(source, error);
                                return 0;
                            }

                            try {
                                manager.handleIslandCommand(player);
                            } catch (Exception ex) {
                                CoreError error = new CoreError(
                                        CoreErrorCode.ISLAND_CREATE_FAILED,
                                        CoreErrorSeverity.ERROR,
                                        "Failed to handle /island create",
                                        ex,
                                        Map.of(
                                                "player", player.getGameProfile().getName(),
                                                "uuid", player.getUuid().toString()
                                        )
                                );
                                CoreErrorUtil.notify(source, error);
                                return 0;
                            }

                            return 1;
                        })
                        // ----------------------------------------------------------------
                        // /island delete           → löscht eigene Island
                        // /island delete <target>  → löscht Island eines Zielspielers (Admin)
                        // Tab-Completion: Spielername über EntityArgumentType
                        // ----------------------------------------------------------------
                        .then(
                                CommandUtil.literalWithPermission(this, "delete", CorePermission.ISLAND_ADMIN)
                                        // /island delete
                                        .executes(ctx -> {
                                            ServerCommandSource source = ctx.getSource();
                                            MinecraftServer server = source.getServer();
                                            ServerPlayerEntity player = source.getPlayer();

                                            if (player == null) {
                                                Messages.send(source, CoreMessage.ONLY_PLAYERS);
                                                return 0;
                                            }

                                            UUID ownerId = player.getUuid();

                                            try {
                                                handleDeleteIslandCommand(server, ownerId, source);
                                            } catch (Exception ex) {
                                                CoreError error = new CoreError(
                                                        CoreErrorCode.ISLAND_DELETE_FAILED,
                                                        CoreErrorSeverity.ERROR,
                                                        "Failed to delete island for own player",
                                                        ex,
                                                        Map.of(
                                                                "command", "island delete",
                                                                "ownerId", ownerId.toString()
                                                        )
                                                );
                                                CoreErrorUtil.notify(source, error);
                                                return 0;
                                            }

                                            Messages.send(source, CoreMessage.ISLAND_DELETE_SUCCESS, ownerId.toString());
                                            return 1;
                                        })
                                        // /island delete <target>
                                        .then(
                                                CommandManager.argument("target", EntityArgumentType.player())
                                                        // Player-Tab-Completion kommt hier automatisch von Minecraft
                                                        .executes(ctx -> {
                                                            ServerCommandSource source = ctx.getSource();
                                                            MinecraftServer server = source.getServer();
                                                            ServerPlayerEntity target =
                                                                    EntityArgumentType.getPlayer(ctx, "target");

                                                            UUID ownerId = target.getUuid();

                                                            try {
                                                                handleDeleteIslandCommand(server, ownerId, source);
                                                            } catch (Exception ex) {
                                                                CoreError error = new CoreError(
                                                                        CoreErrorCode.ISLAND_DELETE_FAILED,
                                                                        CoreErrorSeverity.ERROR,
                                                                        "Failed to delete island for target player",
                                                                        ex,
                                                                        Map.of(
                                                                                "command", "island delete <target>",
                                                                                "ownerId", ownerId.toString(),
                                                                                "targetName", target.getGameProfile().getName()
                                                                        )
                                                                );
                                                                CoreErrorUtil.notify(source, error);
                                                                return 0;
                                                            }

                                                            Messages.send(source, CoreMessage.ISLAND_DELETE_SUCCESS,
                                                                    ownerId.toString());
                                                            return 1;
                                                        })
                                        )

                                        // 2) Offline-OwnerId (UUID) via Redis Suggestions
                                        .then(CommandManager.argument("ownerId", StringArgumentType.word())
                                                .suggests((ctx, builder) -> {
                                                    // no redis here:
                                                    IslandOwnerSuggestionCache.current().forEach(builder::suggest);
                                                    return builder.buildFuture();
                                                })
                                                .executes(ctx -> {
                                                    ServerCommandSource source = ctx.getSource();
                                                    MinecraftServer server = source.getServer();

                                                    String raw = StringArgumentType.getString(ctx, "ownerId");
                                                    UUID ownerId;
                                                    try {
                                                        ownerId = UUID.fromString(raw);
                                                    } catch (IllegalArgumentException ex) {
                                                        CoreErrorUtil.notify(source, CoreError.of(
                                                                CoreErrorCode.INVALID_ARGUMENT,
                                                                CoreErrorSeverity.WARN,
                                                                "Invalid ownerId UUID"
                                                        ).withContextEntry("ownerId", raw));
                                                        return 0;
                                                    }

                                                    handleDeleteIslandCommand(server, ownerId, source);
                                                    Messages.send(source, CoreMessage.ISLAND_DELETE_SUCCESS, ownerId.toString());
                                                    return 1;
                                                })
                                        )
                        )
                        // ----------------------------------------------------------------
                        // /island info [mode]
                        // mode ∈ {all, owner, filesystem}
                        // Tab-Completion via CommandSuggestions.strings(...)
                        // ----------------------------------------------------------------
                        .then(
                                CommandUtil.literalWithPermission(this, "info", CorePermission.ISLAND_CREATE)
                                        // /island info
                                        .executes(ctx -> executeInfo(ctx.getSource(), "all"))
                                        // /island info <mode>
                                        .then(
                                                CommandManager.argument("mode", StringArgumentType.word())
                                                        .suggests(CommandSuggestions.strings("all", "owner", "filesystem"))
                                                        .executes(ctx -> {
                                                            String mode = StringArgumentType.getString(ctx, "mode")
                                                                    .toLowerCase();
                                                            return executeInfo(ctx.getSource(), mode);
                                                        })
                                        )
                        )
        );
    }

    // ------------------------------------------------------------------------
    // /island info [mode] → Info-Output mit einfachem Mode-Switch
    // ------------------------------------------------------------------------

    private int executeInfo(ServerCommandSource source, String mode) {
        ServerPlayerEntity player = source.getPlayer();
        if (player == null) {
            Messages.send(source, CoreMessage.ONLY_PLAYERS);
            return 0;
        }

        World world = player.getWorld();
        Identifier worldId = world.getRegistryKey().getValue();
        boolean isIsland = PrivateIslandWorldNodeConfig.isIslandWorldKey(world.getRegistryKey());

        // Normaler WhereAmI-Style-Header
        if ("all".equals(mode)) {
            source.sendMessage(Messages.t(CoreMessage.WHEREAMI_HEADER));
        }

        if ("all".equals(mode) || "world".equals(mode)) {
            source.sendMessage(Messages.t(CoreMessage.WHEREAMI_WORLD_ID, worldId.toString()));
            source.sendMessage(Messages.t(CoreMessage.WHEREAMI_DIM_TYPE, world.getDimension().toString()));
            source.sendMessage(Messages.t(CoreMessage.WHEREAMI_IS_ISLAND, isIsland));
        }

        if (isIsland && ("all".equals(mode) || "owner".equals(mode))) {
            String path = worldId.getPath(); // island/<uuid>
            String[] parts = path.split("/");
            if (parts.length == 2) {
                String uuid = parts[1];
                source.sendMessage(Messages.t(CoreMessage.WHEREAMI_ISLAND_OWNER, uuid));
            }
        }

        if (isIsland && ("all".equals(mode) || "filesystem".equals(mode))) {
            var key = world.getRegistryKey();
            var resolved = PrivateIslandWorldNodeConfig
                    .resolveIslandPath(PrivateIslandWorldNodeConfig.getIslandsBasePath(), key);

            source.sendMessage(Messages.t(CoreMessage.WHEREAMI_ISLAND_FILESYSTEM, resolved.toString()));
        }

        if ("all".equals(mode)) {
            source.sendMessage(Messages.t(CoreMessage.WHEREAMI_FOOTER));
        }

        return 1;
    }

    // ------------------------------------------------------------------------
    // Lösch-logger()ik
    // ------------------------------------------------------------------------

    public static void handleDeleteIslandCommand(MinecraftServer server,
                                                 UUID ownerId,
                                                 ServerCommandSource source) {

        final PrivateIslandWorldOwnerRepositoryRedis nodeRepo;
        try {
            nodeRepo = PrivateIslandWorldOwnerRepositoryRedis.get();
        } catch (Exception ex) {
            CoreError error = new CoreError(
                    CoreErrorCode.REDIS_FAILURE,
                    CoreErrorSeverity.ERROR,
                    "Failed to obtain PrivateIslandWorldOwnerRepositoryRedis for delete",
                    ex,
                    Map.of(
                            "ownerId", ownerId.toString(),
                            "command", "island delete"
                    )
            );
            CoreErrorUtil.notify(source, error);
            return;
        }

        String nodeId;
        try {
            nodeId = nodeRepo.getAssignedNode(ownerId);
        } catch (Exception ex) {
            CoreError error = new CoreError(
                    CoreErrorCode.REDIS_FAILURE,
                    CoreErrorSeverity.ERROR,
                    "Failed to resolve assigned node for island owner",
                    ex,
                    Map.of(
                            "ownerId", ownerId.toString(),
                            "command", "island delete"
                    )
            );
            CoreErrorUtil.notify(source, error);
            return;
        }

        if (nodeId == null || nodeId.isEmpty()) {
            CoreError error = CoreError.of(
                            CoreErrorCode.ISLAND_DELETE_FAILED,
                            CoreErrorSeverity.WARN,
                            "No island node assigned for owner, nothing to delete"
                    )
                    .withContextEntry("ownerId", ownerId.toString())
                    .withContextEntry("command", "island delete");
            CoreErrorUtil.notify(source, error);
            return;
        }

        // Ist das hier selbst ein Island-Node?
        if (PrivateIslandWorldNodeConfig.IS_ISLAND_SERVER &&
                PrivateIslandWorldNodeConfig.SERVER_ID.equals(nodeId)) {

            logger("PrivateIslandCommand").info("[Island-Command] Deleting island for owner {} locally on node {}", ownerId, nodeId);
            deleteIslandForOwner(server, ownerId, source);
            return;
        }

        // Nicht der Island-Node -> Delete-Job für Zielnode erzeugen
        logger("PrivateIslandCommand").info("[Island-Command] Forwarding delete request for owner {} to island node {}", ownerId, nodeId);
        try {
            PrivateIslandWorldDeleteClient.enqueueDeleteJob(ownerId, nodeId);
        } catch (Exception ex) {
            CoreError error = new CoreError(
                    CoreErrorCode.ISLAND_DELETE_FAILED,
                    CoreErrorSeverity.ERROR,
                    "Failed to enqueue delete job for remote island node",
                    ex,
                    Map.of(
                            "ownerId", ownerId.toString(),
                            "targetNodeId", nodeId
                    )
            );
            CoreErrorUtil.notify(source, error);
        }
    }

    public static void deleteIslandForOwner(MinecraftServer server,
                                            UUID ownerId,
                                            ServerCommandSource source) {

        final PrivateIslandWorldManager manager;
        try {
            manager = PrivateIslandWorldManager.get(server);
        } catch (Exception ex) {
            CoreError error = new CoreError(
                    CoreErrorCode.SERVICE_MISSING,
                    CoreErrorSeverity.CRITICAL,
                    "PrivateIslandWorldManager not available for local island delete",
                    ex,
                    Map.of(
                            "ownerId", ownerId.toString()
                    )
            );
            CoreErrorUtil.notify(source, error);
            return;
        }

        if (manager == null) {
            CoreError error = CoreError.of(
                            CoreErrorCode.SERVICE_MISSING,
                            CoreErrorSeverity.CRITICAL,
                            "PrivateIslandWorldManager is null for local island delete"
                    )
                    .withContextEntry("ownerId", ownerId.toString());
            CoreErrorUtil.notify(source, error);
            return;
        }

        Identifier dimId = Identifier.of("coresystem", "island/" + ownerId);
        RegistryKey<World> worldKey = RegistryKey.of(RegistryKeys.WORLD, dimId);

        ServerWorld loadedWorld = server.getWorld(worldKey);

        if (loadedWorld != null) {
            // Welt ist geladen → normaler Delete-Queue-Prozess
            logger("PrivateIslandCommand").debug("Enqueuing loaded island world {} for deletion", dimId);
            try {
                manager.deleteIslandWorld(loadedWorld, ownerId);
            } catch (Exception ex) {
                CoreError error = new CoreError(
                        CoreErrorCode.ISLAND_DELETE_FAILED,
                        CoreErrorSeverity.ERROR,
                        "Failed to enqueue loaded island world for deletion",
                        ex,
                        Map.of(
                                "dimId", dimId.toString(),
                                "ownerId", ownerId.toString()
                        )
                );
                CoreErrorUtil.notify(source, error);
            }
            return;
        }

        // Welt ist NICHT geladen → FORCED DELETE
        logger("PrivateIslandCommand").debug("Island world {} not loaded. Forcing offline deletion.", dimId);

        File worldFolder;
        try {
            worldFolder = ((MinecraftServerAccess) server)
                    .getSession()
                    .getWorldDirectory(worldKey)
                    .toFile();
        } catch (Exception ex) {
            CoreError error = new CoreError(
                    CoreErrorCode.ISLAND_DELETE_FAILED,
                    CoreErrorSeverity.ERROR,
                    "Failed to resolve world directory for offline delete",
                    ex,
                    Map.of(
                            "dimId", dimId.toString(),
                            "ownerId", ownerId.toString()
                    )
            );
            CoreErrorUtil.notify(source, error);
            return;
        }

        if (worldFolder.exists()) {
            try {
                FileUtils.deleteDirectory(worldFolder);
                logger("PrivateIslandCommand").info("Successfully removed offline island {}", dimId);
            } catch (Exception e) {
                CoreError error = new CoreError(
                        CoreErrorCode.ISLAND_DELETE_FAILED,
                        CoreErrorSeverity.ERROR,
                        "Failed to delete offline world folder; scheduling deleteOnExit()",
                        e,
                        Map.of(
                                "dimId", dimId.toString(),
                                "ownerId", ownerId.toString(),
                                "worldFolder", worldFolder.getAbsolutePath()
                        )
                );
                CoreErrorUtil.notify(source, error);

                // Fallback, wie bisher
                worldFolder.deleteOnExit();
            }
        } else {
            logger("PrivateIslandCommand").info("No world directory found for {}", dimId);
        }
    }
}
