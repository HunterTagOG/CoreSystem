package dev.huntertagog.coresystem.fabric.common.player.lifecycle;

import dev.huntertagog.coresystem.common.error.CoreError;
import dev.huntertagog.coresystem.common.error.CoreErrorCode;
import dev.huntertagog.coresystem.common.error.CoreErrorSeverity;
import dev.huntertagog.coresystem.common.log.Logger;
import dev.huntertagog.coresystem.common.log.LoggerFactory;
import dev.huntertagog.coresystem.common.provider.ServiceProvider;
import dev.huntertagog.coresystem.common.text.CoreMessage;
import dev.huntertagog.coresystem.fabric.common.player.PlayerContext;
import dev.huntertagog.coresystem.fabric.common.text.Messages;
import dev.huntertagog.coresystem.platform.clans.ClanInvite;
import dev.huntertagog.coresystem.platform.clans.ClanService;
import dev.huntertagog.coresystem.platform.friends.FriendService;
import dev.huntertagog.coresystem.platform.friends.OfflineFriendMessage;
import dev.huntertagog.coresystem.platform.message.PlatformText;
import dev.huntertagog.coresystem.platform.message.PlayerMessageService;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

public final class PlayerLifecycleListener {

    private static final Logger LOG = LoggerFactory.get("PlayerLifecycle");

    private static final String CURRENT_SERVER_NAME =
            System.getenv().getOrDefault("SERVER_NAME", "unknown-server");

    // wie bei dir vereinbart: SERVER_ID kommt aus ENV
    private static final String NODE_ID =
            System.getenv().getOrDefault("SERVER_ID", "unknown-node");

    private static final DateTimeFormatter TS =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm").withZone(ZoneId.systemDefault());

    private PlayerLifecycleListener() {
    }

    public static void register() {
        ServerPlayConnectionEvents.JOIN.register(PlayerLifecycleListener::onJoin);
        ServerPlayConnectionEvents.DISCONNECT.register(PlayerLifecycleListener::onQuit);
    }

    private static PlayerMessageService messages() {
        PlayerMessageService service = ServiceProvider.getService(PlayerMessageService.class);
        if (service == null) {
            CoreError.of(
                    CoreErrorCode.SERVICE_MISSING,
                    CoreErrorSeverity.CRITICAL,
                    "PlayerMessageService not registered in PlayerLifecycleListener"
            ).withContextEntry("service", "platform.message.PlayerMessageService").log();
            throw new IllegalStateException("PlayerMessageService not registered");
        }
        return service;
    }

    private static PlatformText pt(Text mcText) {
        // bewusst plain, weil platform-api kein MC-Text kennt
        return PlatformText.of(mcText.getString());
    }

    private static void onJoin(ServerPlayNetworkHandler handler, PacketSender sender, MinecraftServer server) {
        ServerPlayerEntity player = handler.getPlayer();
        UUID playerId = player.getUuid();

        try {
            PlayerContext ctx = PlayerContext.onJoin(player, CURRENT_SERVER_NAME, NODE_ID);

            if (ctx.isFirstJoin()) {
                messages().sendInfoChat(
                        playerId,
                        pt(Messages.t(CoreMessage.FIRST_JOIN_WELCOME_MESSAGE, ctx.name()))
                );
            } else {
                messages().sendInfoChat(
                        playerId,
                        pt(Messages.t(CoreMessage.JOIN_WELCOME_BACK_MESSAGE, ctx.name()))
                );
            }

            handleOfflineMessages(player);
            handleClanInvites(player);

        } catch (Exception e) {
            CoreError.of(
                            CoreErrorCode.PLAYERCONTEXT_INIT_FAILED,
                            CoreErrorSeverity.ERROR,
                            "Failed to initialize PlayerContext on join. Player will continue without profile context."
                    )
                    .withContextEntry("uuid", player.getUuidAsString())
                    .withContextEntry("server", CURRENT_SERVER_NAME)
                    .withContextEntry("nodeId", NODE_ID)
                    .withCause(e)
                    .log();
        }
    }

    private static void onQuit(ServerPlayNetworkHandler handler, MinecraftServer server) {
        ServerPlayerEntity player = handler.getPlayer();

        try {
            PlayerContext.onQuit(player, CURRENT_SERVER_NAME, NODE_ID);
        } catch (Exception e) {
            CoreError.of(
                            CoreErrorCode.PLAYERCONTEXT_QUIT_FAILED,
                            CoreErrorSeverity.WARN,
                            "Failed to handle PlayerContext on quit. Ignoring."
                    )
                    .withContextEntry("uuid", player.getUuidAsString())
                    .withContextEntry("server", CURRENT_SERVER_NAME)
                    .withContextEntry("nodeId", NODE_ID)
                    .withCause(e)
                    .log();
        }
    }

