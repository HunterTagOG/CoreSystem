package dev.huntertagog.coresystem.fabric.server.command.impl;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import dev.huntertagog.coresystem.common.command.BaseCommand;
import dev.huntertagog.coresystem.common.friends.follow.RedisDailyUsageCounter;
import dev.huntertagog.coresystem.common.friends.follow.RedisFollowRequestStore;
import dev.huntertagog.coresystem.common.player.PlayerProfileLookup;
import dev.huntertagog.coresystem.common.provider.ServiceProvider;
import dev.huntertagog.coresystem.common.text.CoreMessage;
import dev.huntertagog.coresystem.fabric.common.friends.follow.FollowLimitResolver;
import dev.huntertagog.coresystem.fabric.common.permission.PermissionService;
import dev.huntertagog.coresystem.fabric.common.player.PlayerContext;
import dev.huntertagog.coresystem.fabric.common.teleport.TeleportManagerService;
import dev.huntertagog.coresystem.fabric.common.text.Messages;
import dev.huntertagog.coresystem.fabric.server.command.CoreCommand;
import dev.huntertagog.coresystem.platform.friends.FriendService;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.Optional;
import java.util.UUID;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

public final class FriendFollowCommand extends BaseCommand implements CoreCommand {

    public FriendFollowCommand() {
    }

    private final RedisFollowRequestStore requests = new RedisFollowRequestStore();
    private final RedisDailyUsageCounter daily = new RedisDailyUsageCounter();

    @Override
    public void register(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess registryAccess, CommandManager.RegistrationEnvironment environment) {

        dispatcher.register(
                literal("friend")
                        .then(literal("follow")
                                // /friend follow <player>
                                .then(argument("player", StringArgumentType.word())
                                        .executes(ctx -> requestFollow(ctx.getSource(),
                                                StringArgumentType.getString(ctx, "player"))))
                                // /friend follow accept <player>
                                .then(literal("accept")
                                        .then(argument("player", StringArgumentType.word())
                                                .executes(ctx -> acceptFollow(ctx.getSource(),
                                                        StringArgumentType.getString(ctx, "player")))))
                                // /friend follow deny <player>
                                .then(literal("deny")
                                        .then(argument("player", StringArgumentType.word())
                                                .executes(ctx -> denyFollow(ctx.getSource(),
                                                        StringArgumentType.getString(ctx, "player")))))
                        )
        );
    }

    private int requestFollow(ServerCommandSource source, String targetName) {
        ServerPlayerEntity requester = source.getPlayer();
        if (requester == null) {
            Messages.send(source, CoreMessage.ONLY_PLAYERS);
            return 0;
        }

        PermissionService perms = ServiceProvider.getService(PermissionService.class);
        if (perms == null || !perms.has(source, "coresystem.friend.follow.use")) {
            Messages.send(source, CoreMessage.NO_PERMISSION);
            return 0;
        }

        // Target muss online sein, sonst kannst du nicht "requesten"
        ServerPlayerEntity target = source.getServer().getPlayerManager().getPlayer(targetName);
        if (target == null) {
            Messages.send(source, CoreMessage.PLAYER_NOT_ONLINE, targetName);
            return 0;
        }

        UUID a = requester.getUuid();
        UUID b = target.getUuid();

        if (a.equals(b)) {
            Messages.send(source, CoreMessage.FOLLOW_SELF_DENY);
            return 0;
        }

        FriendService friends = ServiceProvider.getService(FriendService.class);
        if (friends == null || !friends.areFriends(a, b)) {
            Messages.send(source, CoreMessage.FOLLOW_NOT_FRIENDS);
            return 0;
        }

        // Daily Limit
        int limit = FollowLimitResolver.resolveDailyLimit(perms, source);
        if (limit != Integer.MAX_VALUE) {
            int used = daily.get(a);
            if (used >= limit) {
                Messages.send(source, CoreMessage.FOLLOW_DAILY_LIMIT, used, limit);
                return 0;
            }
        }

        // Duplicate request?
        if (requests.hasRequest(b, a)) {
            Messages.send(source, CoreMessage.FOLLOW_ALREADY_REQUESTED, target.getGameProfile().getName());
            return 0;
        }

        // Store request (TTL)
        requests.putRequest(b, a, requester.getGameProfile().getName(), System.currentTimeMillis());

        // Notify both
        Messages.send(requester, CoreMessage.FOLLOW_REQUEST_SENT, target.getGameProfile().getName());
        Messages.send(target, CoreMessage.FOLLOW_REQUEST_RECEIVED, requester.getGameProfile().getName());

        return 1;
    }

