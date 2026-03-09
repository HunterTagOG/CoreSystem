package dev.huntertagog.coresystem.fabric.server.command.impl;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import dev.huntertagog.coresystem.common.command.BaseCommand;
import dev.huntertagog.coresystem.common.provider.ServiceProvider;
import dev.huntertagog.coresystem.common.text.CoreMessage;
import dev.huntertagog.coresystem.fabric.common.friends.gui.FriendGuiServerNet;
import dev.huntertagog.coresystem.fabric.common.permission.PermissionService;
import dev.huntertagog.coresystem.fabric.common.player.PlayerLookup;
import dev.huntertagog.coresystem.fabric.common.text.Messages;
import dev.huntertagog.coresystem.fabric.server.command.CommandSuggestions;
import dev.huntertagog.coresystem.fabric.server.command.CoreCommand;
import dev.huntertagog.coresystem.platform.friends.FriendService;
import dev.huntertagog.coresystem.platform.friends.OfflineFriendMessage;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

public final class FriendCommand extends BaseCommand implements CoreCommand {

    public FriendCommand() {
    }

    @Override
    public void register(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess registryAccess, CommandManager.RegistrationEnvironment environment) {

        dispatcher.register(
                literal("friend")
                        .executes(this::executeGui)
                        .then(literal("add")
                                .then(argument("player", StringArgumentType.word())
                                        .suggests(CommandSuggestions.onlinePlayers())
                                        .executes(ctx -> add(ctx.getSource(), StringArgumentType.getString(ctx, "player")))))
                        .then(literal("accept")
                                .then(argument("player", StringArgumentType.word())
                                        .suggests(CommandSuggestions.onlinePlayers())
                                        .executes(ctx -> accept(ctx.getSource(), StringArgumentType.getString(ctx, "player")))))
                        .then(literal("deny")
                                .then(argument("player", StringArgumentType.word())
                                        .suggests(CommandSuggestions.onlinePlayers())
                                        .executes(ctx -> deny(ctx.getSource(), StringArgumentType.getString(ctx, "player")))))
                        .then(literal("remove")
                                .then(argument("player", StringArgumentType.word())
                                        .suggests(CommandSuggestions.onlinePlayers())
                                        .executes(ctx -> remove(ctx.getSource(), StringArgumentType.getString(ctx, "player")))))
                        .then(literal("list")
                                .executes(ctx -> list(ctx.getSource())))
                        .then(literal("requests")
                                .executes(ctx -> requests(ctx.getSource())))
                        .then(literal("msg")
                                .then(argument("player", StringArgumentType.word())
                                        .suggests(CommandSuggestions.onlinePlayers())
                                        .then(argument("message", StringArgumentType.greedyString())
                                                .executes(ctx -> msg(
                                                        ctx.getSource(),
                                                        StringArgumentType.getString(ctx, "player"),
                                                        StringArgumentType.getString(ctx, "message")
                                                )))))
        );
    }

    // ---------------------------------------------------------------------
    // Core: Permission Gate
    // ---------------------------------------------------------------------

    private static boolean canUse(ServerCommandSource src) {
        PermissionService perms = ServiceProvider.getService(PermissionService.class);
        // Falls du keine Permission willst, gib hier einfach true zurück.
        return perms == null || perms.has(src, "coresystem.friend.use");
    }

    private static FriendService friendsOrFail(ServerCommandSource src) {
        FriendService friends = ServiceProvider.getService(FriendService.class);
        if (friends == null) {
            Messages.send(src, CoreMessage.SERVICE_UNAVAILABLE);
            return null;
        }
        return friends;
    }

    // ---------------------------------------------------------------------
    // /friend add <player>
    // ---------------------------------------------------------------------

    private int add(ServerCommandSource src, String name) {
        if (!canUse(src)) {
            Messages.send(src, CoreMessage.NO_PERMISSION);
            return 0;
        }

        ServerPlayerEntity self = src.getPlayer();
        if (self == null) {
            Messages.send(src, CoreMessage.ONLY_PLAYERS);
            return 0;
        }

        FriendService friends = friendsOrFail(src);
        if (friends == null) return 0;

        MinecraftServer server = src.getServer();

        Optional<UUID> otherOpt = PlayerLookup.resolveUuid(server, name);
        if (otherOpt.isEmpty()) {
            Messages.send(src, CoreMessage.PLAYER_UNKNOWN, name);
            return 0;
        }

        UUID other = otherOpt.get();
        UUID me = self.getUuid();

        if (me.equals(other)) {
            Messages.send(src, CoreMessage.FRIEND_SELF_DENY);
            return 0;
        }

        if (friends.areFriends(me, other)) {
            Messages.send(src, CoreMessage.FRIEND_ALREADY, name);
            return 0;
        }

        boolean ok = friends.sendRequest(me, other);
        if (!ok) {
            Messages.send(src, CoreMessage.FRIEND_REQUEST_FAILED, name);
            return 0;
        }

        Messages.send(self, CoreMessage.FRIEND_REQUEST_SENT, name);

        // Wenn Ziel online: Sofort-Hinweis
        ServerPlayerEntity online = server.getPlayerManager().getPlayer(other);
        if (online != null) {
            Messages.send(online, CoreMessage.FRIEND_REQUEST_RECEIVED, self.getGameProfile().getName());
        }

        return 1;
    }

