package dev.huntertagog.coresystem.velocity.net;

import com.velocitypowered.api.proxy.messages.ChannelIdentifier;
import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier;

public final class BridgeChannel {
    private BridgeChannel() {
    }

    public static final ChannelIdentifier IDENTIFIER =
            MinecraftChannelIdentifier.create("coresystem", "bridge");
}
