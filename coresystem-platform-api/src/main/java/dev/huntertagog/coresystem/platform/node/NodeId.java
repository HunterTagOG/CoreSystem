package dev.huntertagog.coresystem.platform.node;

import java.nio.charset.Charset;
import java.util.Objects;

public record NodeId(String value) {
    public NodeId {
        Objects.requireNonNull(value, "nodeId");
        if (value.isBlank()) throw new IllegalArgumentException("nodeId is blank");
    }

    @Override
    public String toString() {
        return value;
    }

    public static NodeId nodeIdFromEnv() {
        String v = System.getenv("SERVER_ID");
        if (v == null || v.isBlank()) {
            throw new IllegalStateException("Missing env SERVER_ID");
        }
        return new NodeId(v.trim());
    }

    public static String nodeNameFromEnv() {
        String v = System.getenv("SERVER_NAME");
        if (v == null || v.isBlank()) {
            throw new IllegalStateException("Missing env SERVER_NAME");
        }
        return v;
    }

    public byte[] getBytes(Charset utf8) {
        return value.getBytes(utf8);
    }
}