    // ---------------------------------------------------------------------
    // /friend accept <player>
    // ---------------------------------------------------------------------

    private int accept(ServerCommandSource src, String name) {
        if (!canUse(src)) {
            Messages.send(src, CoreMessage.NO_PERMISSION);
            return 0;
        }

        ServerPlayerEntity self = src.getPlayer();
        if (self == null) {
            Messages.send(src, CoreMessage.ONLY_PLAYERS);
            return 0;
        }

        FriendService friends = friendsOrFail(src);
        if (friends == null) return 0;

        Optional<UUID> fromOpt = PlayerLookup.resolveUuid(src.getServer(), name);
        if (fromOpt.isEmpty()) {
            Messages.send(src, CoreMessage.PLAYER_UNKNOWN, name);
            return 0;
        }

        UUID from = fromOpt.get();
        boolean ok = friends.acceptRequest(self.getUuid(), from);

        if (!ok) {
            Messages.send(src, CoreMessage.FRIEND_ACCEPT_FAILED, name);
            return 0;
        }

        Messages.send(self, CoreMessage.FRIEND_ACCEPTED, name);

        // Wenn der andere online: Feedback
        ServerPlayerEntity otherOnline = src.getServer().getPlayerManager().getPlayer(from);
        if (otherOnline != null) {
            Messages.send(otherOnline, CoreMessage.FRIEND_ACCEPTED_OTHER, self.getGameProfile().getName());
        }

        return 1;
    }

    // ---------------------------------------------------------------------
    // /friend deny <player>
    // ---------------------------------------------------------------------

    private int deny(ServerCommandSource src, String name) {
        if (!canUse(src)) {
            Messages.send(src, CoreMessage.NO_PERMISSION);
            return 0;
        }

        ServerPlayerEntity self = src.getPlayer();
        if (self == null) {
            Messages.send(src, CoreMessage.ONLY_PLAYERS);
            return 0;
        }

        FriendService friends = friendsOrFail(src);
        if (friends == null) return 0;

        Optional<UUID> fromOpt = PlayerLookup.resolveUuid(src.getServer(), name);
        if (fromOpt.isEmpty()) {
            Messages.send(src, CoreMessage.PLAYER_UNKNOWN, name);
            return 0;
        }

        UUID from = fromOpt.get();
        boolean ok = friends.denyRequest(self.getUuid(), from);

        if (!ok) {
            Messages.send(src, CoreMessage.FRIEND_DENY_FAILED, name);
            return 0;
        }

        Messages.send(self, CoreMessage.FRIEND_DENIED, name);

        ServerPlayerEntity otherOnline = src.getServer().getPlayerManager().getPlayer(from);
        if (otherOnline != null) {
            Messages.send(otherOnline, CoreMessage.FRIEND_DENIED_OTHER, self.getGameProfile().getName());
        }

        return 1;
    }

    // ---------------------------------------------------------------------
    // /friend remove <player>
    // ---------------------------------------------------------------------

    private int remove(ServerCommandSource src, String name) {
        if (!canUse(src)) {
            Messages.send(src, CoreMessage.NO_PERMISSION);
            return 0;
        }

        ServerPlayerEntity self = src.getPlayer();
        if (self == null) {
            Messages.send(src, CoreMessage.ONLY_PLAYERS);
            return 0;
        }

        FriendService friends = friendsOrFail(src);
        if (friends == null) return 0;

        Optional<UUID> otherOpt = PlayerLookup.resolveUuid(src.getServer(), name);
        if (otherOpt.isEmpty()) {
            Messages.send(src, CoreMessage.PLAYER_UNKNOWN, name);
            return 0;
        }

        boolean ok = friends.removeFriend(self.getUuid(), otherOpt.get());
        if (!ok) {
            Messages.send(src, CoreMessage.FRIEND_REMOVE_FAILED, name);
            return 0;
        }

        Messages.send(self, CoreMessage.FRIEND_REMOVED, name);
        return 1;
    }

    // ---------------------------------------------------------------------
    // /friend list
    // ---------------------------------------------------------------------

