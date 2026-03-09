package dev.huntertagog.coresystem.fabric.common.error;

import dev.huntertagog.coresystem.common.error.CoreError;
import dev.huntertagog.coresystem.common.log.Logger;
import dev.huntertagog.coresystem.common.log.LoggerFactory;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public final class CoreErrorUtil {

    private static final Logger LOG = LoggerFactory.get("CoreError");

    private CoreErrorUtil() {
    }

    /**
     * Sendet CoreError an Server/Spieler und loggt ihn sauber.
     */
    public static void notify(ServerCommandSource src, CoreError error) {
        // 1) Logging auf Server-Seite
        if (error.cause() != null) {
            LOG.error(error.toLogString(), error.cause());
        } else {
            LOG.error(error.toLogString());
        }

        // 2) Spieler-Feedback
        // → technische Message NICHT ungefiltert zeigen
        // → aber ADMINS können optional zusätzliche Details sehen
        src.sendError(
                Text.literal("[" + error.code() + "] ")
                        .append(error.technicalMessage())
                        .formatted(Formatting.RED)
        );

        // Optional: Admin bekommt zusätzliche Debug-Infos
        if (Permissions.check(src, "coresystem.debug.error", false)) {
            src.sendFeedback(
                    () -> Text.literal("Context: " + error.context()).formatted(Formatting.DARK_GRAY),
                    false
            );
        }
    }

    public static void notifyServiceMissing(ServerCommandSource src, String clanService, String clanCreate) {
        src.sendError(
                Text.literal("Der Dienst \"" + clanService + "\" ist nicht verfügbar. " +
                                "Die Aktion \"" + clanCreate + "\" kann daher nicht ausgeführt werden.")
                        .formatted(Formatting.RED)
        );
    }
}
