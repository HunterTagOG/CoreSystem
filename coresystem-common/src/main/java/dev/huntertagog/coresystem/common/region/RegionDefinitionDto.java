package dev.huntertagog.coresystem.common.region;

import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Flaches Transportobjekt für Redis-Persistenz.
 */
public record RegionDefinitionDto(
        String id,
        String name,
        UUID ownerId,
        String world, // Identifier als String (namespace:path)
        int minX, int minY, int minZ,
        int maxX, int maxY, int maxZ,
        Set<String> flags,            // RegionFlag.name()
        List<String> onEnterCommands,
        List<String> onLeaveCommands
) {
}
