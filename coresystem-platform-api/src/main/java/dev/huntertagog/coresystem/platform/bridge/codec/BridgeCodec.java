package dev.huntertagog.coresystem.platform.bridge.codec;

import dev.huntertagog.coresystem.platform.bridge.BridgeMessageType;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

public final class BridgeCodec {

    private BridgeCodec() {
    }

    // ========= TELEPORT REQUEST =========
    // payload: [type][playerUuid(16)][targetServer][reason][context]
    public static byte[] encodeTeleportRequest(
            UUID requesterId,
            UUID playerId,
            String targetServerName,
            String reason,
            String inventoryContext,
            String replyToNodeId
    ) {
        byte[] targetBytes = safe(targetServerName).getBytes(StandardCharsets.UTF_8);
        byte[] reasonBytes = safe(reason).getBytes(StandardCharsets.UTF_8);
        byte[] ctxBytes = safe(inventoryContext).getBytes(StandardCharsets.UTF_8);

        ByteBuffer buf = ByteBuffer.allocate(
                1 + 16 +
                        4 + targetBytes.length +
                        4 + reasonBytes.length +
                        4 + ctxBytes.length
        );

        buf.put(BridgeMessageType.TELEPORT_REQUEST.id);
        putUuid(buf, playerId);
        putString(buf, targetBytes);
        putString(buf, reasonBytes);
        putString(buf, ctxBytes);

        return buf.array();
    }

    // ========= REQUEST =========
    public static byte[] encodePrepareRequest(
            UUID requestId,
            UUID ownerId,
            String targetNodeId,
            String replyToNodeId
    ) {
        byte[] targetBytes = targetNodeId.getBytes(StandardCharsets.UTF_8);
        byte[] replyBytes = replyToNodeId.getBytes(StandardCharsets.UTF_8);

        ByteBuffer buf = ByteBuffer.allocate(
                1 +                 // type
                        16 +                // requestId
                        16 +                // ownerId
                        4 + targetBytes.length +
                        4 + replyBytes.length
        );

        buf.put(BridgeMessageType.PREPARE_REQUEST.id);
        putUuid(buf, requestId);
        putUuid(buf, ownerId);
        putString(buf, targetBytes);
        putString(buf, replyBytes);
        return buf.array();
    }

    public static PrepareRequest decodePrepareRequest(byte[] data) {
        ByteBuffer buf = ByteBuffer.wrap(data);
        expect(buf, BridgeMessageType.PREPARE_REQUEST);

        UUID requestId = getUuid(buf);
        UUID ownerId = getUuid(buf);
        String target = getString(buf);
        String replyTo = getString(buf);

        return new PrepareRequest(requestId, ownerId, target, replyTo);
    }

    public record PrepareRequest(
            UUID requestId,
            UUID ownerId,
            String targetNodeId,
            String replyToNodeId
    ) {
    }

    // ========= RESPONSE =========
    public static byte[] encodePrepareResponse(
            UUID requestId,
            boolean ok,
            String error,
            String replyToNodeId
    ) {
        byte[] errBytes = (error == null ? "" : error).getBytes(StandardCharsets.UTF_8);
        byte[] replyBytes = replyToNodeId.getBytes(StandardCharsets.UTF_8);

        ByteBuffer buf = ByteBuffer.allocate(
                1 + 16 + 1 +
                        4 + errBytes.length +
                        4 + replyBytes.length
        );

        buf.put(BridgeMessageType.PREPARE_RESPONSE.id);
        putUuid(buf, requestId);
        buf.put((byte) (ok ? 1 : 0));
        putString(buf, errBytes);
        putString(buf, replyBytes);
        return buf.array();
    }

    public static PrepareResponse decodePrepareResponse(byte[] data) {
        ByteBuffer buf = ByteBuffer.wrap(data);
        expect(buf, BridgeMessageType.PREPARE_RESPONSE);

        UUID requestId = getUuid(buf);
        boolean ok = buf.get() == 1;
        String error = getString(buf);
        String replyTo = getString(buf);

        if (error.isBlank()) error = null;
        return new PrepareResponse(requestId, ok, error, replyTo);
    }

    public record PrepareResponse(
            UUID requestId,
            boolean ok,
            String error,
            String replyToNodeId
    ) {
    }

