package dev.huntertagog.coresystem.fabric.server.command;

import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import dev.huntertagog.coresystem.common.error.CoreError;
import dev.huntertagog.coresystem.common.error.CoreErrorCode;
import dev.huntertagog.coresystem.common.error.CoreErrorSeverity;
import dev.huntertagog.coresystem.common.log.Logger;
import dev.huntertagog.coresystem.common.log.LoggerFactory;
import dev.huntertagog.coresystem.common.permission.CorePermission;
import net.minecraft.server.command.ServerCommandSource;

import java.util.Collection;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Predicate;

public final class CommandSuggestions {

    private static final Logger LOG = LoggerFactory.get("CommandSuggestions");

    private CommandSuggestions() {
    }

    public static SuggestionProvider<ServerCommandSource> strings(Collection<String> options) {
        return (ctx, builder) -> suggestMatching(options, builder);
    }

    public static SuggestionProvider<ServerCommandSource> strings(String... options) {
        return (ctx, builder) -> suggestMatching(java.util.List.of(options), builder);
    }

    public static <E extends Enum<E>> SuggestionProvider<ServerCommandSource> enumValues(Class<E> enumClass) {
        return (ctx, builder) -> {
            try {
                E[] constants = enumClass.getEnumConstants();
                if (constants == null) {
                    CoreError error = CoreError.of(
                                    CoreErrorCode.COMMAND_SUGGESTIONS_FAILED,
                                    CoreErrorSeverity.ERROR,
                                    "enumClass.getEnumConstants() returned null for suggestions"
                            )
                            .withContextEntry("enumClass", enumClass.getName());
                    LOG.error(error.toLogString());
                    return builder.buildFuture();
                }

                for (E value : constants) {
                    builder.suggest(value.name().toLowerCase(Locale.ROOT));
                }
            } catch (Exception e) {
                CoreError error = new CoreError(
                        CoreErrorCode.COMMAND_SUGGESTIONS_FAILED,
                        CoreErrorSeverity.ERROR,
                        "Failed to build enum suggestions",
                        e,
                        Map.of("enumClass", enumClass.getName())
                );
                LOG.error(error.toLogString(), e);
            }
            return builder.buildFuture();
        };
    }

    public static SuggestionProvider<ServerCommandSource> corePermissions() {
        return corePermissions(perm -> true);
    }

    public static SuggestionProvider<ServerCommandSource> corePermissions(Predicate<CorePermission> filter) {
        return (ctx, builder) -> {
            try {
                for (CorePermission perm : CorePermission.values()) {
                    if (filter.test(perm)) {
                        builder.suggest(perm.key());
                    }
                }
            } catch (Exception e) {
                CoreError error = new CoreError(
                        CoreErrorCode.COMMAND_SUGGESTIONS_FAILED,
                        CoreErrorSeverity.ERROR,
                        "Failed to build CorePermission suggestions",
                        e,
                        Map.of("filter", filter.toString())
                );
                LOG.error(error.toLogString(), e);
            }
            return builder.buildFuture();
        };
    }

    private static CompletableFuture<Suggestions> suggestMatching(Collection<String> options,
                                                                  SuggestionsBuilder builder) {
        String remaining = builder.getRemaining().toLowerCase(Locale.ROOT);

        for (String option : options) {
            if (option.toLowerCase(Locale.ROOT).startsWith(remaining)) {
                builder.suggest(option);
            }
        }

        return builder.buildFuture();
    }

    public static SuggestionProvider<ServerCommandSource> onlinePlayers() {
        return (ctx, builder) -> {
            try {
                ctx.getSource().getServer().getPlayerManager().getPlayerList().forEach(player ->
                        builder.suggest(player.getGameProfile().getName())
                );
            } catch (Exception e) {
                CoreError error = new CoreError(
                        CoreErrorCode.COMMAND_SUGGESTIONS_FAILED,
                        CoreErrorSeverity.ERROR,
                        "Failed to build online player suggestions",
                        e,
                        Map.of()
                );
                LOG.error(error.toLogString(), e);
            }
            return builder.buildFuture();
        };
    }

    public static SuggestionProvider<ServerCommandSource> uuids(Collection<java.util.UUID> options) {
        return (ctx, builder) -> suggestMatching(
                options.stream().map(java.util.UUID::toString).toList(),
                builder
        );
    }

    public static SuggestionProvider<ServerCommandSource> fromSupplier(java.util.function.Supplier<Collection<String>> supplier) {
        return (ctx, builder) -> {
            try {
                return suggestMatching(supplier.get(), builder);
            } catch (Exception e) {
                CoreError error = new CoreError(
                        CoreErrorCode.COMMAND_SUGGESTIONS_FAILED,
                        CoreErrorSeverity.ERROR,
                        "Failed to build supplier-based suggestions",
                        e,
                        Map.of("supplier", supplier.getClass().getName())
                );
                LOG.error(error.toLogString(), e);
                return builder.buildFuture();
            }
        };
    }
}
