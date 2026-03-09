package dev.huntertagog.coresystem.common.islands;

import java.util.UUID;

public final class PrivateIslandWorldRedisProtocol {

    private PrivateIslandWorldRedisProtocol() {
    }

    public static String requestChannel(String nodeId) {
        return "coresystem:islands:prepare:" + nodeId;
    }

    public static String responseChannel(UUID requestId) {
        return "coresystem:islands:prepare:resp:" + requestId;
    }

    public static String formatRequest(UUID requestId, UUID ownerId) {
        return requestId + ";" + ownerId;
    }

    public static String formatOkResponse(UUID requestId, UUID ownerId) {
        return "OK;" + requestId + ";" + ownerId;
    }

    public static String formatErrResponse(UUID requestId, UUID ownerId, String error) {
        return "ERR;" + requestId + ";" + ownerId + ";" + error;
    }

    public static String responseChannelPattern() {
        return "coresystem:islands:prepare:resp:*";
    }
}
