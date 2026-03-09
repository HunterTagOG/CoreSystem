package dev.huntertagog.coresystem.fabric.server.command.impl;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import dev.huntertagog.coresystem.common.command.BaseCommand;
import dev.huntertagog.coresystem.common.permission.PermissionKeys;
import dev.huntertagog.coresystem.common.provider.ServiceProvider;
import dev.huntertagog.coresystem.common.text.CoreMessage;
import dev.huntertagog.coresystem.fabric.common.text.Messages;
import dev.huntertagog.coresystem.fabric.server.command.CoreCommand;
import dev.huntertagog.coresystem.platform.chat.ChatFilterService;
import dev.huntertagog.coresystem.platform.command.CommandMeta;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.server.command.ServerCommandSource;

import java.util.Set;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

@CommandMeta(value = "coresystem_chatfilter", permission = PermissionKeys.STAFF_CHAT_ADMIN, enabled = true)
public final class ChatFilterCommand extends BaseCommand implements CoreCommand {

    public ChatFilterCommand() {
    }

    @Override
    public void register(CommandDispatcher<ServerCommandSource> dispatcher,
                         CommandRegistryAccess registryAccess,
                         net.minecraft.server.command.CommandManager.RegistrationEnvironment environment) {

        dispatcher.register(
                literal("chatfilter")
                        // /chatfilter add <wort>
                        .then(literal("add")
                                .then(argument("word", StringArgumentType.word())
                                        .executes(ctx -> {
                                            ServerCommandSource src = ctx.getSource();
                                            String word = StringArgumentType.getString(ctx, "word");

                                            ChatFilterService service = ServiceProvider.getService(ChatFilterService.class);
                                            if (service == null) {
                                                Messages.send(src, CoreMessage.SERVICE_UNAVAILABLE);
                                                return 0;
                                            }

                                            service.addBadWord(word);
                                            Messages.send(src, CoreMessage.CHATFILTER_ADDED, word.toLowerCase());
                                            return 1;
                                        })
                                )
                        )

                        // /chatfilter remove <wort>
                        .then(literal("remove")
                                .then(argument("word", StringArgumentType.word())
                                        .executes(ctx -> {
                                            ServerCommandSource src = ctx.getSource();
                                            String word = StringArgumentType.getString(ctx, "word");

                                            ChatFilterService service = ServiceProvider.getService(ChatFilterService.class);
                                            if (service == null) {
                                                Messages.send(src, CoreMessage.SERVICE_UNAVAILABLE);
                                                return 0;
                                            }

                                            service.removeBadWord(word);
                                            Messages.send(src, CoreMessage.CHATFILTER_REMOVED, word.toLowerCase());
                                            return 1;
                                        })
                                )
                        )

                        // /chatfilter list
                        .then(literal("list")
                                .executes(ctx -> {
                                    ServerCommandSource src = ctx.getSource();

                                    ChatFilterService service = ServiceProvider.getService(ChatFilterService.class);
                                    if (service == null) {
                                        Messages.send(src, CoreMessage.SERVICE_UNAVAILABLE);
                                        return 0;
                                    }

                                    Set<String> words = service.getBadWords();
                                    if (words.isEmpty()) {
                                        Messages.send(src, CoreMessage.CHATFILTER_LIST_EMPTY);
                                        return 1;
                                    }

                                    Messages.send(src, CoreMessage.CHATFILTER_LIST_HEADER, words.size());
                                    for (String w : words) {
                                        Messages.send(src, CoreMessage.CHATFILTER_LIST_ENTRY, w);
                                    }
                                    return 1;
                                })
                        )
        );
    }
}
