package dev.huntertagog.coresystem.client.region;

import dev.huntertagog.coresystem.common.region.RegionImageFacing;
import dev.huntertagog.coresystem.common.region.RegionImageMode;
import dev.huntertagog.coresystem.fabric.common.net.payload.RegionImageSetS2CPayload;
import dev.huntertagog.coresystem.fabric.common.region.RegionImageDef;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Box;

import java.util.Locale;

public final class RegionImageClientMapper {

    private RegionImageClientMapper() {
    }

    public static RegionImageDef fromPayload(RegionImageSetS2CPayload p) {
        var bounds = new Box(
                p.minX(), p.minY(), p.minZ(),
                p.maxX(), p.maxY(), p.maxZ()
        );

        return new RegionImageDef(
                p.regionId(),
                p.worldId(),
                bounds,
                RegionImageMode.valueOf(p.mode().toUpperCase()),
                RegionImageFacing.from(p.facing()),
                p.baseY(),
                p.textureId()
        );
    }

    public static Identifier textureIdentifier(String textureId) {
        // textureId MUSS z.B. "portal_1" sein
        String safe = textureId
                .toLowerCase(Locale.ROOT)
                .replace(".png", "")
                .replaceAll("[^a-z0-9/._-]", "_");

        return Identifier.of(
                "coresystem",
                "textures/region_images/" + safe + ".png"
        );
    }

}
