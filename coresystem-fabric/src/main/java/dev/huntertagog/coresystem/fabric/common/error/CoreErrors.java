package dev.huntertagog.coresystem.fabric.common.error;

import dev.huntertagog.coresystem.common.error.CoreError;
import dev.huntertagog.coresystem.common.error.CoreErrorCode;
import dev.huntertagog.coresystem.common.error.CoreErrorSeverity;
import dev.huntertagog.coresystem.common.error.CoreException;
import dev.huntertagog.coresystem.common.log.Logger;
import dev.huntertagog.coresystem.common.log.LoggerFactory;
import dev.huntertagog.coresystem.common.provider.ServiceProvider;
import dev.huntertagog.coresystem.platform.event.DomainEvent;
import dev.huntertagog.coresystem.platform.event.DomainEventBus;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public final class CoreErrors {

    private static final Logger LOG = LoggerFactory.get("CoreError");
    private static final DomainEventBus EVENT_BUS =
            ServiceProvider.getService(DomainEventBus.class);

    private CoreErrors() {
    }

    public static CoreException fail(CoreErrorCode code,
                                     CoreErrorSeverity severity,
                                     String msg) {
        CoreError error = CoreError.of(code, severity, msg);
        log(error, null);
        publish(error);
        return new CoreException(error);
    }

    public static CoreException fail(CoreErrorCode code,
                                     CoreErrorSeverity severity,
                                     String msg,
                                     Throwable cause) {
        CoreError error = CoreError.of(code, severity, msg).withCause(cause);
        log(error, null);
        publish(error);
        return new CoreException(error);
    }

    public static void log(CoreError error, @Nullable Logger customLogger) {
        Logger logger = customLogger != null ? customLogger : LOG;

        switch (error.severity()) {
            case INFO -> logger.info(error.toLogString());
            case WARN -> logger.warn(error.toLogString());
            case ERROR, CRITICAL -> {
                if (error.cause() != null) {
                    logger.error(error.toLogString(), error.cause());
                } else {
                    logger.error(error.toLogString());
                }
            }
        }
    }

    public static void publish(CoreError error) {
        if (EVENT_BUS == null) return;

        EVENT_BUS.publishAsync(new CoreErrorEvent(error));
    }

    /**
     * Optional: generische Rückmeldung an Commands/Player ohne i18n-Bindung.
     * Für produktive Pfade lieber Messages + CoreMessage verwenden.
     */
    public static void sendToSource(ServerCommandSource source,
                                    CoreError error,
                                    boolean includeCode) {
        String base = "Ein interner Fehler ist aufgetreten.";
        if (includeCode) {
            base += " (" + error.code().name() + ")";
        }
        source.sendError(Text.literal(base));
    }

    public static void sendToPlayer(ServerPlayerEntity player,
                                    CoreError error,
                                    boolean includeCode) {
        String base = "Es ist ein Fehler aufgetreten.";
        if (includeCode) {
            base += " (" + error.code().name() + ")";
        }
        player.sendMessage(Text.literal(base), false);
    }

    // DomainEvent-Wrapper
    public record CoreErrorEvent(CoreError error) implements DomainEvent {

        @Override
        public @NotNull String toString() {
            return "CoreErrorEvent{" + error.toLogString() + '}';
        }

        @Override
        public long occurredAt() {
            return System.currentTimeMillis();
        }

        @Override
        public UUID correlationId() {
            return UUID.randomUUID();
        }
    }
}
