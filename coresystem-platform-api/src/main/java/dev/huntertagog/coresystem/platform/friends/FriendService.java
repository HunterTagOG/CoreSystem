package dev.huntertagog.coresystem.platform.friends;

import dev.huntertagog.coresystem.platform.provider.Service;

import java.util.List;
import java.util.UUID;

public interface FriendService extends Service {

    // Basis-Beziehung
    FriendRelation getRelation(UUID self, UUID other);

    List<UUID> getFriends(UUID self);

    List<UUID> getIncomingRequests(UUID self);

    List<UUID> getOutgoingRequests(UUID self);

    boolean areFriends(UUID a, UUID b);

    // Requests
    boolean sendRequest(UUID from, UUID to);

    boolean acceptRequest(UUID target, UUID from);

    boolean denyRequest(UUID target, UUID from);

    boolean removeFriend(UUID a, UUID b);

    // Offline Messages
    void sendOfflineMessage(OfflineFriendMessage message);

    /**
     * Holt alle ausstehenden Nachrichten und löscht sie aus der Queue.
     */
    List<OfflineFriendMessage> pollOfflineMessages(UUID target);
}
