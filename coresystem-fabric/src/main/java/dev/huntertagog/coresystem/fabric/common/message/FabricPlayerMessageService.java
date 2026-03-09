package dev.huntertagog.coresystem.fabric.common.message;

import dev.huntertagog.coresystem.common.error.CoreError;
import dev.huntertagog.coresystem.common.error.CoreErrorCode;
import dev.huntertagog.coresystem.common.error.CoreErrorSeverity;
import dev.huntertagog.coresystem.common.log.Logger;
import dev.huntertagog.coresystem.common.log.LoggerFactory;
import dev.huntertagog.coresystem.platform.message.PlatformText;
import dev.huntertagog.coresystem.platform.message.PlayerMessageService;
import dev.huntertagog.coresystem.platform.message.PlayerNotification;
import net.minecraft.network.packet.s2c.play.SubtitleS2CPacket;
import net.minecraft.network.packet.s2c.play.TitleFadeS2CPacket;
import net.minecraft.network.packet.s2c.play.TitleS2CPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.UUID;

public final class FabricPlayerMessageService implements PlayerMessageService {

    private static final Logger LOG = LoggerFactory.get("PlayerMessageService");

    private final MinecraftServer server;

    public FabricPlayerMessageService(MinecraftServer server) {
        this.server = server;
    }

    @Override
    public void send(UUID playerId, PlayerNotification notification) {
        ServerPlayerEntity player = server.getPlayerManager().getPlayer(playerId);
        if (player == null) return;

        try {
            switch (notification.channel()) {
                case CHAT -> sendChatInternal(player, notification);
                case ACTION_BAR -> sendActionBarInternal(player, notification);
                case TITLE -> sendTitleInternal(player, notification);
                case SUBTITLE -> sendSubtitleInternal(player, notification);
                case SYSTEM -> sendSystemInternal(player, notification);
                case TOAST -> sendToastInternal(player, notification);
            }
        } catch (Exception e) {
            CoreError.of(
                            CoreErrorCode.PLAYER_MESSAGE_SEND_FAILED,
                            CoreErrorSeverity.WARN,
                            "Failed to send player notification"
                    )
                    .withCause(e)
                    .withContextEntry("playerUuid", player.getUuidAsString())
                    .withContextEntry("channel", notification.channel().name())
                    .withContextEntry("severity", notification.severity().name())
                    .withContextEntry("message", toText(notification.message()).getString())
                    .log();

            LOG.warn("Failed to send notification '{}' to player {} ({})",
                    toText(notification.message()).getString(),
                    player.getGameProfile().getName(),
                    player.getUuidAsString());
        }
    }

    // ---------------- Chat ----------------

    private void sendChatInternal(ServerPlayerEntity player, PlayerNotification n) {
        player.sendMessage(applySeverityFormatting(n), false);
    }

    // ---------------- ActionBar ----------------

    private void sendActionBarInternal(ServerPlayerEntity player, PlayerNotification n) {
        player.sendMessage(applySeverityFormatting(n), true);
    }

    // ---------------- Title / Subtitle ----------------

    private void sendTitleInternal(ServerPlayerEntity player, PlayerNotification n) {
        Text title = applySeverityFormatting(n);

        var timings = n.titleTimings().orElse(null);
        if (timings != null) {
            player.networkHandler.sendPacket(new TitleFadeS2CPacket(
                    timings.fadeInTicks(),
                    timings.stayTicks(),
                    timings.fadeOutTicks()
            ));
        }

        player.networkHandler.sendPacket(new TitleS2CPacket(title));

        // optional: subtitle im selben Notification-Objekt
        if (n.subtitle().isPresent()) {
            player.networkHandler.sendPacket(new SubtitleS2CPacket(toText(n.subtitle().get())));
        }
    }

    private void sendSubtitleInternal(ServerPlayerEntity player, PlayerNotification n) {
        Text subtitle = applySeverityFormatting(n);
        player.networkHandler.sendPacket(new SubtitleS2CPacket(subtitle));
    }

    // ---------------- System ----------------

    private void sendSystemInternal(ServerPlayerEntity player, PlayerNotification n) {
        player.sendMessage(applySeverityFormatting(n), false);
    }

    // ---------------- Toast (Server-Fallback) ----------------

    private void sendToastInternal(ServerPlayerEntity player, PlayerNotification n) {
        Text base = toText(n.message());

        Text prefixed = switch (n.severity()) {
            case SUCCESS -> Text.literal("✔ ").formatted(Formatting.GREEN).append(base);
            case WARNING -> Text.literal("⚠ ").formatted(Formatting.YELLOW).append(base);
            case ERROR -> Text.literal("✖ ").formatted(Formatting.RED).append(base);
            case INFO -> Text.literal("• ").formatted(Formatting.GRAY).append(base);
        };

        player.sendMessage(prefixed, false);
    }

    // ---------------- Styling ----------------

    private Text applySeverityFormatting(PlayerNotification n) {
        Text base = toText(n.message());

        return switch (n.severity()) {
            case SUCCESS -> base.copy().formatted(Formatting.GREEN);
            case WARNING -> base.copy().formatted(Formatting.YELLOW);
            case ERROR -> base.copy().formatted(Formatting.RED);
            case INFO -> base.copy().formatted(Formatting.GRAY);
        };
    }

    private Text toText(PlatformText t) {
        // Minimal-Mapper (ausbaubar: MiniMessage/JSON/i18n)
        if (t == null) return Text.empty();
        return Text.literal(t.plain());
    }
}
