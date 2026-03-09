package dev.huntertagog.coresystem.client.model;

import dev.huntertagog.coresystem.fabric.common.item.impl.ServerKeyItem;
import net.minecraft.util.Identifier;
import software.bernie.geckolib.model.GeoModel;

public class ServerKeyModel extends GeoModel<ServerKeyItem> {

    @Override
    public Identifier getModelResource(ServerKeyItem animatable) {
        return Identifier.of("coresystem", "geo/server_key.geo.json");
    }

    @Override
    public Identifier getTextureResource(ServerKeyItem animatable) {
        return Identifier.of("coresystem", "textures/item/server_key.png");
    }

    @Override
    public Identifier getAnimationResource(ServerKeyItem animatable) {
        return Identifier.of("coresystem", "animations/server_key.animation.json");
    }
}
