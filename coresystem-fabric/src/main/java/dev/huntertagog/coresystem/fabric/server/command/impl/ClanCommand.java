package dev.huntertagog.coresystem.fabric.server.command.impl;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import dev.huntertagog.coresystem.common.command.BaseCommand;
import dev.huntertagog.coresystem.common.permission.CorePermission;
import dev.huntertagog.coresystem.common.provider.ServiceProvider;
import dev.huntertagog.coresystem.common.text.CoreMessage;
import dev.huntertagog.coresystem.fabric.common.clans.gui.ClanGuiServerNet;
import dev.huntertagog.coresystem.fabric.common.error.CoreErrorUtil;
import dev.huntertagog.coresystem.fabric.common.text.Messages;
import dev.huntertagog.coresystem.fabric.server.command.CommandUtil;
import dev.huntertagog.coresystem.fabric.server.command.CoreCommand;
import dev.huntertagog.coresystem.platform.clans.Clan;
import dev.huntertagog.coresystem.platform.clans.ClanService;
import dev.huntertagog.coresystem.platform.clans.chat.ClanChatService;
import dev.huntertagog.coresystem.platform.command.CommandMeta;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Formatting;

import java.util.Optional;
import java.util.UUID;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

@CommandMeta(
        value = "clan",
        permission = "coresystem.clan.base", // oder PermissionKeys.CLAN_BASE
        enabled = true
)
public final class ClanCommand extends BaseCommand implements CoreCommand {

    public ClanCommand() {
    }

    @Override
    public void register(CommandDispatcher<ServerCommandSource> dispatcher,
                         CommandRegistryAccess registryAccess,
                         CommandManager.RegistrationEnvironment environment) {

        dispatcher.register(
                literal("clan")
                        .executes(this::executeGui)

                        // /clan create <tag> <name...>
                        .then(
                                CommandUtil.literalWithPermission(this, "create", CorePermission.PLAYER_CLAN_CREATE)
                                        .then(argument("tag", StringArgumentType.word())
                                                .then(argument("name", StringArgumentType.greedyString())
                                                        .executes(this::executeCreate)))
                        )
                        // /clan invite <player>
                        .then(
                                CommandUtil.literalWithPermission(this, "invite", CorePermission.PLAYER_CLAN_INVITE)
                                        .then(argument("player", StringArgumentType.word())
                                                .executes(this::executeInvite))
                        )
                        // /clan join <tag>  (Akzeptiert Invite)
                        .then(
                                CommandUtil.literalWithPermission(this, "join", CorePermission.PLAYER_CLAN_JOIN)
                                        .then(argument("tag", StringArgumentType.word())
                                                .executes(this::executeJoin))
                        )
                        // /clan leave
                        .then(
                                CommandUtil.literalWithPermission(this, "leave", CorePermission.PLAYER_CLAN_LEAVE)
                                        .executes(this::executeLeave)
                        )
                        // /clan info
                        .then(
                                CommandUtil.literalWithPermission(this, "info", CorePermission.PLAYER_CLAN_INFO)
                                        .executes(this::executeInfo)
                        )
                        // /clan chat [on|off|toggle]
                        .then(
                                CommandUtil.literalWithPermission(this, "chat", CorePermission.PLAYER_CLAN_CHAT)
                                        .executes(this::executeChatToggle)
                                        .then(argument("mode", StringArgumentType.word())
                                                .suggests((c, b) -> {
                                                    b.suggest("on");
                                                    b.suggest("off");
                                                    b.suggest("toggle");
                                                    return b.buildFuture();
                                                })
                                                .executes(this::executeChatMode))
                        )
        );
    }

    // ------------------------------------------------------------------------
    // /clan create <tag> <name...>
    // ------------------------------------------------------------------------

    private int executeCreate(CommandContext<ServerCommandSource> ctx) {
        ServerCommandSource src = ctx.getSource();
        ServerPlayerEntity player = src.getPlayer();
        if (player == null) {
            Messages.send(src, CoreMessage.ONLY_PLAYERS);
            return 0;
        }

        ClanService clans = ServiceProvider.getService(ClanService.class);
        if (clans == null) {
            CoreErrorUtil.notifyServiceMissing(src, "ClanService", "clan create");
            return 0;
        }

        String tag = StringArgumentType.getString(ctx, "tag");
        String name = StringArgumentType.getString(ctx, "name");

        // already in clan?
        if (clans.findByMember(player.getUuid()).isPresent()) {
            Messages.send(player, CoreMessage.CLAN_ALREADY_IN_CLAN);
            return 0;
        }

        Clan created = clans.createClan(player.getUuid(), tag, name);
        if (created == null) {
            // Clan konnte nicht erstellt werden (Tag belegt, Fehler, ...)
            Messages.send(player, CoreMessage.CLAN_CREATE_FAILED, tag);
            return 0;
        }

        Messages.send(player, CoreMessage.CLAN_CREATE_SUCCESS, created.tag(), created.name());
        return 1;
    }

