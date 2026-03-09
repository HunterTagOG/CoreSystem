package dev.huntertagog.coresystem.fabric.server.net;

import dev.huntertagog.coresystem.common.error.CoreError;
import dev.huntertagog.coresystem.common.error.CoreErrorCode;
import dev.huntertagog.coresystem.common.error.CoreErrorSeverity;
import dev.huntertagog.coresystem.common.log.Logger;
import dev.huntertagog.coresystem.common.log.LoggerFactory;
import dev.huntertagog.coresystem.common.model.ServerTarget;
import dev.huntertagog.coresystem.common.provider.ServiceProvider;
import dev.huntertagog.coresystem.fabric.common.net.payload.OpenMenuPayload;
import dev.huntertagog.coresystem.fabric.common.net.payload.PrivateIslandListPayload;
import dev.huntertagog.coresystem.fabric.common.net.payload.RequestPrivateIslandListPayload;
import dev.huntertagog.coresystem.fabric.common.net.payload.SelectServerPayload;
import dev.huntertagog.coresystem.fabric.common.teleport.TeleportManagerService;
import dev.huntertagog.coresystem.fabric.server.islands.PrivateIslandWorldCache;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.List;
import java.util.Objects;

public final class ServerSwitcherNetworking {

    private static final Logger LOG = LoggerFactory.get("ServerSwitcherNet");

    private ServerSwitcherNetworking() {
    }

    /**
     * Common-Init:
     * - Payload-Typen registrieren (C2S & S2C)
     * - C2S-Handler setzen
     */
    public static void init() {
        try {
            // S2C: Menü öffnen
            PayloadTypeRegistry.playS2C().register(OpenMenuPayload.ID, OpenMenuPayload.CODEC);
            PayloadTypeRegistry.playS2C().register(PrivateIslandListPayload.ID, PrivateIslandListPayload.CODEC);

            // C2S: Menüaktionen
            PayloadTypeRegistry.playC2S().register(SelectServerPayload.ID, SelectServerPayload.CODEC);
            PayloadTypeRegistry.playC2S().register(RequestPrivateIslandListPayload.ID, RequestPrivateIslandListPayload.CODEC);
        } catch (Exception e) {
            CoreError error = CoreError.of(
                    CoreErrorCode.NETWORK_REGISTRATION_FAILED,
                    CoreErrorSeverity.FATAL,
                    "Failed to register server switcher payload types."
            ).withCause(e);
            LOG.error(error.toLogString(), e);
            throw new IllegalStateException(error.toLogString(), e);
        }

        // Auswahl eines Ziel-Servers aus dem GUI
        ServerPlayNetworking.registerGlobalReceiver(SelectServerPayload.ID, (payload, context) -> {
            try {
                ServerPlayerEntity player = context.player();
                String target = payload.target();

                if (target == null || target.isEmpty()) {
                    CoreError error = CoreError.of(
                            CoreErrorCode.NETWORK_INVALID_PAYLOAD_DATA,
                            CoreErrorSeverity.WARN,
                            "Empty target in SelectServerPayload."
                    ).withContextEntry("player", player.getGameProfile().getName());
                    LOG.warn(error.toLogString());
                    return;
                }

                MinecraftServer server = player.getServer();
                if (server == null) {
                    CoreError error = CoreError.of(
                                    CoreErrorCode.NETWORK_HANDLER_FAILED,
                                    CoreErrorSeverity.ERROR,
                                    "SelectServer handler called without server instance."
                            ).withContextEntry("player", player.getGameProfile().getName())
                            .withContextEntry("target", target);
                    LOG.error(error.toLogString());
                    return;
                }

                // Routing immer über den Server-Thread
                server.execute(() -> {
                    try {
                        TeleportManagerService tp = ServiceProvider.getService(TeleportManagerService.class);
                        tp.teleportPlayer(player, target, "gui-teleport", "Player selected server from switcher GUI");
                    } catch (Exception ex) {
                        CoreError err = CoreError.of(
                                        CoreErrorCode.NETWORK_HANDLER_FAILED,
                                        CoreErrorSeverity.ERROR,
                                        "Failed to execute SelectServer routing."
                                )
                                .withContextEntry("player", player.getGameProfile().getName())
                                .withContextEntry("target", target)
                                .withCause(ex);
                        LOG.error(err.toLogString(), ex);
                    }
                });
            } catch (Exception e) {
                CoreError error = CoreError.of(
                        CoreErrorCode.NETWORK_HANDLER_FAILED,
                        CoreErrorSeverity.ERROR,
                        "Unhandled exception in SelectServerPayload handler."
                ).withCause(e);
                LOG.error(error.toLogString(), e);
            }
        });

        // Admin-Request: Liste der Spieler auf Private-Islands
        ServerPlayNetworking.registerGlobalReceiver(
                RequestPrivateIslandListPayload.ID,
                (payload, context) -> {
                    try {
                        ServerPlayerEntity player = context.player();
                        MinecraftServer server = context.server();

                        if (server == null) {
                            CoreError error = CoreError.of(
                                    CoreErrorCode.NETWORK_HANDLER_FAILED,
                                    CoreErrorSeverity.ERROR,
                                    "RequestPrivateIslandList handler without server instance."
                            ).withContextEntry("player", player.getGameProfile().getName());
                            LOG.error(error.toLogString());
                            return;
                        }

                        server.execute(() -> {
                            try {
                                sendPrivateIslandList(player);
                            } catch (Exception ex) {
                                CoreError err = CoreError.of(
                                                CoreErrorCode.NETWORK_HANDLER_FAILED,
                                                CoreErrorSeverity.ERROR,
                                                "Failed to send private island list."
                                        )
                                        .withContextEntry("player", player.getGameProfile().getName())
                                        .withCause(ex);
                                LOG.error(err.toLogString(), ex);
                            }
                        });
                    } catch (Exception e) {
                        CoreError error = CoreError.of(
                                CoreErrorCode.NETWORK_HANDLER_FAILED,
                                CoreErrorSeverity.ERROR,
                                "Unhandled exception in RequestPrivateIslandList handler."
                        ).withCause(e);
                        LOG.error(error.toLogString(), e);
                    }
                }
        );
    }

