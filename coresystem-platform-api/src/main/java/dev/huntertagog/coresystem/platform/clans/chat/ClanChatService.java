package dev.huntertagog.coresystem.platform.clans.chat;

import dev.huntertagog.coresystem.platform.provider.Service;

import java.util.UUID;

public interface ClanChatService extends Service {

    boolean isClanChatEnabled(UUID playerId);

    void setClanChatEnabled(UUID playerId, boolean enabled);

    default boolean toggle(UUID playerId) {
        boolean now = !isClanChatEnabled(playerId);
        setClanChatEnabled(playerId, now);
        return now;
    }
}
