package dev.huntertagog.coresystem.fabric.common.economy;

import dev.huntertagog.coresystem.platform.player.PlayerRef;
import net.minecraft.server.network.ServerPlayerEntity;

public final class FabricPlayerRefs {

    public static PlayerRef of(ServerPlayerEntity player) {
        return new PlayerRef(player.getUuid());
    }
}