    private int list(ServerCommandSource src) {
        if (!canUse(src)) {
            Messages.send(src, CoreMessage.NO_PERMISSION);
            return 0;
        }

        ServerPlayerEntity self = src.getPlayer();
        if (self == null) {
            Messages.send(src, CoreMessage.ONLY_PLAYERS);
            return 0;
        }

        FriendService friends = friendsOrFail(src);
        if (friends == null) return 0;

        List<UUID> list = friends.getFriends(self.getUuid());
        if (list == null || list.isEmpty()) {
            Messages.send(self, CoreMessage.FRIEND_LIST_EMPTY);
            return 1;
        }

        Messages.send(self, CoreMessage.FRIEND_LIST_HEADER, list.size());

        for (UUID u : list) {
            String n = PlayerLookup.resolveName(src.getServer(), u).orElse(u.toString().substring(0, 8));
            boolean online = src.getServer().getPlayerManager().getPlayer(u) != null;

            Text line = Text.literal(" - ")
                    .formatted(Formatting.DARK_GRAY)
                    .append(Text.literal(n).formatted(online ? Formatting.GREEN : Formatting.GRAY))
                    .append(Text.literal(online ? " (online)" : " (offline)").formatted(Formatting.DARK_GRAY));

            self.sendMessage(line);
        }

        return 1;
    }

    // ---------------------------------------------------------------------
    // /friend requests
    // ---------------------------------------------------------------------

    private int requests(ServerCommandSource src) {
        if (!canUse(src)) {
            Messages.send(src, CoreMessage.NO_PERMISSION);
            return 0;
        }

        ServerPlayerEntity self = src.getPlayer();
        if (self == null) {
            Messages.send(src, CoreMessage.ONLY_PLAYERS);
            return 0;
        }

        FriendService friends = friendsOrFail(src);
        if (friends == null) return 0;

        List<UUID> incoming = friends.getIncomingRequests(self.getUuid());
        List<UUID> outgoing = friends.getOutgoingRequests(self.getUuid());

        int in = incoming == null ? 0 : incoming.size();
        int out = outgoing == null ? 0 : outgoing.size();

        Messages.send(self, CoreMessage.FRIEND_REQUESTS_HEADER, in, out);

        if (in > 0) {
            Messages.send(self, CoreMessage.FRIEND_REQUESTS_INCOMING);
            for (UUID u : incoming) {
                String n = PlayerLookup.resolveName(src.getServer(), u).orElse(u.toString().substring(0, 8));
                self.sendMessage(Text.literal(" - " + n + "  §7(/friend accept " + n + " | /friend deny " + n + ")"));
            }
        }

        if (out > 0) {
            Messages.send(self, CoreMessage.FRIEND_REQUESTS_OUTGOING);
            for (UUID u : outgoing) {
                String n = PlayerLookup.resolveName(src.getServer(), u).orElse(u.toString().substring(0, 8));
                self.sendMessage(Text.literal(" - " + n));
            }
        }

        return 1;
    }

    // ---------------------------------------------------------------------
    // /friend msg <player> <message>
    // ---------------------------------------------------------------------

    private int msg(ServerCommandSource src, String targetName, String message) {
        if (!canUse(src)) {
            Messages.send(src, CoreMessage.NO_PERMISSION);
            return 0;
        }

        ServerPlayerEntity self = src.getPlayer();
        if (self == null) {
            Messages.send(src, CoreMessage.ONLY_PLAYERS);
            return 0;
        }

        if (message == null || message.isBlank()) {
            Messages.send(self, CoreMessage.FRIEND_MSG_EMPTY);
            return 0;
        }

        FriendService friends = friendsOrFail(src);
        if (friends == null) return 0;

        Optional<UUID> targetOpt = PlayerLookup.resolveUuid(src.getServer(), targetName);
        if (targetOpt.isEmpty()) {
            Messages.send(src, CoreMessage.PLAYER_UNKNOWN, targetName);
            return 0;
        }

        UUID target = targetOpt.get();

        if (!friends.areFriends(self.getUuid(), target)) {
            Messages.send(self, CoreMessage.FRIEND_MSG_NOT_FRIENDS, targetName);
            return 0;
        }

        // online => sofort DM (als Chat)
        ServerPlayerEntity online = src.getServer().getPlayerManager().getPlayer(target);
        if (online != null) {
            Text toTarget = Text.literal("§b[DM] §f" + self.getGameProfile().getName() + "§7: §f" + message);
            online.sendMessage(toTarget);

            Messages.send(self, CoreMessage.FRIEND_MSG_SENT, online.getGameProfile().getName());
            return 1;
        }

        // offline => persist
        OfflineFriendMessage off = new OfflineFriendMessage(
                target,
                self.getUuid(),
                self.getGameProfile().getName(),
                message,
                System.currentTimeMillis()
        );

        friends.sendOfflineMessage(off);
        Messages.send(self, CoreMessage.FRIEND_MSG_QUEUED, targetName);

        return 1;
    }

    // ---------------------------------------------------------------------
    // /friend gui
    // ---------------------------------------------------------------------
    private int executeGui(CommandContext<ServerCommandSource> ctx) {
        ServerCommandSource src = ctx.getSource();
        ServerPlayerEntity player = src.getPlayer();
        if (player == null) {
            Messages.send(src, CoreMessage.ONLY_PLAYERS);
            return 0;
        }

        FriendGuiServerNet.openFor(src.getServer(), player);
        return 1;
    }
}
