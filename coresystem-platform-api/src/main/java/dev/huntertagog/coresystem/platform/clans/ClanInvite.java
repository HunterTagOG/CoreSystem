package dev.huntertagog.coresystem.platform.clans;

import java.util.UUID;

public final class ClanInvite {

    private final UUID clanId;
    private final String clanName;
    private final String clanTag;
    private final UUID invitedPlayerId = null;
    private final UUID inviterId = null;
    private final String inviterName = null;
    private final long createdAtEpochMillis;

    public ClanInvite(
            UUID clanId,
            String clanName,
            String clanTag
    ) {
        this.clanId = clanId;
        this.clanName = clanName;
        this.clanTag = clanTag;
        this.createdAtEpochMillis = System.currentTimeMillis();
    }

    public UUID getClanId() {
        return clanId;
    }

    public String getClanName() {
        return clanName;
    }

    public String getClanTag() {
        return clanTag;
    }

    public UUID getInvitedPlayerId() {
        return invitedPlayerId;
    }

    public UUID getInviterId() {
        return inviterId;
    }

    public String getInviterName() {
        return inviterName;
    }

    public long getCreatedAtEpochMillis() {
        return createdAtEpochMillis;
    }
}