    // ------------------------------------------------------------------------
    // /clan invite <player>
    // ------------------------------------------------------------------------

    private int executeInvite(CommandContext<ServerCommandSource> ctx) {
        ServerCommandSource src = ctx.getSource();
        ServerPlayerEntity player = src.getPlayer();
        if (player == null) {
            Messages.send(src, CoreMessage.ONLY_PLAYERS);
            return 0;
        }

        ClanService clans = ServiceProvider.getService(ClanService.class);
        if (clans == null) {
            CoreErrorUtil.notifyServiceMissing(src, "ClanService", "clan invite");
            return 0;
        }

        String targetName = StringArgumentType.getString(ctx, "player");
        MinecraftServer server = src.getServer();
        ServerPlayerEntity target = server.getPlayerManager().getPlayer(targetName);

        if (target == null) {
            Messages.send(player, CoreMessage.CLAN_INVITE_TARGET_OFFLINE, targetName);
            return 0;
        }

        Optional<Clan> myClanOpt = clans.findByMember(player.getUuid());
        if (myClanOpt.isEmpty()) {
            Messages.send(player, CoreMessage.CLAN_NOT_IN_CLAN);
            return 0;
        }

        Clan clan = myClanOpt.get();
        boolean ok = clans.inviteMember(clan.id(), player.getUuid(), target.getUuid());
        if (!ok) {
            Messages.send(player, CoreMessage.CLAN_INVITE_FAILED, target.getGameProfile().getName());
            return 0;
        }

        Messages.send(player, CoreMessage.CLAN_INVITE_SENT, target.getGameProfile().getName());
        Messages.send(target, CoreMessage.CLAN_INVITE_RECEIVED, clan.tag(), clan.name());
        return 1;
    }

    // ------------------------------------------------------------------------
    // /clan join <tag>  → akzeptiert Invite
    // ------------------------------------------------------------------------

    private int executeJoin(CommandContext<ServerCommandSource> ctx) {
        ServerCommandSource src = ctx.getSource();
        ServerPlayerEntity player = src.getPlayer();
        if (player == null) {
            Messages.send(src, CoreMessage.ONLY_PLAYERS);
            return 0;
        }

        ClanService clans = ServiceProvider.getService(ClanService.class);
        if (clans == null) {
            CoreErrorUtil.notifyServiceMissing(src, "ClanService", "clan join");
            return 0;
        }

        // bereits in Clan?
        if (clans.findByMember(player.getUuid()).isPresent()) {
            Messages.send(player, CoreMessage.CLAN_ALREADY_IN_CLAN);
            return 0;
        }

        String tag = StringArgumentType.getString(ctx, "tag");
        Optional<Clan> clanOpt = clans.findByTag(tag);
        if (clanOpt.isEmpty()) {
            Messages.send(player, CoreMessage.CLAN_NOT_FOUND_BY_TAG, tag);
            return 0;
        }

        Clan clan = clanOpt.get();
        boolean ok = clans.acceptInvite(clan.id(), player.getUuid());
        if (!ok) {
            Messages.send(player, CoreMessage.CLAN_JOIN_FAILED_NO_INVITE, clan.tag());
            return 0;
        }

        Messages.send(player, CoreMessage.CLAN_JOIN_SUCCESS, clan.tag(), clan.name());
        return 1;
    }

    // ------------------------------------------------------------------------
    // /clan leave
    // ------------------------------------------------------------------------

    private int executeLeave(CommandContext<ServerCommandSource> ctx) {
        ServerCommandSource src = ctx.getSource();
        ServerPlayerEntity player = src.getPlayer();
        if (player == null) {
            Messages.send(src, CoreMessage.ONLY_PLAYERS);
            return 0;
        }

        ClanService clans = ServiceProvider.getService(ClanService.class);
        if (clans == null) {
            CoreErrorUtil.notifyServiceMissing(src, "ClanService", "clan leave");
            return 0;
        }

        UUID self = player.getUuid();
        Optional<Clan> opt = clans.findByMember(self);
        if (opt.isEmpty()) {
            Messages.send(player, CoreMessage.CLAN_NOT_IN_CLAN);
            return 0;
        }

        Clan clan = opt.get();
        if (clan.isOwner(self)) {
            // Owner kann nicht einfach /leave benutzen
            Messages.send(player, CoreMessage.CLAN_OWNER_CANNOT_LEAVE);
            return 0;
        }

        boolean ok = clans.kickMember(clan.id(), self, self);
        if (!ok) {
            Messages.send(player, CoreMessage.CLAN_LEAVE_FAILED);
            return 0;
        }

        Messages.send(player, CoreMessage.CLAN_LEAVE_SUCCESS, clan.tag());
        return 1;
    }

