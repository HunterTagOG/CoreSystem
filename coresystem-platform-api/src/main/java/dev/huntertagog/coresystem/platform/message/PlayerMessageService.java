package dev.huntertagog.coresystem.platform.message;

import dev.huntertagog.coresystem.platform.provider.Service;

import java.util.UUID;

public interface PlayerMessageService extends Service {

    void send(UUID playerId, PlayerNotification notification);

    // ---------------- Chat ----------------

    default void sendInfoChat(UUID playerId, PlatformText message) {
        send(playerId, PlayerNotification.chatInfo(message));
    }

    default void sendSuccessChat(UUID playerId, PlatformText message) {
        send(playerId, PlayerNotification.chatSuccess(message));
    }

    default void sendWarnChat(UUID playerId, PlatformText message) {
        send(playerId, PlayerNotification.chatWarn(message));
    }

    default void sendErrorChat(UUID playerId, PlatformText message) {
        send(playerId, PlayerNotification.chatError(message));
    }

    // ---------------- ActionBar ----------------

    default void sendInfoActionBar(UUID playerId, PlatformText message) {
        send(playerId, PlayerNotification.actionBarInfo(message));
    }

    default void sendSuccessActionBar(UUID playerId, PlatformText message) {
        send(playerId, PlayerNotification.actionBarSuccess(message));
    }

    default void sendWarnActionBar(UUID playerId, PlatformText message) {
        send(playerId, PlayerNotification.actionBarWarn(message));
    }

    default void sendErrorActionBar(UUID playerId, PlatformText message) {
        send(playerId, PlayerNotification.actionBarError(message));
    }

    // ---------------- Title ----------------

    default void sendTitle(UUID playerId,
                           PlatformText title,
                           PlatformText subtitle,
                           int fadeInTicks,
                           int stayTicks,
                           int fadeOutTicks) {

        send(playerId,
                PlayerNotification.title(
                        PlayerNotification.Severity.INFO,
                        title,
                        subtitle,
                        new PlayerNotification.TitleTimings(
                                fadeInTicks,
                                stayTicks,
                                fadeOutTicks
                        )
                )
        );
    }

    // ---------------- Toast ----------------

    default void sendToastInfo(UUID playerId, PlatformText message) {
        send(playerId, PlayerNotification.toastInfo(message));
    }

    default void sendToastSuccess(UUID playerId, PlatformText message) {
        send(playerId, PlayerNotification.toastSuccess(message));
    }

    default void sendToastWarn(UUID playerId, PlatformText message) {
        send(playerId, PlayerNotification.toastWarn(message));
    }

    default void sendToastError(UUID playerId, PlatformText message) {
        send(playerId, PlayerNotification.toastError(message));
    }
}
