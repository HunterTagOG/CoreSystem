package dev.huntertagog.coresystem.common.region;

public enum RegionImageFacing {
    NORTH,
    SOUTH,
    EAST,
    WEST;

    public static RegionImageFacing from(String facing) {
        try {
            return RegionImageFacing.valueOf(facing.toUpperCase());
        } catch (Exception e) {
            return NORTH;
        }
    }
}
