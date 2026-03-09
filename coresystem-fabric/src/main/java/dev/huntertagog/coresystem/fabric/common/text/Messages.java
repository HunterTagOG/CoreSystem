package dev.huntertagog.coresystem.fabric.common.text;

import dev.huntertagog.coresystem.common.error.CoreError;
import dev.huntertagog.coresystem.common.error.CoreErrorCode;
import dev.huntertagog.coresystem.common.error.CoreErrorSeverity;
import dev.huntertagog.coresystem.common.text.CoreMessage;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.function.Supplier;

public final class Messages {

    private static final Text PREFIX = Text.literal("[Cobblecrew] ")
            .formatted(Formatting.DARK_GRAY);

    private Messages() {
    }

    // ------------------------------------------------------------------
    // Text-Erzeugung
    // ------------------------------------------------------------------

    public static Text t(CoreMessage msg, Object... args) {
        return Text.translatable(msg.key(), args);
    }

    public static Text tp(CoreMessage msg, Object... args) {
        return PREFIX.copy().append(t(msg, args));
    }

    // ------------------------------------------------------------------
    // Senden an CommandSource
    // ------------------------------------------------------------------

    public static void send(ServerCommandSource source, CoreMessage msg, Object... args) {
        send(source, false, msg, args);
    }

    public static void send(ServerCommandSource source,
                            boolean broadcastToOps,
                            CoreMessage msg,
                            Object... args) {

        Supplier<Text> supplier = () -> tp(msg, args);

        try {
            source.sendFeedback(supplier, broadcastToOps);
        } catch (Exception e) {
            CoreError.of(
                            CoreErrorCode.MESSAGE_ERROR,
                            CoreErrorSeverity.ERROR,
                            "Failed to send CoreMessage to ServerCommandSource"
                    )
                    .withCause(e)
                    .withContextEntry("msgKey", msg.key())
                    .withContextEntry("broadcastToOps", broadcastToOps)
                    .log();
        }
    }

    // ------------------------------------------------------------------
    // Direkt an Spieler
    // ------------------------------------------------------------------

    public static void send(ServerPlayerEntity player, CoreMessage msg, Object... args) {
        try {
            player.sendMessage(tp(msg, args));
        } catch (Exception e) {
            CoreError.of(
                            CoreErrorCode.MESSAGE_ERROR,
                            CoreErrorSeverity.ERROR,
                            "Failed to send CoreMessage to player"
                    )
                    .withCause(e)
                    .withContextEntry("msgKey", msg.key())
                    .withContextEntry("playerUuid", player.getUuid().toString())
                    .withContextEntry("playerName", player.getGameProfile().getName())
                    .log();
        }
    }

    public static void sendRaw(ServerPlayerEntity player, CoreMessage msg, Object... args) {
        try {
            player.sendMessage(t(msg, args));
        } catch (Exception e) {
            CoreError.of(
                            CoreErrorCode.MESSAGE_ERROR,
                            CoreErrorSeverity.ERROR,
                            "Failed to send raw CoreMessage to player"
                    )
                    .withCause(e)
                    .withContextEntry("msgKey", msg.key())
                    .withContextEntry("playerUuid", player.getUuid().toString())
                    .withContextEntry("playerName", player.getGameProfile().getName())
                    .log();
        }
    }

    public static Text literal(String txt) {
        return Text.literal(txt);
    }

}
