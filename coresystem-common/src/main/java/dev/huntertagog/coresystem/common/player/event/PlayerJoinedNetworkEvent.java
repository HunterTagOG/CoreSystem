package dev.huntertagog.coresystem.common.player.event;

import dev.huntertagog.coresystem.common.event.AbstractDomainEvent;
import dev.huntertagog.coresystem.platform.player.PlayerProfile;

import java.util.UUID;

/**
 * Wird ausgelöst, wenn ein Spieler einem beliebigen Server im Netzwerk joint.
 */
public final class PlayerJoinedNetworkEvent extends AbstractDomainEvent {

    private final PlayerProfile profile;

    public PlayerJoinedNetworkEvent(PlayerProfile profile) {
        super();
        this.profile = profile;
    }

    public PlayerJoinedNetworkEvent(PlayerProfile profile, UUID correlationId) {
        super(correlationId);
        this.profile = profile;
    }

    public PlayerProfile profile() {
        return profile;
    }

    @Override
    public String eventType() {
        return "player.joined";
    }
}
