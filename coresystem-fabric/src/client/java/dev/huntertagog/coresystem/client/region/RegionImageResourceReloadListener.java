package dev.huntertagog.coresystem.client.region;

import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.minecraft.resource.ResourceType;
import net.minecraft.util.Identifier;

public final class RegionImageResourceReloadListener {

    private RegionImageResourceReloadListener() {
    }

    public static void register() {
        ResourceManagerHelper.get(ResourceType.CLIENT_RESOURCES).registerReloadListener(
                new SimpleSynchronousResourceReloadListener() {
                    @Override
                    public Identifier getFabricId() {
                        return Identifier.of("coresystem", "region_image_reload");
                    }

                    @Override
                    public void reload(net.minecraft.resource.ResourceManager manager) {
                        // MVP: optional validation only – kein Upload erforderlich.
                        // Du kannst hier z.B. bei Bedarf fehlende Texturen loggen.
                    }
                }
        );
    }
}
