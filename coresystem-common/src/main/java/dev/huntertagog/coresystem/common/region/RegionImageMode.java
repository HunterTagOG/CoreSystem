package dev.huntertagog.coresystem.common.region;

public enum RegionImageMode {
    WALL,
    FLOOR;

    public static RegionImageMode parse(String raw) {
        if (raw == null) return WALL;
        return switch (raw.toLowerCase(java.util.Locale.ROOT)) {
            case "wall", "wand" -> WALL;
            case "floor", "boden" -> FLOOR;
            default -> throw new IllegalArgumentException("Unknown mode: " + raw);
        };
    }
}
