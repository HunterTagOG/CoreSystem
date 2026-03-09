package dev.huntertagog.coresystem.fabric.common.region;

import dev.huntertagog.coresystem.common.region.RegionImageFacing;
import dev.huntertagog.coresystem.common.region.RegionImageMode;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Box;

public record RegionImageDef(
        String regionId,
        Identifier worldId,

        Box bounds,                 // Weltkoordinaten
        RegionImageMode mode,       // WALL / FLOOR
        RegionImageFacing facing,   // NORTH/SOUTH/EAST/WEST
        int baseY,

        String textureId        // Client-Texture (NativeImageBackedTexture)
) {
}
