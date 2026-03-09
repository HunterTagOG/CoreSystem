package dev.huntertagog.coresystem.fabric.server.world.runtime;

import dev.huntertagog.coresystem.fabric.server.islands.PrivateIslandWorldService;
import dev.huntertagog.coresystem.platform.world.PrivateIslandWorldAccess;
import net.minecraft.server.world.ServerWorld;

public record RuntimeWorldHandle(PrivateIslandWorldService piws, ServerWorld world) {

    public void setTickWhenEmpty(boolean tickWhenEmpty) {
        ((PrivateIslandWorldAccess) this.world).piws$setTickWhenEmpty(tickWhenEmpty);
    }

    public void delete() {
        this.piws.enqueueWorldDeletion(this.world);
    }

    public ServerWorld asWorld() {
        return this.world;
    }
}
