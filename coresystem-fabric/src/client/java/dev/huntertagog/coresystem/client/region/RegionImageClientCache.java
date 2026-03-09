package dev.huntertagog.coresystem.client.region;

import dev.huntertagog.coresystem.fabric.common.region.RegionImageDef;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class RegionImageClientCache {
    private RegionImageClientCache() {
    }

    private static final Map<String, RegionImageDef> BY_REGION = new ConcurrentHashMap<>();

    public static void upsert(RegionImageDef def) {
        BY_REGION.put(def.regionId(), def);
    }

    public static void remove(String regionId) {
        BY_REGION.remove(regionId);
    }

    public static Map<String, RegionImageDef> snapshot() {
        return Map.copyOf(BY_REGION);
    }

    public static void clear() {
        BY_REGION.clear();
    }
}
