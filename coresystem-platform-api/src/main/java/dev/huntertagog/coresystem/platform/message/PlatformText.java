package dev.huntertagog.coresystem.platform.message;

import java.util.Objects;

public final class PlatformText {

    private final String plain;

    private PlatformText(String plain) {
        this.plain = Objects.requireNonNull(plain, "plain");
    }

    public String plain() {
        return plain;
    }

    public static PlatformText of(String plain) {
        return new PlatformText(plain);
    }
}
