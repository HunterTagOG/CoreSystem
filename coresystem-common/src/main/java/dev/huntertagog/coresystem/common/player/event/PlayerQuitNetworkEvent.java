package dev.huntertagog.coresystem.common.player.event;

import dev.huntertagog.coresystem.common.event.AbstractDomainEvent;
import dev.huntertagog.coresystem.platform.player.PlayerProfile;

import java.util.UUID;

/**
 * Wird ausgelöst, wenn ein Spieler das Netzwerk verlässt.
 */
public final class PlayerQuitNetworkEvent extends AbstractDomainEvent {

    private final PlayerProfile profile;

    public PlayerQuitNetworkEvent(PlayerProfile profile) {
        super();
        this.profile = profile;
    }

    public PlayerQuitNetworkEvent(PlayerProfile profile, UUID correlationId) {
        super(correlationId);
        this.profile = profile;
    }

    public PlayerProfile profile() {
        return profile;
    }

    @Override
    public String eventType() {
        return "player.quit";
    }
}
