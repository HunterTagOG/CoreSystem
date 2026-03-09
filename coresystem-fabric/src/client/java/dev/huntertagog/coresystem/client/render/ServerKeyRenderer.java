package dev.huntertagog.coresystem.client.render;

import dev.huntertagog.coresystem.client.model.ServerKeyModel;
import dev.huntertagog.coresystem.fabric.common.item.impl.ServerKeyItem;
import software.bernie.geckolib.renderer.GeoItemRenderer;

public class ServerKeyRenderer extends GeoItemRenderer<ServerKeyItem> {
    public ServerKeyRenderer() {
        super(new ServerKeyModel());
        this.withScale(0.9f);
    }
}