    public static TeleportRequest decodeTeleportRequest(byte[] data) {
        ByteBuffer buf = ByteBuffer.wrap(data);
        expect(buf, BridgeMessageType.TELEPORT_REQUEST);

        UUID playerId = getUuid(buf);
        String target = getString(buf);
        String reason = getString(buf);
        String ctx = getString(buf);

        if (reason.isBlank()) reason = null;
        if (ctx.isBlank()) ctx = null;

        return new TeleportRequest(playerId, target, reason, ctx);
    }

    public record TeleportRequest(
            UUID playerId,
            String targetServerName,
            String reason,
            String inventoryContext
    ) {
    }

    // ========= helpers =========
    private static void putUuid(ByteBuffer buf, UUID u) {
        buf.putLong(u.getMostSignificantBits());
        buf.putLong(u.getLeastSignificantBits());
    }

    private static UUID getUuid(ByteBuffer buf) {
        return new UUID(buf.getLong(), buf.getLong());
    }

    private static void putString(ByteBuffer buf, byte[] b) {
        buf.putInt(b.length);
        buf.put(b);
    }

    private static String getString(ByteBuffer buf) {
        int len = buf.getInt();
        byte[] b = new byte[len];
        buf.get(b);
        return new String(b, StandardCharsets.UTF_8);
    }

    private static void expect(ByteBuffer buf, BridgeMessageType type) {
        BridgeMessageType t = BridgeMessageType.fromId(buf.get());
        if (t != type) throw new IllegalArgumentException("Expected " + type);
    }

    // ========= DELETE REQUEST =========
    public static byte[] encodeIslandDeleteRequest(
            UUID requestId,
            UUID ownerId,
            String targetNodeId,
            String replyToNodeId
    ) {
        byte[] targetBytes = targetNodeId.getBytes(StandardCharsets.UTF_8);
        byte[] replyBytes = replyToNodeId.getBytes(StandardCharsets.UTF_8);

        ByteBuffer buf = ByteBuffer.allocate(
                1 + 16 + 16 +
                        4 + targetBytes.length +
                        4 + replyBytes.length
        );

        buf.put(BridgeMessageType.ISLAND_DELETE_REQUEST.id);
        putUuid(buf, requestId);
        putUuid(buf, ownerId);
        putString(buf, targetBytes);
        putString(buf, replyBytes);
        return buf.array();
    }

    public static IslandDeleteRequest decodeIslandDeleteRequest(byte[] data) {
        ByteBuffer buf = ByteBuffer.wrap(data);
        expect(buf, BridgeMessageType.ISLAND_DELETE_REQUEST);

        UUID requestId = getUuid(buf);
        UUID ownerId = getUuid(buf);
        String target = getString(buf);
        String replyTo = getString(buf);

        return new IslandDeleteRequest(requestId, ownerId, target, replyTo);
    }

    public record IslandDeleteRequest(
            UUID requestId,
            UUID ownerId,
            String targetNodeId,
            String replyToNodeId
    ) {
    }

    // ========= DELETE RESPONSE =========
    public static byte[] encodeIslandDeleteResponse(
            UUID requestId,
            boolean ok,
            String error,
            String replyToNodeId
    ) {
        byte[] errBytes = (error == null ? "" : error).getBytes(StandardCharsets.UTF_8);
        byte[] replyBytes = replyToNodeId.getBytes(StandardCharsets.UTF_8);

        ByteBuffer buf = ByteBuffer.allocate(
                1 + 16 + 1 +
                        4 + errBytes.length +
                        4 + replyBytes.length
        );

        buf.put(BridgeMessageType.ISLAND_DELETE_RESPONSE.id);
        putUuid(buf, requestId);
        buf.put((byte) (ok ? 1 : 0));
        putString(buf, errBytes);
        putString(buf, replyBytes);
        return buf.array();
    }

    public static IslandDeleteResponse decodeIslandDeleteResponse(byte[] data) {
        ByteBuffer buf = ByteBuffer.wrap(data);
        expect(buf, BridgeMessageType.ISLAND_DELETE_RESPONSE);

        UUID requestId = getUuid(buf);
        boolean ok = buf.get() == 1;
        String error = getString(buf);
        String replyTo = getString(buf);

        if (error.isBlank()) error = null;
        return new IslandDeleteResponse(requestId, ok, error, replyTo);
    }

    public record IslandDeleteResponse(
            UUID requestId,
            boolean ok,
            String error,
            String replyToNodeId
    ) {
    }

    private static String safe(String s) {
        return s == null ? "" : s;
    }
}