    /**
     * S2C: Öffnet das Server-Switcher-GUI beim Client.
     */
    public static void sendOpenMenu(ServerPlayerEntity player,
                                    List<ServerTarget> targets,
                                    boolean adminMode) {
        try {
            OpenMenuPayload payload = new OpenMenuPayload(adminMode, targets);
            ServerPlayNetworking.send(player, payload);
        } catch (Exception e) {
            CoreError error = CoreError.of(
                            CoreErrorCode.NETWORK_SEND_FAILED,
                            CoreErrorSeverity.WARN,
                            "Failed to send OpenMenuPayload."
                    )
                    .withContextEntry("player", player.getGameProfile().getName())
                    .withContextEntry("targetsSize", targets != null ? targets.size() : -1)
                    .withContextEntry("adminMode", adminMode)
                    .withCause(e);

            LOG.warn(error.toLogString(), e);
        }
    }

    /**
     * S2C: Schickt die aktuelle Private-Island-Liste an einen Admin-Client.
     */
    public static void sendPrivateIslandList(ServerPlayerEntity admin) {
        try {
            var server = Objects.requireNonNull(admin.getServer(), "Server must not be null");
            var list = PrivateIslandWorldCache.getOnlinePlayersOnPrivateIslands(server);

            ServerPlayNetworking.send(
                    admin,
                    new PrivateIslandListPayload(list)
            );
        } catch (Exception e) {
            CoreError error = CoreError.of(
                            CoreErrorCode.NETWORK_SEND_FAILED,
                            CoreErrorSeverity.WARN,
                            "Failed to send PrivateIslandListPayload."
                    )
                    .withContextEntry("player", admin.getGameProfile().getName())
                    .withCause(e);

            LOG.warn(error.toLogString(), e);
        }
    }
}
