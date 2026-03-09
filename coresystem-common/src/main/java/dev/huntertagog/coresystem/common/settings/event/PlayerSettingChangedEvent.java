package dev.huntertagog.coresystem.common.settings.event;


import dev.huntertagog.coresystem.common.event.AbstractDomainEvent;

import java.util.UUID;

public final class PlayerSettingChangedEvent extends AbstractDomainEvent {

    private final UUID playerId;
    private final String settingKey;
    private final String oldValue;
    private final String newValue;

    public PlayerSettingChangedEvent(UUID playerId,
                                     String settingKey,
                                     String oldValue,
                                     String newValue) {
        this.playerId = playerId;
        this.settingKey = settingKey;
        this.oldValue = oldValue;
        this.newValue = newValue;
    }

    public UUID playerId() {
        return playerId;
    }

    public String settingKey() {
        return settingKey;
    }

    public String oldValue() {
        return oldValue;
    }

    public String newValue() {
        return newValue;
    }

    @Override
    public String eventType() {
        return "player.setting.changed";
    }
}
