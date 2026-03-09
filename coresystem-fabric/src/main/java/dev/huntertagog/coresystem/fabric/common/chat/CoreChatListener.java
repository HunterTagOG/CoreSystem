package dev.huntertagog.coresystem.fabric.common.chat;

import dev.huntertagog.coresystem.common.log.Logger;
import dev.huntertagog.coresystem.common.log.LoggerFactory;
import dev.huntertagog.coresystem.common.permission.CorePermission;
import dev.huntertagog.coresystem.common.provider.ServiceProvider;
import dev.huntertagog.coresystem.fabric.common.permission.PermissionService;
import dev.huntertagog.coresystem.platform.chat.ChatFilterService;
import dev.huntertagog.coresystem.platform.clans.Clan;
import dev.huntertagog.coresystem.platform.clans.ClanService;
import dev.huntertagog.coresystem.platform.clans.chat.ClanChatService;
import net.fabricmc.fabric.api.message.v1.ServerMessageEvents;
import net.minecraft.network.message.MessageType;
import net.minecraft.network.message.SignedMessage;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.Optional;
import java.util.UUID;

public final class CoreChatListener {

    private static final Logger LOG = LoggerFactory.get("CoreChat");

    private static ChatDisplayNameFormatter formatter;
    private static ChatFilterService chatFilter;
    private static PermissionService permissionService;

    private static ClanService clanService;
    private static ClanChatService clanChatService;

    private CoreChatListener() {
    }

    public static void register(MinecraftServer server) {
        formatter = new ChatDisplayNameFormatter();
        chatFilter = ServiceProvider.getService(ChatFilterService.class);
        permissionService = ServiceProvider.getService(PermissionService.class);

        clanService = ServiceProvider.getService(ClanService.class);
        clanChatService = ServiceProvider.getService(ClanChatService.class);

        // Wir übernehmen das gesamte Chat-Handover
        ServerMessageEvents.ALLOW_CHAT_MESSAGE.register(
                (SignedMessage signedMessage,
                 ServerPlayerEntity sender,
                 MessageType.Parameters params) -> {

                    // 1) DisplayName (Prefix + Nick des Senders)
                    Text displayName = formatter.buildDisplayName(sender);

                    // 2) Roh-Content aus SignedMessage
                    String rawContent = signedMessage.getContent().getString();

                    // 3) Gefilterter Content (für "normale" Spieler)
                    String filteredContent = (chatFilter != null)
                            ? chatFilter.filter(rawContent)
                            : rawContent;

                    // 4) ClanChat-Zustand / Clan des Senders
                    boolean senderClanChat = clanChatService != null && clanChatService.isClanChatEnabled(sender.getUuid());
                    Optional<Clan> finalSenderClanOpt = (clanService != null)
                            ? clanService.findByMember(sender.getUuid())
                            : Optional.empty();

                    // Fallback: ClanChat an, aber kein Clan -> automatisch deaktivieren
                    if (senderClanChat && finalSenderClanOpt.isEmpty()) {
                        if (clanChatService != null) {
                            clanChatService.setClanChatEnabled(sender.getUuid(), false);
                        }
                        senderClanChat = false;
                    }

                    // 5) Basiszeile für Logging / Server-Konsole (immer ungefiltert)
                    MutableText baseLogLine = buildChatLine(displayName, rawContent);

                    boolean finalSenderClanChat = senderClanChat;

                    sender.server.getPlayerManager().broadcast(
                            baseLogLine, // fürs Server-Log
                            target -> {
                                // Admin/Staff: Chat Spy darf immer sehen
                                boolean spy = hasSpyPermission(target);

                                // Target ist im ClanChat-only Modus?
                                boolean targetClanChat = clanChatService != null && clanChatService.isClanChatEnabled(target.getUuid());

                                // Bypass: sieht ungefiltert
                                boolean bypassFilter = hasBypassPermission(target);
                                String contentForTarget = bypassFilter ? rawContent : filteredContent;

                                // CASE A) Sender schreibt im Clan-Chat -> nur an Clan (oder Spy)
                                if (finalSenderClanChat) {
                                    if (spy) {
                                        return buildClanChatLine(displayName, contentForTarget, true);
                                    }

                                    if (isSameClan(finalSenderClanOpt.get(), target)) {
                                        return buildClanChatLine(displayName, contentForTarget, false);
                                    }

                                    return null; // nicht zustellen
                                }

                                // CASE B) Sender schreibt im Global-Chat
                                // Empfänger im ClanChat-only Modus bekommt KEIN Global (außer Spy)
                                if (targetClanChat && !spy) {
                                    return null;
                                }

                                // Global normal zustellen
                                return buildChatLine(displayName, contentForTarget);
                            },
                            false
                    );

                    // zusätzlich ins eigene Log schreiben (ungefiltert)
                    LOG.info("[Chat] {}: {}", displayName.getString(), rawContent);

                    // Vanilla-Broadcast (signed) unterdrücken
                    return false;
                });

        LOG.info("CoreChatListener registered (prefix, nick, filter, clan-chat routing, admin bypass/spy).");
    }

    // -------------------------------
    // Chat Line Builder
    // -------------------------------

    private static MutableText buildChatLine(Text displayName, String content) {
        return Text.empty()
                .append(displayName)
                .append(Text.literal(" ").formatted(Formatting.GRAY))
                .append(Text.literal("» ").formatted(Formatting.DARK_GRAY))
                .append(Text.literal(content).formatted(Formatting.WHITE));
    }

    private static MutableText buildClanChatLine(Text displayName, String content, boolean spyView) {
        // Format: [Clan] <name> » msg  (Spy sieht zusätzlich Markierung)
        MutableText prefix = Text.literal("[Clan] ").formatted(Formatting.DARK_AQUA);
        if (spyView) {
            prefix = prefix.append(Text.literal("[Spy] ").formatted(Formatting.DARK_GRAY));
        }

        return Text.empty()
                .append(prefix)
                .append(displayName)
                .append(Text.literal(" ").formatted(Formatting.GRAY))
                .append(Text.literal("» ").formatted(Formatting.DARK_GRAY))
                .append(Text.literal(content).formatted(Formatting.WHITE));
    }

    // -------------------------------
    // Permissions
    // -------------------------------

    private static boolean hasBypassPermission(ServerPlayerEntity target) {
        if (permissionService == null) return false;
        try {
            return permissionService.has(target.getCommandSource(), CorePermission.STAFF_CHAT_FILTER_BYPASS);
        } catch (Exception e) {
            LOG.warn("Failed to evaluate chat filter bypass permission for {}", target.getGameProfile().getName(), e);
            return false;
        }
    }

    private static boolean hasSpyPermission(ServerPlayerEntity target) {
        if (permissionService == null) return false;
        try {
            // -> Diese Permission musst du anlegen (CorePermission.STAFF_CHAT_SPY)
            return permissionService.has(target.getCommandSource(), CorePermission.STAFF_CHAT_SPY);
        } catch (Exception e) {
            LOG.warn("Failed to evaluate chat spy permission for {}", target.getGameProfile().getName(), e);
            return false;
        }
    }

    // -------------------------------
    // Clan helper
    // -------------------------------

    private static boolean isSameClan(Clan senderClan, ServerPlayerEntity target) {
        if (clanService == null) return false;

        UUID tid = target.getUuid();
        return clanService.findByMember(tid)
                .map(c -> c.id().equals(senderClan.id()))
                .orElse(false);
    }
}
