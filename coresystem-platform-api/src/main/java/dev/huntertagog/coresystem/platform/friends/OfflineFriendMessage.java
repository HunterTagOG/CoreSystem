package dev.huntertagog.coresystem.platform.friends;

import java.util.UUID;

public record OfflineFriendMessage(
        UUID to,
        UUID senderId,
        String senderName,
        String message,
        long sentAtEpochMillis
) {
    public static OfflineFriendMessage of(UUID to, UUID senderId, String senderName, String message) {
        return new OfflineFriendMessage(to, senderId, senderName, message, System.currentTimeMillis());
    }
}
