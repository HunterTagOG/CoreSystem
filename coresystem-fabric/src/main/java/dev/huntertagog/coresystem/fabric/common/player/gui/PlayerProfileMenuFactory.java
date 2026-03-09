package dev.huntertagog.coresystem.fabric.common.player.gui;

import dev.huntertagog.coresystem.fabric.common.gui.GuiBuilder;
import dev.huntertagog.coresystem.fabric.common.gui.GuiItem;
import dev.huntertagog.coresystem.fabric.common.item.ItemBuilder;
import dev.huntertagog.coresystem.fabric.common.player.PlayerContext;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public final class PlayerProfileMenuFactory {

    private static final DateTimeFormatter DATE_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss")
                    .withZone(ZoneId.systemDefault());

    private PlayerProfileMenuFactory() {
    }

    public static void openProfile(ServerPlayerEntity player, PlayerContext ctx) {
        var profile = ctx.profile();

        Text title = Text.literal("Dein Profil");

        // Kopf-Item mit Basisdaten
        var headItem = ItemBuilder.of(Items.PLAYER_HEAD)
                .name(Text.literal("§a" + profile.getName()))
                .loreLine(Text.literal("§7UUID: §f" + profile.getUniqueId()))
                .loreLine(Text.literal("§7Erster Login: §f" + formatTimestamp(profile.getFirstSeenAt())))
                .loreLine(Text.literal("§7Letzter Login: §f" + formatTimestamp(profile.getLastSeenAt())))
                .loreLine(Text.literal("§7Joins gesamt: §f" + profile.getTotalJoins()))
                .loreLine(Text.literal("§7Letzter Server: §f" + profile.getLastServer()))
                .loreLine(Text.literal("§7Letzter Node: §f" + profile.getLastNodeId()))
                .build();

        // Placeholder-Glass
        var filler = GuiItem.of(
                ItemBuilder.of(Items.GRAY_STAINED_GLASS_PANE)
                        .name(Text.literal(" "))
                        .build()
        );

        GuiBuilder builder = GuiBuilder.chestRows(3)
                .title(title)
                .fillAll(filler)
                .item(13, GuiItem.of(headItem)); // mittlerer Slot (3x9 -> Index 13)

        builder.open(player);
    }

    private static String formatTimestamp(long millis) {
        if (millis <= 0L) return "-";
        return DATE_TIME_FORMATTER.format(Instant.ofEpochMilli(millis));
    }
}