    private int acceptFollow(ServerCommandSource source, String requesterName) {
        ServerPlayerEntity target = source.getPlayer();
        if (target == null) {
            Messages.send(source, CoreMessage.ONLY_PLAYERS);
            return 0;
        }

        ServerPlayerEntity requester = source.getServer().getPlayerManager().getPlayer(requesterName);
        if (requester == null) {
            Messages.send(source, CoreMessage.PLAYER_NOT_ONLINE, requesterName);
            return 0;
        }

        UUID targetId = target.getUuid();
        UUID requesterId = requester.getUuid();

        if (!requests.hasRequest(targetId, requesterId)) {
            Messages.send(source, CoreMessage.FOLLOW_NO_REQUEST, requester.getGameProfile().getName());
            return 0;
        }

        // Consume request first (so it can’t be replayed)
        requests.removeRequest(targetId, requesterId);

        // Daily usage increments on the requester (the one who is following)
        PermissionService perms = ServiceProvider.getService(PermissionService.class);
        int limit = (perms != null) ? FollowLimitResolver.resolveDailyLimit(perms, requester.getCommandSource()) : 3;
        if (limit != Integer.MAX_VALUE) {
            int usedNow = daily.incrementAndGet(requesterId);
            if (usedNow > limit) {
                // exceeded due to race: deny the follow
                Messages.send(requester, CoreMessage.FOLLOW_DAILY_LIMIT, usedNow, limit);
                Messages.send(target, CoreMessage.FOLLOW_ACCEPT_RACE_DENY, requester.getGameProfile().getName());
                return 0;
            }
        }

        // Resolve target last location from your PlayerContext method
        PlayerContext ctx = PlayerContext.of(requester); // requester context
        Optional<PlayerProfileLookup.LastLocation> locOpt = ctx.resolveLastLocation(targetId);

        if (locOpt.isEmpty() || locOpt.get().lastServer().equalsIgnoreCase("unknown")) {
            Messages.send(requester, CoreMessage.FOLLOW_TARGET_LOCATION_UNKNOWN, target.getGameProfile().getName());
            Messages.send(target, CoreMessage.FOLLOW_ACCEPTED_BUT_UNKNOWN, requester.getGameProfile().getName());
            return 1;
        }

        String serverName = locOpt.get().lastServer();

        // Notify
        Messages.send(requester, CoreMessage.FOLLOW_ACCEPTED, target.getGameProfile().getName(), serverName);
        Messages.send(target, CoreMessage.FOLLOW_ACCEPTED_TARGET, requester.getGameProfile().getName());

        // Proxy connect
        TeleportManagerService tp = ServiceProvider.getService(TeleportManagerService.class);
        tp.teleportPlayer(requester, serverName, "friend-followup", "Friend follow accepted");

        return 1;
    }

    private int denyFollow(ServerCommandSource source, String requesterName) {
        ServerPlayerEntity target = source.getPlayer();
        if (target == null) {
            Messages.send(source, CoreMessage.ONLY_PLAYERS);
            return 0;
        }

        ServerPlayerEntity requester = source.getServer().getPlayerManager().getPlayer(requesterName);
        if (requester == null) {
            Messages.send(source, CoreMessage.PLAYER_NOT_ONLINE, requesterName);
            return 0;
        }

        UUID targetId = target.getUuid();
        UUID requesterId = requester.getUuid();

        if (!requests.hasRequest(targetId, requesterId)) {
            Messages.send(source, CoreMessage.FOLLOW_NO_REQUEST, requester.getGameProfile().getName());
            return 0;
        }

        requests.removeRequest(targetId, requesterId);

        Messages.send(target, CoreMessage.FOLLOW_DENIED_TARGET, requester.getGameProfile().getName());
        Messages.send(requester, CoreMessage.FOLLOW_DENIED, target.getGameProfile().getName());

        return 1;
    }
}