    // ------------------------------------------------------------------------
    // /clan info
    // ------------------------------------------------------------------------

    private int executeInfo(CommandContext<ServerCommandSource> ctx) {
        ServerCommandSource src = ctx.getSource();
        ServerPlayerEntity player = src.getPlayer();
        if (player == null) {
            Messages.send(src, CoreMessage.ONLY_PLAYERS);
            return 0;
        }

        ClanService clans = ServiceProvider.getService(ClanService.class);
        if (clans == null) {
            CoreErrorUtil.notifyServiceMissing(src, "ClanService", "clan info");
            return 0;
        }

        Optional<Clan> opt = clans.findByMember(player.getUuid());
        if (opt.isEmpty()) {
            Messages.send(player, CoreMessage.CLAN_NOT_IN_CLAN);
            return 0;
        }

        Clan clan = opt.get();

        src.sendFeedback(
                () -> Messages.t(CoreMessage.CLAN_INFO_HEADER, clan.tag(), clan.name())
                        .copy().formatted(Formatting.GOLD),
                false
        );

        src.sendFeedback(
                () -> Messages.t(CoreMessage.CLAN_INFO_OWNER, clan.ownerId().toString())
                        .copy().formatted(Formatting.GRAY),
                false
        );

        src.sendFeedback(
                () -> Messages.t(CoreMessage.CLAN_INFO_MEMBERS, clan.members().size())
                        .copy().formatted(Formatting.GRAY),
                false
        );

        return 1;
    }

    // ------------------------------------------------------------------------
    // /clan chat [on|off|toggle]
    // ------------------------------------------------------------------------
    private int executeChatToggle(CommandContext<ServerCommandSource> ctx) {
        return executeChatModeInternal(ctx, "toggle");
    }

    private int executeChatMode(CommandContext<ServerCommandSource> ctx) {
        String mode = StringArgumentType.getString(ctx, "mode");
        return executeChatModeInternal(ctx, mode);
    }

    private int executeChatModeInternal(CommandContext<ServerCommandSource> ctx, String mode) {
        ServerCommandSource src = ctx.getSource();
        ServerPlayerEntity player = src.getPlayer();
        if (player == null) {
            Messages.send(src, CoreMessage.ONLY_PLAYERS);
            return 0;
        }

        ClanService clans = ServiceProvider.getService(ClanService.class);
        if (clans == null) {
            CoreErrorUtil.notifyServiceMissing(src, "ClanService", "clan chat");
            return 0;
        }

        // Nur sinnvoll, wenn Spieler auch im Clan ist
        Optional<Clan> clanOpt = clans.findByMember(player.getUuid());
        if (clanOpt.isEmpty()) {
            Messages.send(player, CoreMessage.CLAN_NOT_IN_CLAN);
            return 0;
        }

        var chat = ServiceProvider.getService(ClanChatService.class);
        if (chat == null) {
            CoreErrorUtil.notifyServiceMissing(src, "ClanChatService", "clan chat");
            return 0;
        }

        boolean enabled;
        switch (mode.toLowerCase()) {
            case "on" -> {
                chat.setClanChatEnabled(player.getUuid(), true);
                enabled = true;
            }
            case "off" -> {
                chat.setClanChatEnabled(player.getUuid(), false);
                enabled = false;
            }
            case "toggle" -> enabled = chat.toggle(player.getUuid());
            default -> {
                Messages.send(player, CoreMessage.INVALID_ARGUMENT);
                return 0;
            }
        }

        if (enabled) {
            Messages.send(player, CoreMessage.CLAN_CHAT_ENABLED);
        } else {
            Messages.send(player, CoreMessage.CLAN_CHAT_DISABLED);
        }

        return 1;
    }

    // ------------------------------------------------------------------------
    // /clan
    // ------------------------------------------------------------------------
    private int executeGui(CommandContext<ServerCommandSource> ctx) {
        ServerCommandSource src = ctx.getSource();
        ServerPlayerEntity player = src.getPlayer();
        if (player == null) {
            Messages.send(src, CoreMessage.ONLY_PLAYERS);
            return 0;
        }

        ClanGuiServerNet.openFor(src.getServer(), player);
        return 1;
    }
}
