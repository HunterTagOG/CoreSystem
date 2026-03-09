package dev.huntertagog.coresystem.fabric.server.command.impl;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import dev.huntertagog.coresystem.common.command.BaseCommand;
import dev.huntertagog.coresystem.common.permission.PermissionKeys;
import dev.huntertagog.coresystem.common.provider.ServiceProvider;
import dev.huntertagog.coresystem.common.text.CoreMessage;
import dev.huntertagog.coresystem.fabric.common.chat.PlayerNicknameService;
import dev.huntertagog.coresystem.fabric.common.text.Messages;
import dev.huntertagog.coresystem.fabric.server.command.CoreCommand;
import dev.huntertagog.coresystem.platform.command.CommandMeta;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

@CommandMeta(value = "coresystem_nick", permission = PermissionKeys.NICK_USE, enabled = true)
public final class NickCommand extends BaseCommand implements CoreCommand {

    public NickCommand() {
    }

    @Override
    public void register(CommandDispatcher<ServerCommandSource> dispatcher,
                         CommandRegistryAccess registryAccess,
                         net.minecraft.server.command.CommandManager.RegistrationEnvironment environment) {

        dispatcher.register(
                literal("nick")
                        .executes(ctx -> {
                            ServerPlayerEntity player = ctx.getSource().getPlayer();
                            if (player == null) {
                                Messages.send(ctx.getSource(), CoreMessage.ONLY_PLAYERS);
                                return 0;
                            }

                            // Nick entfernen
                            PlayerNicknameService service = ServiceProvider.getService(PlayerNicknameService.class);
                            if (service == null) {
                                Messages.send(player, CoreMessage.SERVICE_UNAVAILABLE);
                                return 0;
                            }

                            service.clearNickname(player.getUuid());
                            Messages.send(player, CoreMessage.NICK_CLEARED);
                            return 1;
                        })
                        .then(argument("nickname", StringArgumentType.greedyString())
                                .executes(ctx -> {
                                    ServerPlayerEntity player = ctx.getSource().getPlayer();
                                    if (player == null) {
                                        Messages.send(ctx.getSource(), CoreMessage.ONLY_PLAYERS);
                                        return 0;
                                    }

                                    String nick = StringArgumentType.getString(ctx, "nickname");

                                    // hier kannst du Length/Regex prüfen
                                    if (nick.length() < 3 || nick.length() > 16) {
                                        Messages.send(player, CoreMessage.NICK_INVALID_LENGTH);
                                        return 0;
                                    }

                                    PlayerNicknameService service = ServiceProvider.getService(PlayerNicknameService.class);
                                    if (service == null) {
                                        Messages.send(player, CoreMessage.SERVICE_UNAVAILABLE);
                                        return 0;
                                    }

                                    service.setNickname(player.getUuid(), nick);
                                    Messages.send(player, CoreMessage.NICK_SET, nick);
                                    return 1;
                                })
                        )
        );
    }
}