    private static void handleOfflineMessages(ServerPlayerEntity player) {
        FriendService friends = ServiceProvider.getService(FriendService.class);
        if (friends == null) {
            CoreError.of(
                            CoreErrorCode.SERVICE_MISSING,
                            CoreErrorSeverity.WARN,
                            "FriendService not available for offline message delivery"
                    )
                    .withContextEntry("playerUuid", player.getUuidAsString())
                    .log();
            return;
        }

        List<OfflineFriendMessage> msgs = friends.pollOfflineMessages(player.getUuid());
        if (msgs == null || msgs.isEmpty()) return;

        PlayerMessageService pms = ServiceProvider.getService(PlayerMessageService.class);

        Text header = Messages.t(CoreMessage.FRIEND_OFFLINE_HEADER, msgs.size());

        if (pms != null) {
            pms.sendToastInfo(
                    player.getUuid(),
                    toast(
                            Messages.t(CoreMessage.FRIEND_OFFLINE_TOAST_TITLE),
                            header
                    )
            );
        } else {
            // Fallback: alter Fabric-Chat
            player.sendMessage(header);
        }

        int limit = Math.min(5, msgs.size());
        for (int i = 0; i < limit; i++) {
            OfflineFriendMessage msg = msgs.get(i);
            String ts = TS.format(Instant.ofEpochMilli(msg.sentAtEpochMillis()));

            Text line = Messages.t(
                    CoreMessage.FRIEND_OFFLINE_LINE,
                    ts,
                    msg.senderName(),
                    msg.message()
            );

            if (pms != null) {
                pms.sendInfoChat(player.getUuid(), pt(line));
            } else {
                player.sendMessage(line);
            }
        }

        if (msgs.size() > limit) {
            int remaining = msgs.size() - limit;
            Text more = Messages.t(CoreMessage.FRIEND_OFFLINE_MORE, remaining);

            if (pms != null) {
                pms.sendInfoChat(player.getUuid(), pt(more));
            } else {
                player.sendMessage(more);
            }
        }

        LOG.info("Delivered {} offline friend messages to {}", msgs.size(), player.getGameProfile().getName());
    }

    private static void handleClanInvites(ServerPlayerEntity player) {
        ClanService clans = ServiceProvider.getService(ClanService.class);
        if (clans == null) {
            CoreError.of(
                            CoreErrorCode.SERVICE_MISSING,
                            CoreErrorSeverity.WARN,
                            "ClanService not available for clan invite delivery"
                    )
                    .withContextEntry("playerUuid", player.getUuidAsString())
                    .log();
            return;
        }

        List<ClanInvite> invites = clans.pollPendingInvites(player.getUuid());
        if (invites == null || invites.isEmpty()) return;

        PlayerMessageService pms = ServiceProvider.getService(PlayerMessageService.class);

        Text header = Messages.t(CoreMessage.CLAN_INVITES_HEADER, invites.size());

        if (pms != null) {
            pms.sendToastInfo(
                    player.getUuid(),
                    toast(
                            Messages.t(CoreMessage.CLAN_INVITES_TOAST_TITLE),
                            header
                    )
            );
        } else {
            player.sendMessage(header);
        }

        int limit = Math.min(5, invites.size());
        for (int i = 0; i < limit; i++) {
            ClanInvite inv = invites.get(i);
            String ts = TS.format(Instant.ofEpochMilli(inv.getCreatedAtEpochMillis()));

            String clanName = inv.getClanName();

            Text line = Messages.t(
                    CoreMessage.CLAN_INVITES_LINE,
                    ts,
                    inv.getInviterName(),
                    clanName,
                    clanName,
                    clanName
            );

            if (pms != null) {
                pms.sendInfoChat(player.getUuid(), pt(line));
            } else {
                player.sendMessage(line);
            }
        }

        if (invites.size() > limit) {
            Text more = Messages.t(CoreMessage.CLAN_INVITES_MORE);
            if (pms != null) {
                pms.sendInfoChat(player.getUuid(), pt(more));
            } else {
                player.sendMessage(more);
            }
        }

        LOG.info("Delivered {} clan invites to {}", invites.size(), player.getGameProfile().getName());
    }

    private static PlatformText toast(Text title, Text body) {
        // Company-Style: einheitlich, kurz, ohne MC-Formatting-Abhängigkeiten
        String t = title.getString();
        String b = body.getString();

        if (t.isBlank()) return PlatformText.of(b);
        if (b.isBlank()) return PlatformText.of(t);
        return PlatformText.of(t + " - " + b);
    }
}
