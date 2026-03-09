package dev.huntertagog.coresystem.platform.bridge;

public enum BridgeMessageType {
    PREPARE_REQUEST((byte) 1),
    PREPARE_RESPONSE((byte) 2),
    ISLAND_DELETE_REQUEST((byte) 3),
    ISLAND_DELETE_RESPONSE((byte) 4),

    TELEPORT_REQUEST((byte) 10);

    public final byte id;

    BridgeMessageType(byte id) {
        this.id = id;
    }

    public static BridgeMessageType fromId(byte id) {
        for (var t : values()) if (t.id == id) return t;
        throw new IllegalArgumentException("Unknown BridgeMessageType id=" + id);
    }
}
