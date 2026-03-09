package dev.huntertagog.coresystem.platform.friends;

import java.util.UUID;

public record FriendRelation(
        UUID self,
        UUID other,
        FriendRelationStatus status
) {
}
